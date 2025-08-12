package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.*;
import com.xinyu.InterviewCoach_v2.entity.Message;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.xinyu.InterviewCoach_v2.enums.MessageType;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.mapper.MessageMapper;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import com.xinyu.InterviewCoach_v2.mapper.SessionMapper;
import com.xinyu.InterviewCoach_v2.mapper.UserAttemptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心对话服务 - 处理AI面试对话和状态控制
 */
@Service
public class ChatService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserAttemptMapper userAttemptMapper;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private QuestionSetService questionSetService;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    private final RestTemplate restTemplate = new RestTemplate();

    // 为 structured_set 模式存储题目队列
    private final Map<Long, List<Long>> sessionQuestionQueues = new HashMap<>();

    /**
     * 启动新的面试会话
     */
    @Transactional
    public SessionResponse startSession(Long userId, StartSessionRequest request) {
        try {
            // 验证请求
            validateStartSessionRequest(request);

            // 结束用户所有活跃会话
            sessionService.endAllActiveSessionsByUserId(userId);

            // 创建新会话
            SessionDTO session = sessionService.createSession(
                    userId,
                    request.getMode(),
                    request.getExpectedQuestionCount()
            );

            // 为 structured_set 模式初始化题目队列
            if (request.getMode() == SessionMode.STRUCTURED_SET) {
                initQuestionQueue(session.getId(), request.getQuestionIds());
            }

            // 发送开场白和第一题
            String firstQuestion = generateFirstQuestion(session.getId(), request);
            MessageDTO aiMessage = saveAIMessage(session.getId(), firstQuestion);

            // 更新已提问数量
            sessionService.incrementAskedQuestionCount(session.getId());

            return new SessionResponse(
                    true,
                    session,
                    InterviewState.WAITING_FOR_USER_ANSWER,
                    true
            );

        } catch (Exception e) {
            return new SessionResponse(false, "启动面试失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户消息并生成AI回复
     */
    @Transactional
    public ChatResponse processUserMessage(Long userId, Long sessionId, SendMessageRequest request) {
        try {
            // 验证会话
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return new ChatResponse(false, "无权访问此会话");
            }

            if (!sessionService.isSessionActive(sessionId)) {
                return new ChatResponse(false, "会话已结束");
            }

            // 保存用户消息
            saveUserMessage(sessionId, request.getText());

            // 推断当前面试状态
            InterviewState currentState = inferInterviewState(sessionId);

            // 根据状态处理用户回答
            return handleUserResponse(sessionId, request.getText(), currentState);

        } catch (Exception e) {
            return new ChatResponse(false, "处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息历史
     */
    public List<MessageDTO> getSessionMessages(Long userId, Long sessionId) {
        // 验证会话所有权
        if (!sessionService.validateSessionOwnership(sessionId, userId)) {
            throw new RuntimeException("无权访问此会话");
        }

        return messageMapper.findBySessionId(sessionId).stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 结束面试会话
     */
    @Transactional
    public ChatResponse endSession(Long userId, Long sessionId) {
        try {
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return new ChatResponse(false, "无权访问此会话");
            }

            // 生成结束语
            String finalSummary = generateFinalSummary(sessionId);
            MessageDTO aiMessage = saveAIMessage(sessionId, finalSummary);

            // 结束会话
            sessionService.endSession(sessionId);

            // 清理题目队列
            sessionQuestionQueues.remove(sessionId);

            return new ChatResponse(
                    true,
                    aiMessage,
                    InterviewState.SESSION_ENDED,
                    false
            );

        } catch (Exception e) {
            return new ChatResponse(false, "结束会话失败: " + e.getMessage());
        }
    }

    /**
     * 为 structured_set 模式初始化题目队列
     */
    private void initQuestionQueue(Long sessionId, List<Long> questionIds) {
        if (questionIds != null && !questionIds.isEmpty()) {
            // 创建题目队列的副本，避免修改原始列表
            List<Long> queue = new ArrayList<>(questionIds);
            sessionQuestionQueues.put(sessionId, queue);
        }
    }

    /**
     * 基于消息时序推断当前面试状态
     */
    private InterviewState inferInterviewState(Long sessionId) {
        List<Message> messages = messageMapper.findBySessionId(sessionId);
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("会话不存在");
        }

        Session session = sessionOpt.get();

        // 如果没有消息，说明面试刚开始
        if (messages.isEmpty()) {
            return InterviewState.STARTED;
        }

        // 检查是否已完成所有题目
        if (session.isCompleted()) {
            return InterviewState.INTERVIEW_COMPLETED;
        }

        // 分析最后几条消息的模式
        Message lastMessage = messages.get(messages.size() - 1);

        // 如果最后一条消息是用户消息，说明等待AI分析和反馈
        if (lastMessage.getType() == MessageType.USER) {
            return InterviewState.AI_ANALYZING;
        }

        // 如果最后一条是AI消息，需要判断是反馈还是新题目
        if (lastMessage.getType() == MessageType.AI) {
            // 简单判断：如果AI消息包含问号，可能是新题目
            if (lastMessage.getText().contains("?") || lastMessage.getText().contains("？")) {
                return InterviewState.WAITING_FOR_USER_ANSWER;
            } else {
                // 可能是反馈，准备下一题
                return InterviewState.AI_FEEDBACK;
            }
        }

        return InterviewState.WAITING_FOR_USER_ANSWER;
    }

    /**
     * 处理用户回答
     */
    private ChatResponse handleUserResponse(Long sessionId, String userAnswer, InterviewState state) {
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return new ChatResponse(false, "会话不存在");
        }

        Session session = sessionOpt.get();

        // 记录用户答题尝试（这里需要从上下文推断当前题目）
        recordUserAttempt(session.getUserId(), sessionId);

        // 增加已完成题目数量
        sessionService.incrementCompletedQuestionCount(sessionId);

        // 检查是否已完成所有题目
        if (session.getCompletedQuestionCount() + 1 >= session.getExpectedQuestionCount()) {
            // 生成最终评价
            String finalEvaluation = generateFinalEvaluation(sessionId, userAnswer);
            MessageDTO aiMessage = saveAIMessage(sessionId, finalEvaluation);

            // 结束会话
            sessionService.endSession(sessionId);

            // 清理题目队列
            sessionQuestionQueues.remove(sessionId);

            return new ChatResponse(
                    true,
                    aiMessage,
                    InterviewState.INTERVIEW_COMPLETED,
                    false
            );
        } else {
            // 生成反馈并提出下一题
            String feedbackAndNextQuestion = generateFeedbackAndNextQuestion(sessionId, userAnswer);
            MessageDTO aiMessage = saveAIMessage(sessionId, feedbackAndNextQuestion);

            // 增加已提问数量
            sessionService.incrementAskedQuestionCount(sessionId);

            return new ChatResponse(
                    true,
                    aiMessage,
                    InterviewState.WAITING_FOR_USER_ANSWER,
                    true
            );
        }
    }

    /**
     * 生成第一个问题
     */
    private String generateFirstQuestion(Long sessionId, StartSessionRequest request) {
        Question selectedQuestion = selectQuestion(sessionId, request);

        if (selectedQuestion == null) {
            return "很抱歉，暂时没有合适的题目。请稍后再试。";
        }

        String prompt = buildFirstQuestionPrompt(request.getMode(), selectedQuestion);
        return callOpenAI(prompt);
    }

    /**
     * 选择题目的策略
     */
    private Question selectQuestion(Long sessionId, StartSessionRequest request) {
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        Session session = sessionOpt.get();
        Long userId = session.getUserId();

        switch (request.getMode()) {
            case SINGLE_TOPIC:
                return selectQuestionByTag(userId, request.getTagId());
            case STRUCTURED_SET:
                return selectQuestionFromQueue(sessionId);
            case STRUCTURED_TEMPLATE:
                return selectQuestionByTemplate(userId);
            default:
                return null;
        }
    }

    /**
     * 从题目队列中选择题目（structured_set模式）
     */
    private Question selectQuestionFromQueue(Long sessionId) {
        List<Long> queue = sessionQuestionQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }

        // 从队列头部取出一个题目ID
        Long questionId = queue.remove(0);
        return questionMapper.findById(questionId).orElse(null);
    }

    /**
     * 根据标签选择题目（优先级：未尝试 > 尝试次数少 > 随机）
     */
    private Question selectQuestionByTag(Long userId, Long tagId) {
        if (tagId == null) {
            return null;
        }

        // 优先选择未尝试过的题目
        List<Question> untriedQuestions = userAttemptMapper.findUntriedQuestionsByTagId(userId, tagId);
        if (!untriedQuestions.isEmpty()) {
            return untriedQuestions.get(0);
        }

        // 选择尝试次数最少的题目
        List<Question> leastAttempted = userAttemptMapper.findLeastAttemptedQuestionsByTagId(userId, tagId, 5);
        if (!leastAttempted.isEmpty()) {
            return leastAttempted.get(new Random().nextInt(leastAttempted.size()));
        }

        return null;
    }

    /**
     * 从题集中选择题目（废弃的方法，现在使用队列）
     */
    private Question selectQuestionFromSet(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return null;
        }

        Long questionId = questionIds.get(0); // 简单实现：选择第一个
        return questionMapper.findById(questionId).orElse(null);
    }

    /**
     * 根据模板选择题目
     */
    private Question selectQuestionByTemplate(Long userId) {
        // 简单实现：随机选择一个题目
        List<Question> allQuestions = questionMapper.findRandom(10);
        return allQuestions.isEmpty() ? null : allQuestions.get(0);
    }

    /**
     * 记录用户答题尝试
     */
    private void recordUserAttempt(Long userId, Long sessionId) {
        // 这里需要从会话上下文中推断当前题目ID
        // 简化实现：可以在Message中存储额外信息或者通过其他方式关联
        // 暂时跳过具体实现
    }

    /**
     * 构建第一题的提示词
     */
    private String buildFirstQuestionPrompt(SessionMode mode, Question question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的面试官，正在进行技术面试。");

        // 根据模式添加不同的背景描述
        switch (mode) {
            case SINGLE_TOPIC:
                prompt.append("本次面试将围绕特定主题进行。");
                break;
            case STRUCTURED_SET:
                prompt.append("本次面试将按照预设的题目顺序进行。");
                break;
            case STRUCTURED_TEMPLATE:
                prompt.append("本次面试将根据结构化模板进行。");
                break;
        }

        prompt.append("请根据以下题目向候选人提问：\n\n");
        prompt.append("题目：").append(question.getText()).append("\n\n");
        prompt.append("请以自然、友好的语气提出这个问题，并可以适当补充一些背景信息。");

        return prompt.toString();
    }

    /**
     * 生成反馈和下一题
     */
    private String generateFeedbackAndNextQuestion(Long sessionId, String userAnswer) {
        // 获取下一个题目
        Question nextQuestion = getNextQuestion(sessionId);

        if (nextQuestion == null) {
            // 如果没有下一题，说明题目已经用完，提前结束面试
            return generateFinalEvaluation(sessionId, userAnswer);
        }

        String prompt = String.format(
                "作为面试官，请对候选人的回答进行简短评价，然后提出下一个问题。\n\n" +
                        "候选人回答：%s\n\n" +
                        "下一个问题：%s\n\n" +
                        "请先给出简短的反馈（2-3句话），然后自然地引入下一个问题。",
                userAnswer,
                nextQuestion.getText()
        );

        return callOpenAI(prompt);
    }

    /**
     * 获取下一个题目
     */
    private Question getNextQuestion(Long sessionId) {
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        Session session = sessionOpt.get();

        // 根据会话模式选择下一题
        switch (session.getMode()) {
            case SINGLE_TOPIC:
                return getNextQuestionByTag(session);
            case STRUCTURED_SET:
                return getNextQuestionFromQueue(sessionId);
            case STRUCTURED_TEMPLATE:
                return getNextQuestionByTemplate(session);
            default:
                return null;
        }
    }

    /**
     * 从队列获取下一题（structured_set模式）
     */
    private Question getNextQuestionFromQueue(Long sessionId) {
        return selectQuestionFromQueue(sessionId);
    }

    /**
     * 根据标签获取下一题
     */
    private Question getNextQuestionByTag(Session session) {
        // 简化实现：随机选择一个该标签下的题目
        // 实际应该基于用户答题历史进行智能选择
        return selectQuestionByTag(session.getUserId(), 1L); // 假设tagId为1
    }

    /**
     * 从题集获取下一题（废弃的方法）
     */
    private Question getNextQuestionFromSet(Session session) {
        // 简化实现：根据已提问数量获取题集中的下一题
        return questionMapper.findRandom(1).stream().findFirst().orElse(null);
    }

    /**
     * 根据模板获取下一题
     */
    private Question getNextQuestionByTemplate(Session session) {
        return selectQuestionByTemplate(session.getUserId());
    }

    /**
     * 生成最终评价
     */
    private String generateFinalEvaluation(Long sessionId, String lastAnswer) {
        List<Message> messages = messageMapper.findBySessionId(sessionId);
        StringBuilder conversationHistory = new StringBuilder();

        for (Message msg : messages) {
            conversationHistory.append(msg.getType() == MessageType.AI ? "面试官：" : "候选人：")
                    .append(msg.getText()).append("\n\n");
        }

        conversationHistory.append("候选人：").append(lastAnswer);

        String prompt = String.format(
                "作为面试官，请对整场面试进行总结评价。\n\n" +
                        "面试对话记录：\n%s\n\n" +
                        "请给出：\n" +
                        "1. 总体表现评价\n" +
                        "2. 主要优点\n" +
                        "3. 需要改进的地方\n" +
                        "4. 建议\n\n" +
                        "请保持专业、客观、鼓励的语气。",
                conversationHistory.toString()
        );

        return callOpenAI(prompt);
    }

    /**
     * 生成结束语
     */
    private String generateFinalSummary(Long sessionId) {
        return "感谢您参加本次面试！面试已结束。希望这次练习对您有所帮助。祝您求职顺利！";
    }

    /**
     * 调用OpenAI API
     */
    private String callOpenAI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAiModel);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", "你是一个专业的技术面试官，经验丰富，善于引导候选人。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    openAiApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "AI暂时无法回应，请稍后再试。";

        } catch (Exception e) {
            return "AI服务暂时不可用：" + e.getMessage();
        }
    }

    /**
     * 保存AI消息
     */
    private MessageDTO saveAIMessage(Long sessionId, String text) {
        Message message = new Message(sessionId, MessageType.AI, text);
        messageMapper.insert(message);
        return convertMessageToDTO(message);
    }

    /**
     * 保存用户消息
     */
    private MessageDTO saveUserMessage(Long sessionId, String text) {
        Message message = new Message(sessionId, MessageType.USER, text);
        messageMapper.insert(message);
        return convertMessageToDTO(message);
    }

    /**
     * 验证启动会话请求
     */
    private void validateStartSessionRequest(StartSessionRequest request) {
        if (request.getMode() == null) {
            throw new IllegalArgumentException("会话模式不能为空");
        }

        if (request.getExpectedQuestionCount() == null || request.getExpectedQuestionCount() < 1 || request.getExpectedQuestionCount() > 10) {
            throw new IllegalArgumentException("期望题目数量必须在1-10之间");
        }

        switch (request.getMode()) {
            case SINGLE_TOPIC:
                if (request.getTagId() == null) {
                    throw new IllegalArgumentException("单主题模式需要指定标签ID");
                }
                break;
            case STRUCTURED_SET:
                if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
                    throw new IllegalArgumentException("结构化题集模式需要指定题目ID列表");
                }
                // 验证题目数量是否与期望的题目数量匹配
                if (request.getQuestionIds().size() < request.getExpectedQuestionCount()) {
                    throw new IllegalArgumentException("题集中的题目数量不足，至少需要 " + request.getExpectedQuestionCount() + " 道题目");
                }
                break;
        }
    }

    /**
     * 将Message实体转换为MessageDTO
     */
    private MessageDTO convertMessageToDTO(Message message) {
        return new MessageDTO(
                message.getId(),
                message.getSessionId(),
                message.getType(),
                message.getText(),
                message.getCreatedAt()
        );
    }
}