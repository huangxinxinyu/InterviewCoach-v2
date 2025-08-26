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
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.xinyu.InterviewCoach_v2.enums.MessageType;
import com.xinyu.InterviewCoach_v2.mapper.*;
import com.xinyu.InterviewCoach_v2.service.cache.AIResponseCacheManager;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心对话服务 - 重构后使用数据库持久化题目队列
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

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

    @Autowired
    private AIResponseCacheManager aiCacheManager;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    /**
     * 启动新的面试会话
     */
    @Transactional
    public InterviewSessionResponseDTO startInterview(Long userId, StartInterviewRequestDTO request) {
        try {
            logger.info("开始启动面试会话: userId={}, mode={}", userId, request.getMode());

            // 1. 验证请求参数
            if (!request.isValid()) {
                return InterviewSessionResponseDTO.builder()
                        .success(false)
                        .message("请求参数无效");
            }

            // 2. 创建会话
            SessionDTO session = sessionService.createSession(
                    userId, request.getMode(), request.getExpectedQuestionCount());
            logger.debug("创建会话成功: sessionId={}", session.getId());

            // 3. 根据模式初始化题目队列
            List<Long> questionIds = initializeQuestionsByMode(request, userId);
            if (questionIds.isEmpty()) {
                return InterviewSessionResponseDTO.builder()
                        .success(false)
                        .message("无法获取题目，请检查配置");
            }
            logger.debug("初始化题目队列: sessionId={}, questionCount={}",
                    session.getId(), questionIds.size());

            // 4. 初始化题目队列到数据库
            sessionService.initializeQuestionQueue(session.getId(), questionIds);

            // 5. 获取第一个题目并生成开场
            Question firstQuestion = sessionService.getCurrentQuestion(session.getId());
            if (firstQuestion == null) {
                return InterviewSessionResponseDTO.builder()
                        .success(false)
                        .message("无法获取第一个题目");
            }

            // 6. 生成开场消息
            String openingMessage = generateOpeningMessage(firstQuestion);
            MessageDTO aiMessage = saveAIMessage(session.getId(), openingMessage);

            // 7. 移动到下一题准备
            sessionService.moveToNextQuestion(session.getId());

            logger.info("面试会话启动成功: sessionId={}, firstQuestionId={}",
                    session.getId(), firstQuestion.getId());

            return InterviewSessionResponseDTO.builder()
                    .success(true)
                    .session(session)
                    .currentState(InterviewState.WAITING_FOR_USER_ANSWER)
                    .chatInputEnabled(true);

        } catch (Exception e) {
            logger.error("启动面试会话失败: userId=" + userId, e);
            return InterviewSessionResponseDTO.builder()
                    .success(false)
                    .message("启动面试失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户消息
     */
    @Transactional
    public ChatMessageResponseDTO processMessage(Long userId, Long sessionId, SendMessageRequestDTO request) {
        try {
            logger.debug("处理用户消息: sessionId={}, messageLength={}",
                    sessionId, request.getText().length());

            // 1. 验证会话
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return ChatMessageResponseDTO.builder()
                        .success(false)
                        .message("无权访问此会话");
            }

            // 2. 保存用户消息
            MessageDTO userMessage = saveUserMessage(sessionId, request.getText());

            // 3. 获取上一题ID用于生成反馈
            Long previousQuestionId = sessionService.getPreviousQuestionId(sessionId);
            logger.debug("上一题ID: {}", previousQuestionId);

            // 4. 检查是否还有更多题目
            boolean hasMoreQuestions = sessionService.hasMoreQuestions(sessionId);
            logger.debug("还有更多题目: {}", hasMoreQuestions);

            String aiResponse;
            InterviewState currentState;
            boolean chatEnabled = true;

            if (hasMoreQuestions) {
                // 还有题目，生成反馈并问下一题
                Question nextQuestion = sessionService.getCurrentQuestion(sessionId);
                aiResponse = generateFeedbackWithNextQuestion(
                        request.getText(), previousQuestionId, nextQuestion);
                currentState = InterviewState.WAITING_FOR_USER_ANSWER;

                // 移动到下一题
                sessionService.moveToNextQuestion(sessionId);
                System.out.println("question position ++");
                // 增加完成题目计数
                sessionService.incrementCompletedQuestionCount(sessionId);

                logger.debug("生成中间反馈和下一题: nextQuestionId={}",
                        nextQuestion != null ? nextQuestion.getId() : null);

            } else {
                // 没有更多题目，生成最终反馈
                aiResponse = generateFinalFeedback(sessionId, request.getText(), previousQuestionId);
                currentState = InterviewState.SESSION_ENDED;
                chatEnabled = false;

                // 结束会话
                sessionService.endSession(sessionId);
                logger.info("面试会话结束: sessionId={}", sessionId);
            }

            MessageDTO aiMessage = saveAIMessage(sessionId, aiResponse);

            return ChatMessageResponseDTO.builder()
                    .success(true)
                    .aiMessage(aiMessage)
                    .currentState(currentState)
                    .chatInputEnabled(chatEnabled);

        } catch (Exception e) {
            logger.error("处理消息失败: sessionId=" + sessionId, e);
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

            // 结束会话并清理缓存
            sessionService.endSession(sessionId);
            sessionService.clearSessionQueueCache(sessionId);

            logger.info("手动结束面试会话: sessionId={}", sessionId);

            return ChatMessageResponseDTO.builder()
                    .success(true)
                    .aiMessage(aiMessage)
                    .currentState(InterviewState.SESSION_ENDED)
                    .chatInputEnabled(false);

        } catch (Exception e) {
            logger.error("结束会话失败: sessionId=" + sessionId, e);
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
     * 根据模式初始化题目队列
     */
    private List<Long> initializeQuestionsByMode(StartInterviewRequestDTO request, Long userId) {
        switch (request.getMode()) {
            case STRUCTURED_SET:
                return handleStructuredSetMode(request);
            case STRUCTURED_TEMPLATE:
                return handleStructuredTemplateMode(request, userId);
            case SINGLE_TOPIC:
                return handleSingleTopicMode(request, userId);
            default:
                throw new RuntimeException("不支持的会话模式: " + request.getMode());
        }
    }

    /**
     * 处理STRUCTURED_SET模式
     */
    private List<Long> handleStructuredSetMode(StartInterviewRequestDTO request) {
        if (request.getQuestionSetId() != null) {
            return questionSetService.getQuestionIdsBySetId(request.getQuestionSetId());
        } else if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            return new ArrayList<>(request.getQuestionIds());
        } else {
            throw new RuntimeException("STRUCTURED_SET模式需要指定题集ID或题目ID列表");
        }
    }

    /**
     * 处理STRUCTURED_TEMPLATE模式
     */
    private List<Long> handleStructuredTemplateMode(StartInterviewRequestDTO request, Long userId) {
        if (request.getTemplateId() == null) {
            throw new RuntimeException("STRUCTURED_TEMPLATE模式需要指定模板ID");
        }

        TemplateDTO template;
        try {
            template = templateService.parseTemplateContent(request.getTemplateId());
        } catch (RuntimeException e) {
            throw new RuntimeException("模板不存在或解析失败: " + request.getTemplateId());
        }

        if (template.getSections() == null || template.getSections().isEmpty()) {
            throw new RuntimeException("模板内容为空或格式错误，无法解析章节信息");
        }

        List<Long> allQuestionIds = new ArrayList<>();

        for (TemplateDTO.TemplateSection section : template.getSections()) {
            List<Long> sectionQuestions = selectQuestionsForSection(section, userId);
            allQuestionIds.addAll(sectionQuestions);
        }

        if (allQuestionIds.isEmpty()) {
            throw new RuntimeException("模板中没有找到可用的题目");
        }

        return allQuestionIds;
    }

    /**
     * 处理SINGLE_TOPIC模式
     */
    private List<Long> handleSingleTopicMode(StartInterviewRequestDTO request, Long userId) {
        if (request.getTagId() == null) {
            throw new RuntimeException("SINGLE_TOPIC模式需要指定标签ID");
        }

        Integer expectedQuestionCount = request.getExpectedQuestionCount();
        if (expectedQuestionCount == null || expectedQuestionCount <= 0) {
            expectedQuestionCount = 3; // 默认3题
        }

        List<Long> selectedQuestionIds = new ArrayList<>();
        Long tagId = request.getTagId();

        // 优先选择用户未尝试过的题目
        List<Question> untriedQuestions = userAttemptMapper.findUntriedQuestionsByTagId(userId, tagId);
        for (Question q : untriedQuestions) {
            selectedQuestionIds.add(q.getId());
            if (selectedQuestionIds.size() >= expectedQuestionCount) {
                break;
            }
        }

        // 如果未尝试的题目不够，添加尝试次数较少的题目
        if (selectedQuestionIds.size() < expectedQuestionCount) {
            List<Question> leastAttempted = userAttemptMapper.findLeastAttemptedQuestionsByTagId(
                    userId, tagId, expectedQuestionCount * 2);

            for (Question q : leastAttempted) {
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
        return selectedQuestionIds.subList(0, Math.min(expectedQuestionCount, selectedQuestionIds.size()));
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
                List<Question> leastAttempted = userAttemptMapper.findLeastAttemptedQuestionsByTagId(
                        userId, tagId, neededCount * 2);
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
     * 生成开场消息
     */
    private String generateOpeningMessage(Question firstQuestion) {

        String prompt = "你好！你是一位专业的技术面试官，现在正在直接与候选人对话。请以第一人称，将以下问题直接提问给候选人。不要回答问题本身，也不要提供任何指导建议或额外信息，直接提问即可。\n\n" +
                firstQuestion.getText();

        return callOpenAI(prompt);
    }

    /**
     * 生成反馈并提出下一题
     */
    private String generateFeedbackWithNextQuestion(String userAnswer, Long previousQuestionId, Question nextQuestion) {
        if (nextQuestion == null) {
            return "系统错误：无法获取下一个问题。";
        }

        // 1. 尝试使用现有的缓存方法
        if (previousQuestionId != null) {
            List<Answer> answers = answerService.getAnswersByQuestionId(previousQuestionId);
            boolean hasStandardAnswer = !answers.isEmpty();

            // 使用AIResponseCacheManager的现有方法
            Optional<String> cachedFeedback = aiCacheManager.getCachedFeedback(
                    previousQuestionId, nextQuestion.getId(), userAnswer, hasStandardAnswer);

            if (cachedFeedback.isPresent()) {
                logger.debug("使用缓存的反馈: prevQuestionId={}, nextQuestionId={}",
                        previousQuestionId, nextQuestion.getId());
                return cachedFeedback.get();
            }
        }

        // 2. 缓存未命中，生成新的反馈
        String standardAnswer = getStandardAnswerForQuestion(previousQuestionId);
        String prompt = buildFeedbackPromptWithAnswer(userAnswer, nextQuestion, standardAnswer);
        String aiResponse = callOpenAI(prompt);

        // 3. 使用现有的缓存方法
        if (aiResponse != null && !aiResponse.contains("暂时无法") && !aiResponse.contains("不可用")) {
            if (previousQuestionId != null) {
                List<Answer> answers = answerService.getAnswersByQuestionId(previousQuestionId);
                boolean hasStandardAnswer = !answers.isEmpty();

                aiCacheManager.cacheFeedback(previousQuestionId, nextQuestion.getId(),
                        userAnswer, hasStandardAnswer, aiResponse);
                logger.debug("缓存反馈回复: prevQ={}, nextQ={}, answerLength={}",
                        previousQuestionId, nextQuestion.getId(), userAnswer.length());
            }
        }

        return aiResponse;
    }

    /**
     * 获取题目的标准答案 - 复用现有逻辑
     */
    private String getStandardAnswerForQuestion(Long questionId) {
        if (questionId == null) {
            return null;
        }

        try {
            List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
            if (!answers.isEmpty()) {
                // 如果有多个答案，取第一个作为主要参考答案
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
            logger.error("获取题目答案失败: questionId=" + questionId, e);
        }

        return null;
    }

    /**
     * 构建包含标准答案的反馈提示词
     */
    private String buildFeedbackPromptWithAnswer(String userAnswer, Question nextQuestion, String standardAnswer) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("用户刚刚回答了一个面试问题。请你作为面试官：\n\n");
        prompt.append("1. 对用户的回答给出简短的反馈（1-2句话）\n");

        if (standardAnswer != null) {
            prompt.append("参考标准答案：").append(standardAnswer + "\n\n");
        }

        prompt.append("用户的回答：").append(userAnswer).append("\n\n");
        prompt.append("2. 然后提出下一个问题：\n");
        prompt.append(nextQuestion.getText()).append("\n\n");
        prompt.append("请保持专业、鼓励的语气，直接与候选人对话。");

        return prompt.toString();
    }

    /**
     * 生成最终反馈
     */
    private String generateFinalFeedback(Long sessionId, String lastAnswer, Long lastQuestionId) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("用户刚刚完成了最后一个面试问题的回答。请你作为面试官：\n\n");
        prompt.append("用户的最后回答：").append(lastAnswer).append("\n\n");

        // 添加标准答案参考
        if (lastQuestionId != null) {
            String standardAnswer = getStandardAnswerForQuestion(lastQuestionId);
            if (standardAnswer != null && !standardAnswer.trim().isEmpty()) {
                prompt.append("参考标准答案：").append(standardAnswer).append("\n\n");
            }
        }

        prompt.append("请提供：\n");
        prompt.append("1. 对最后回答的简短反馈\n");
        prompt.append("2. 对整个面试的总体评价\n");
        prompt.append("3. 主要优点和改进建议\n\n");
        prompt.append("请保持专业、客观、鼓励的语气。");

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
            logger.error("调用OpenAI API失败", e);
            return "AI服务暂时不可用：" + e.getMessage();
        }
    }

    /**
     * 保存AI消息
     */
    private MessageDTO saveAIMessage(Long sessionId, String text) {
        Message message = new Message(sessionId, MessageType.AI, text);
        messageMapper.insert(message);
        return dtoConverter.convertToMessageDTO(message);
    }

    /**
     * 保存用户消息
     */
    private MessageDTO saveUserMessage(Long sessionId, String text) {
        Message message = new Message(sessionId, MessageType.USER, text);
        messageMapper.insert(message);
        return dtoConverter.convertToMessageDTO(message);
    }
}