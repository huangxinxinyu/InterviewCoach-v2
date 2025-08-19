package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.TemplateDTO;
import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.dto.request.chat.SendMessageRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.chat.StartInterviewRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.chat.ChatMessageResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.chat.InterviewSessionResponseDTO;
import com.xinyu.InterviewCoach_v2.entity.Answer;
import com.xinyu.InterviewCoach_v2.entity.Message;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.xinyu.InterviewCoach_v2.enums.MessageType;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.mapper.*;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心对话服务 - 重构后使用统一的DTO结构，添加 UserAttempt 记录
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
    private QuestionTagMapper questionTagMapper;

    @Autowired
    private UserAttemptMapper userAttemptMapper;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private UserAttemptService userAttemptService;

    @Autowired
    private DTOConverter dtoConverter;

    @Autowired
    private QuestionSetService questionSetService;

    @Autowired
    private AnswerService answerService;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    private final RestTemplate restTemplate = new RestTemplate();

    // 为 structured_set 模式存储题目队列
    private final Map<Long, List<Long>> sessionQuestionQueues = new HashMap<>();

    // 添加当前题目跟踪 - 记录每个会话当前正在问的题目ID
    private final Map<Long, Long> sessionCurrentQuestions = new HashMap<>();

    /**
     * 启动新的面试会话
     */
    @Transactional
    public InterviewSessionResponseDTO startInterview(Long userId, StartInterviewRequestDTO request) {
        try {
            // 1. 验证请求参数
            if (!request.isValid()) {
                return InterviewSessionResponseDTO.builder()
                        .success(false)
                        .message("请求参数无效");
            }

            // 2. 处理不同模式的预处理逻辑
            switch (request.getMode()) {
                case STRUCTURED_SET:
                    handleStructuredSetMode(request);
                    break;
                case STRUCTURED_TEMPLATE:
                    handleStructuredTemplateMode(request);
                    break;
                case SINGLE_TOPIC:
                    handleSingleTopicMode(request);
                    break;
            }

            // 3. 创建新会话
            SessionDTO session = sessionService.createSession(
                    userId,
                    request.getMode(),
                    request.getExpectedQuestionCount()
            );

            // 4. 为所有模式初始化题目队列
            initQuestionQueueForMode(session.getId(), request, userId);

            // 5. 选择第一个题目并记录
            Question firstQuestion = peekQuestionFromQueue(session.getId());
            if (firstQuestion == null) {
                return InterviewSessionResponseDTO.builder()
                        .success(false)
                        .message("没有找到合适的题目");
            }

            // 记录当前题目
            sessionCurrentQuestions.put(session.getId(), firstQuestion.getId());

            // 记录用户开始尝试此题目
            userAttemptService.recordAttempt(userId, firstQuestion.getId());

            // 6. 生成并保存第一题的AI消息
            String firstQuestionText = generateFirstQuestion(firstQuestion, request.getMode());
            MessageDTO aiMessage = saveAIMessage(session.getId(), firstQuestionText);

            // 7. 更新已提问数量
            sessionService.incrementAskedQuestionCount(session.getId());

            return InterviewSessionResponseDTO.builder()
                    .success(true)
                    .session(session)
                    .currentState(InterviewState.WAITING_FOR_USER_ANSWER)
                    .chatInputEnabled(true);

        } catch (Exception e) {
            return InterviewSessionResponseDTO.builder()
                    .success(false)
                    .message("启动面试失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户消息并生成AI回复
     */
    @Transactional
    public ChatMessageResponseDTO processUserMessage(Long userId, Long sessionId, SendMessageRequestDTO request) {
        try {
            // 验证会话
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return ChatMessageResponseDTO.builder()
                        .success(false)
                        .message("无权访问此会话");
            }

            if (!sessionService.isSessionActive(sessionId)) {
                return ChatMessageResponseDTO.builder()
                        .success(false)
                        .message("会话已结束");
            }

            // 验证消息内容
            if (!request.isValid()) {
                return ChatMessageResponseDTO.builder()
                        .success(false)
                        .message("消息内容不能为空");
            }

            // 保存用户消息
            saveUserMessage(sessionId, request.getProcessedText());

            // 推断当前面试状态
            InterviewState currentState = inferInterviewState(sessionId);

            // 根据状态处理用户回答
            return handleUserResponse(sessionId, request.getProcessedText(), currentState);

        } catch (Exception e) {
            return ChatMessageResponseDTO.builder()
                    .success(false)
                    .message("处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 结束面试会话
     */
    @Transactional
    public ChatMessageResponseDTO endInterview(Long userId, Long sessionId) {
        try {
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return ChatMessageResponseDTO.builder()
                        .success(false)
                        .message("无权访问此会话");
            }

            // 生成结束语
            String finalSummary = generateFinalSummary(sessionId);
            MessageDTO aiMessage = saveAIMessage(sessionId, finalSummary);

            // 结束会话
            sessionService.endSession(sessionId);

            // 清理题目队列和当前题目记录
            sessionQuestionQueues.remove(sessionId);
            sessionCurrentQuestions.remove(sessionId);

            return ChatMessageResponseDTO.builder()
                    .success(true)
                    .aiMessage(aiMessage)
                    .currentState(InterviewState.SESSION_ENDED)
                    .chatInputEnabled(false);

        } catch (Exception e) {
            return ChatMessageResponseDTO.builder()
                    .success(false)
                    .message("结束会话失败: " + e.getMessage());
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
                .map(dtoConverter::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 为不同模式初始化题目队列 - 统一的队列初始化方法
     */
    private void initQuestionQueueForMode(Long sessionId, StartInterviewRequestDTO request, Long userId) {
        switch (request.getMode()) {
            case STRUCTURED_SET:
                initQuestionQueue(sessionId, request.getQuestionIds());
                break;
            case STRUCTURED_TEMPLATE:
                initTemplateQuestionQueue(sessionId, request.getTemplateId(), userId);
                break;
            case SINGLE_TOPIC:
                initSingleTopicQuestionQueue(sessionId, request.getTagId(), userId, request.getExpectedQuestionCount());
                break;
            default:
                throw new RuntimeException("不支持的面试模式: " + request.getMode());
        }
    }

    /**
     * 为单主题模式初始化题目队列
     */
    private void initSingleTopicQuestionQueue(Long sessionId, Long tagId, Long userId, Integer expectedQuestionCount) {
        if (tagId == null) {
            throw new RuntimeException("单主题模式必须提供tagId");
        }

        if (expectedQuestionCount == null || expectedQuestionCount <= 0) {
            expectedQuestionCount = 5; // 默认5题
        }

        try {
            List<Long> selectedQuestionIds = new ArrayList<>();

            List<Question> untriedQuestions = userAttemptMapper.findUntriedQuestionsByTagId(userId, tagId);
            for (Question q : untriedQuestions) {
                selectedQuestionIds.add(q.getId());
                if (selectedQuestionIds.size() >= expectedQuestionCount) {
                    break;
                }
            }

            if (selectedQuestionIds.size() < expectedQuestionCount) {
                int remainingCount = expectedQuestionCount - selectedQuestionIds.size();
                List<Question> leastAttempted = userAttemptMapper.findLeastAttemptedQuestionsByTagId(
                        userId, tagId, remainingCount * 2); // 获取更多候选以便随机选择

                for (Question q : leastAttempted) {
                    if (!selectedQuestionIds.contains(q.getId())) {
                        selectedQuestionIds.add(q.getId());
                        if (selectedQuestionIds.size() >= expectedQuestionCount) {
                            break;
                        }
                    }
                }
            }

            if (selectedQuestionIds.size() < expectedQuestionCount) {
                List<Question> allTagQuestions = questionTagMapper.findQuestionsByTagId(tagId);
                for (Question q : allTagQuestions) {
                    if (!selectedQuestionIds.contains(q.getId())) {
                        selectedQuestionIds.add(q.getId());
                        if (selectedQuestionIds.size() >= expectedQuestionCount) {
                            break;
                        }
                    }
                }
            }

            if (selectedQuestionIds.isEmpty()) {
                throw new RuntimeException("该标签下没有可用的题目");
            }

            Collections.shuffle(selectedQuestionIds);

            if (selectedQuestionIds.size() > expectedQuestionCount) {
                selectedQuestionIds = selectedQuestionIds.subList(0, expectedQuestionCount);
            }

            sessionQuestionQueues.put(sessionId, new ArrayList<>(selectedQuestionIds));

        } catch (Exception e) {
            throw new RuntimeException("初始化单主题题目队列失败: " + e.getMessage());
        }
    }

    /**
     * 为 structured_set 模式初始化题目队列
     */
    private void initQuestionQueue(Long sessionId, List<Long> questionIds) {
        if (questionIds != null && !questionIds.isEmpty()) {
            List<Long> queue = new ArrayList<>(questionIds);
            sessionQuestionQueues.put(sessionId, queue);
        }
    }

    /**
     * 为模板模式初始化题目队列
     */
    private void initTemplateQuestionQueue(Long sessionId, Long templateId, Long userId) {
        try {
            TemplateDTO template = templateService.parseTemplateContent(templateId);
            List<Long> selectedQuestionIds = new ArrayList<>();

            // 按照模板的每个章节选择题目
            for (TemplateDTO.TemplateSection section : template.getSections()) {
                List<Long> sectionQuestionIds = selectQuestionsForSection(section, userId);
                selectedQuestionIds.addAll(sectionQuestionIds);
            }

            if (selectedQuestionIds.isEmpty()) {
                throw new RuntimeException("模板中没有找到合适的题目");
            }

            // 将题目ID列表放入队列
            sessionQuestionQueues.put(sessionId, new ArrayList<>(selectedQuestionIds));

        } catch (Exception e) {
            throw new RuntimeException("初始化模板题目队列失败: " + e.getMessage());
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

        if (messages.isEmpty()) {
            return InterviewState.STARTED;
        }

        if (session.isCompleted()) {
            return InterviewState.INTERVIEW_COMPLETED;
        }

        Message lastMessage = messages.get(messages.size() - 1);

        if (lastMessage.getType() == MessageType.USER) {
            return InterviewState.AI_ANALYZING;
        }

        if (lastMessage.getType() == MessageType.AI) {
            if (lastMessage.getText().contains("?") || lastMessage.getText().contains("？")) {
                return InterviewState.WAITING_FOR_USER_ANSWER;
            } else {
                return InterviewState.AI_FEEDBACK;
            }
        }

        return InterviewState.WAITING_FOR_USER_ANSWER;
    }

    /**
     * 处理用户回答
     */
    private ChatMessageResponseDTO handleUserResponse(Long sessionId, String userAnswer, InterviewState state) {
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ChatMessageResponseDTO.builder()
                    .success(false)
                    .message("会话不存在");
        }

        Session session = sessionOpt.get();
        Long userId = session.getUserId();

        // 增加已完成题目数量
        sessionService.incrementCompletedQuestionCount(sessionId);

        // 重新获取更新后的session
        session = sessionMapper.findById(sessionId).get();

        // 检查是否已完成所有题目
        if (session.getCompletedQuestionCount() >= session.getExpectedQuestionCount()) {
            // 生成最终评价（传入当前题目ID用于获取标准答案）
            Long currentQuestionId = sessionCurrentQuestions.get(sessionId);
            String finalEvaluation = generateFinalEvaluationWithAnswer(sessionId, userAnswer, currentQuestionId);
            MessageDTO aiMessage = saveAIMessage(sessionId, finalEvaluation);

            // 结束会话
            sessionService.endSession(sessionId);

            // 清理题目队列和当前题目记录
            sessionQuestionQueues.remove(sessionId);
            sessionCurrentQuestions.remove(sessionId);

            return ChatMessageResponseDTO.builder()
                    .success(true)
                    .aiMessage(aiMessage)
                    .currentState(InterviewState.INTERVIEW_COMPLETED)
                    .chatInputEnabled(false);
        } else {
            // 选择下一题
            Question nextQuestion = peekQuestionFromQueue(sessionId);
            if (nextQuestion != null) {
                // 记录当前题目
                sessionCurrentQuestions.put(sessionId, nextQuestion.getId());

                // 记录用户开始尝试新题目
                userAttemptService.recordAttempt(userId, nextQuestion.getId());
            }

            // 生成反馈并提出下一题
            String feedbackAndNextQuestion = generateFeedbackAndNextQuestion(sessionId, userAnswer);
            MessageDTO aiMessage = saveAIMessage(sessionId, feedbackAndNextQuestion);

            // 增加已提问数量
            sessionService.incrementAskedQuestionCount(sessionId);

            return ChatMessageResponseDTO.builder()
                    .success(true)
                    .aiMessage(aiMessage)
                    .currentState(InterviewState.WAITING_FOR_USER_ANSWER)
                    .chatInputEnabled(true);
        }
    }

    /**
     * 生成第一个问题
     */
    private String generateFirstQuestion(Question question, SessionMode mode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的面试官，正在进行技术面试。");

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

        return callOpenAI(prompt.toString());
    }

    /**
     * 获取题目的标准答案
     */
    private String getStandardAnswerForQuestion(Long questionId) {
        if (questionId == null) {
            return null;
        }

        try {
            List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
            if (!answers.isEmpty()) {
                // 如果有多个答案，取第一个作为主要参考答案
                // 或者可以拼接多个答案
                if (answers.size() == 1) {
                    return answers.get(0).getText();
                } else {
                    // 多个答案时，组合它们
                    StringBuilder combinedAnswer = new StringBuilder();
                    for (int i = 0; i < answers.size(); i++) {
                        combinedAnswer.append("参考答案").append(i + 1).append("：\n");
                        combinedAnswer.append(answers.get(i).getText());
                        if (i < answers.size() - 1) {
                            combinedAnswer.append("\n\n");
                        }
                    }
                    return combinedAnswer.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("获取题目答案失败: " + e.getMessage());
        }

        return null;
    }

    private Question peekQuestionFromQueue(Long sessionId) {
        List<Long> queue = sessionQuestionQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }

        Long questionId = queue.get(0);  // 使用get(0)而不是remove(0)
        return questionMapper.findById(questionId).orElse(null);
    }

    private void removeCurrentQuestionFromQueue(Long sessionId) {
        List<Long> queue = sessionQuestionQueues.get(sessionId);
        if (queue != null && !queue.isEmpty()) {
            queue.remove(0);  // 移除已经问过的题目
        }
    }

    /**
     * 根据模板选择题目
     */
    private Question selectQuestionByTemplate(Long userId) {
        List<Question> allQuestions = questionMapper.findRandom(10);
        return allQuestions.isEmpty() ? null : allQuestions.get(0);
    }

    /**
     * 构建包含标准答案的反馈 prompt
     */
    private String buildFeedbackPromptWithAnswer(String userAnswer, Question nextQuestion, String standardAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为面试官，请对候选人的回答进行评价，然后提出下一个问题。\n\n");

        if (standardAnswer != null && !standardAnswer.trim().isEmpty()) {
            prompt.append("标准答案参考：\n").append(standardAnswer).append("\n\n");
            prompt.append("候选人回答：\n").append(userAnswer).append("\n\n");
            prompt.append("评价要求：\n");
            prompt.append("1. 如果候选人回答与标准答案大差不差，请给予肯定的评价\n");
            prompt.append("2. 如果候选人回答不够全面，请适当补充遗漏的重要知识点\n");
            prompt.append("3. 如果候选人回答有错误，请温和地指出并给出正确信息\n");
            prompt.append("4. 保持鼓励的语气，即使答案不完美也要肯定其中的亮点\n\n");
        } else {
            prompt.append("候选人回答：\n").append(userAnswer).append("\n\n");
            prompt.append("评价要求：\n");
            prompt.append("1. 基于你的专业知识对回答进行评价\n");
            prompt.append("2. 给出建设性的反馈和建议\n");
            prompt.append("3. 保持鼓励的语气\n\n");
        }

        prompt.append("下一个问题：\n").append(nextQuestion.getText()).append("\n\n");
        prompt.append("请先给出简短但有价值的反馈（2-4句话），然后自然地引入下一个问题。");

        return prompt.toString();
    }

    /**
     * 生成反馈和下一题
     */
    private String generateFeedbackAndNextQuestion(Long sessionId, String userAnswer) {
        // 获取上一题的标准答案（用户刚刚回答的题目）
        Long previousQuestionId = sessionCurrentQuestions.get(sessionId);
        String standardAnswer = getStandardAnswerForQuestion(previousQuestionId);

        Question nextQuestion = getNextQuestion(sessionId);

        if (nextQuestion == null) {
            System.out.println("-------------no more questions left");
            return generateFinalEvaluationWithAnswer(sessionId, userAnswer, previousQuestionId);
        }

        String prompt = buildFeedbackPromptWithAnswer(userAnswer, nextQuestion, standardAnswer);
        return callOpenAI(prompt);
    }

    /**
     * 为模板的一个章节选择题目
     */
    private List<Long> selectQuestionsForSection(TemplateDTO.TemplateSection section, Long userId) {
        List<Long> selectedQuestionIds = new ArrayList<>();
        int neededCount = section.getQuestionCount();

        // 为该章节的每个标签收集题目
        List<Question> candidateQuestions = new ArrayList<>();
        for (Long tagId : section.getTagIds()) {
            // 优先选择用户未尝试过的题目
            List<Question> untriedQuestions = userAttemptMapper.findUntriedQuestionsByTagId(userId, tagId);
            candidateQuestions.addAll(untriedQuestions);

            // 如果未尝试的题目不够，添加尝试次数较少的题目
            if (candidateQuestions.size() < neededCount) {
                List<Question> leastAttempted = userAttemptMapper.findLeastAttemptedQuestionsByTagId(userId, tagId, neededCount * 2);
                for (Question q : leastAttempted) {
                    if (!candidateQuestions.stream().anyMatch(existing -> existing.getId().equals(q.getId()))) {
                        candidateQuestions.add(q);
                    }
                }
            }
        }

        // 如果还是不够题目，从所有相关标签中随机选择
        if (candidateQuestions.size() < neededCount) {
            for (Long tagId : section.getTagIds()) {
                List<Question> allTagQuestions = questionTagMapper.findQuestionsByTagId(tagId);
                for (Question q : allTagQuestions) {
                    if (!candidateQuestions.stream().anyMatch(existing -> existing.getId().equals(q.getId()))) {
                        candidateQuestions.add(q);
                    }
                    if (candidateQuestions.size() >= neededCount) break;
                }
                if (candidateQuestions.size() >= neededCount) break;
            }
        }

        // 随机选择所需数量的题目
        Collections.shuffle(candidateQuestions);
        int actualCount = Math.min(neededCount, candidateQuestions.size());
        for (int i = 0; i < actualCount; i++) {
            selectedQuestionIds.add(candidateQuestions.get(i).getId());
        }

        return selectedQuestionIds;
    }

    /**
     * 获取下一个题目 - 修改版本支持模板
     */
    private Question getNextQuestion(Long sessionId) {
        Optional<Session> sessionOpt = sessionMapper.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        Session session = sessionOpt.get();

        // 移除当前题目
        removeCurrentQuestionFromQueue(sessionId);

        // 根据模式获取下一题
        switch (session.getMode()) {
            case SINGLE_TOPIC:
            case STRUCTURED_SET:
            case STRUCTURED_TEMPLATE:
                return peekQuestionFromQueue(sessionId);
            default:
                return selectQuestionByTemplate(session.getUserId());
        }
    }

    /**
     * 处理单主题模式的预处理逻辑
     */
    private void handleSingleTopicMode(StartInterviewRequestDTO request) {
        if (request.getTagId() == null) {
            throw new RuntimeException("单主题模式必须提供tagId");
        }

        if (request.getExpectedQuestionCount() == null || request.getExpectedQuestionCount() <= 0) {
            request.setExpectedQuestionCount(5); // 默认5题
        }

        // 验证题目数量不能超过限制
        if (request.getExpectedQuestionCount() > 20) {
            throw new RuntimeException("题目数量不能超过20个");
        }
    }

    /**
     * 处理structured_set模式的特殊逻辑
     */
    private void handleStructuredSetMode(StartInterviewRequestDTO request) {
        if (request.getQuestionSetId() != null) {
            // 如果提供了questionSetId，从题集获取题目列表
            List<Long> questionIds = questionSetService.getQuestionIdsBySetId(request.getQuestionSetId());
            System.out.println("-------------------------questions: " + questionIds.size());
            if (questionIds.isEmpty()) {
                throw new RuntimeException("题集中没有题目");
            }
            request.setQuestionIds(questionIds);
            request.setExpectedQuestionCount(questionIds.size());
        } else if (request.getQuestionIds() != null) {
            // 如果直接提供了questionIds，确保expectedQuestionCount与之一致
            request.setExpectedQuestionCount(request.getQuestionIds().size());
        } else {
            throw new RuntimeException("structured_set模式必须提供questionSetId或questionIds");
        }

        // 验证题目数量不能超过限制
        if (request.getExpectedQuestionCount() > 20) {
            throw new RuntimeException("题目数量不能超过20个");
        }
    }

    /**
     * 处理structured_template模式的特殊逻辑
     */
    private void handleStructuredTemplateMode(StartInterviewRequestDTO request) {
        if (request.getTemplateId() == null) {
            throw new RuntimeException("structured_template模式必须提供templateId");
        }

        // 解析模板获取题目数量
        TemplateDTO template = templateService.parseTemplateContent(request.getTemplateId());
        if (template.getTotalQuestionCount() == null || template.getTotalQuestionCount() <= 0) {
            throw new RuntimeException("模板中没有有效的题目配置");
        }

        // 设置期望题目数量
        request.setExpectedQuestionCount(template.getTotalQuestionCount());

        // 验证题目数量不能超过限制
        if (request.getExpectedQuestionCount() > 20) {
            throw new RuntimeException("模板题目数量不能超过20个");
        }
    }

    /**
     * 生成包含答案参考的最终评价 - 修正版本，明确传入题目ID
     */
    private String generateFinalEvaluationWithAnswer(Long sessionId, String lastAnswer, Long lastQuestionId) {
        List<Message> messages = messageMapper.findBySessionId(sessionId);
        StringBuilder conversationHistory = new StringBuilder();

        for (Message msg : messages) {
            conversationHistory.append(msg.getType() == MessageType.AI ? "面试官：" : "候选人：")
                    .append(msg.getText()).append("\n\n");
        }

        conversationHistory.append("候选人：").append(lastAnswer);

        // 获取最后一题的标准答案
        String lastQuestionAnswer = getStandardAnswerForQuestion(lastQuestionId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("作为面试官，请对整场面试进行总结评价。\n\n");

        if (lastQuestionAnswer != null && !lastQuestionAnswer.trim().isEmpty()) {
            prompt.append("最后一题的标准答案参考：\n").append(lastQuestionAnswer).append("\n\n");
            prompt.append("请在评价时考虑候选人最后一题的回答与标准答案的匹配度。\n\n");
        }

        prompt.append("面试对话记录：\n").append(conversationHistory.toString()).append("\n\n");
        prompt.append("请给出：\n");
        prompt.append("1. 总体表现评价\n");
        prompt.append("2. 主要优点\n");
        prompt.append("3. 需要改进的地方\n");
        prompt.append("4. 具体建议\n\n");
        prompt.append("请保持专业、客观、鼓励的语气，重点关注候选人的思路和表达能力。");

        return callOpenAI(prompt.toString());
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
        System.out.println(prompt);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAiModel);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content",
                            "你是一个专业的技术面试官，正在直接与候选人对话。" +
                                    "请始终以第一人称与候选人交流，就像真正的面试官一样。" +
                                    "不要提供指导建议或元话语，直接进行面试对话。"),
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
        return dtoConverter.convertMessageToDTO(message);
    }

    /**
     * 保存用户消息
     */
    private MessageDTO saveUserMessage(Long sessionId, String text) {
        Message message = new Message(sessionId, MessageType.USER, text);
        messageMapper.insert(message);
        return dtoConverter.convertMessageToDTO(message);
    }
}