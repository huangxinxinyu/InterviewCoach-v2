package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.QuestionSetDTO;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.QuestionSet;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import com.xinyu.InterviewCoach_v2.mapper.QuestionSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 题集业务逻辑层
 */
@Service
public class QuestionSetService {

    @Autowired
    private QuestionSetMapper questionSetMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 创建新题集
     */
    @Transactional
    public QuestionSetDTO createQuestionSet(String name, String description, List<Long> questionIds) {
        // 创建题集
        QuestionSet questionSet = new QuestionSet(name, description);
        int result = questionSetMapper.insert(questionSet);

        if (result > 0) {
            // 如果提供了题目ID列表，添加题目到题集
            if (questionIds != null && !questionIds.isEmpty()) {
                // 验证所有题目是否存在
                for (Long questionId : questionIds) {
                    if (!questionMapper.findById(questionId).isPresent()) {
                        throw new RuntimeException("题目ID " + questionId + " 不存在");
                    }
                }
                questionSetMapper.addQuestionsToSet(questionSet.getId(), questionIds);
            }

            return convertToDTO(questionSet);
        } else {
            throw new RuntimeException("创建题集失败");
        }
    }

    /**
     * 根据ID获取题集（包含题目列表）
     */
    public Optional<QuestionSetDTO> getQuestionSetById(Long id, Long userId) {
        Optional<QuestionSet> questionSetOpt = questionSetMapper.findById(id);

        if (questionSetOpt.isPresent()) {
            QuestionSetDTO dto = convertToDTO(questionSetOpt.get());

            // 获取题集中的题目
            List<Question> questions = questionSetMapper.findQuestionsBySetId(id);
            dto.setQuestions(questions.stream()
                    .map(this::convertQuestionToDTO)
                    .collect(Collectors.toList()));

            // 设置题目数量
            dto.setQuestionCount(questions.size());

            // 检查用户是否已收藏（如果提供了userId）
            if (userId != null) {
                dto.setIsCollected(questionSetMapper.isCollectedByUser(id, userId));
            }

            // 获取收藏次数
            dto.setCollectionCount(questionSetMapper.getCollectionCountBySetId(id));

            return Optional.of(dto);
        }

        return Optional.empty();
    }

    /**
     * 获取所有题集
     */
    public List<QuestionSetDTO> getAllQuestionSets(Long userId) {
        return questionSetMapper.findAll().stream()
                .map(questionSet -> {
                    QuestionSetDTO dto = convertToDTO(questionSet);
                    dto.setQuestionCount(questionSetMapper.getQuestionCountBySetId(questionSet.getId()));
                    dto.setCollectionCount(questionSetMapper.getCollectionCountBySetId(questionSet.getId()));

                    if (userId != null) {
                        dto.setIsCollected(questionSetMapper.isCollectedByUser(questionSet.getId(), userId));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 向题集添加题目
     */
    @Transactional
    public boolean addQuestionsToSet(Long questionSetId, List<Long> questionIds) {
        // 验证题集是否存在
        if (!questionSetMapper.findById(questionSetId).isPresent()) {
            throw new RuntimeException("题集不存在");
        }

        // 过滤掉已存在的题目
        List<Long> newQuestionIds = questionIds.stream()
                .filter(qId -> !questionSetMapper.isQuestionInSet(questionSetId, qId))
                .collect(Collectors.toList());

        if (newQuestionIds.isEmpty()) {
            return true; // 所有题目都已存在
        }

        // 验证所有新题目是否存在
        for (Long questionId : newQuestionIds) {
            if (!questionMapper.findById(questionId).isPresent()) {
                throw new RuntimeException("题目ID " + questionId + " 不存在");
            }
        }

        return questionSetMapper.addQuestionsToSet(questionSetId, newQuestionIds) > 0;
    }

    /**
     * 从题集移除题目
     */
    @Transactional
    public boolean removeQuestionFromSet(Long questionSetId, Long questionId) {
        return questionSetMapper.removeQuestionFromSet(questionSetId, questionId) > 0;
    }

    /**
     * 设置题集的题目（覆盖原有题目）
     */
    @Transactional
    public boolean setQuestionSetContent(Long questionSetId, List<Long> questionIds) {
        // 验证题集是否存在
        if (!questionSetMapper.findById(questionSetId).isPresent()) {
            throw new RuntimeException("题集不存在");
        }

        // 清空原有题目
        questionSetMapper.clearQuestionSet(questionSetId);

        // 添加新题目
        if (questionIds != null && !questionIds.isEmpty()) {
            // 验证所有题目是否存在
            for (Long questionId : questionIds) {
                if (!questionMapper.findById(questionId).isPresent()) {
                    throw new RuntimeException("题目ID " + questionId + " 不存在");
                }
            }
            return questionSetMapper.addQuestionsToSet(questionSetId, questionIds) > 0;
        }

        return true;
    }

    /**
     * 用户收藏题集
     */
    @Transactional
    public boolean collectQuestionSet(Long questionSetId, Long userId) {
        // 验证题集是否存在
        if (!questionSetMapper.findById(questionSetId).isPresent()) {
            throw new RuntimeException("题集不存在");
        }

        // 检查是否已收藏
        if (questionSetMapper.isCollectedByUser(questionSetId, userId)) {
            throw new RuntimeException("已收藏该题集");
        }

        return questionSetMapper.collectQuestionSet(questionSetId, userId) > 0;
    }

    /**
     * 用户取消收藏题集
     */
    @Transactional
    public boolean uncollectQuestionSet(Long questionSetId, Long userId) {
        // 检查是否已收藏
        if (!questionSetMapper.isCollectedByUser(questionSetId, userId)) {
            throw new RuntimeException("未收藏该题集");
        }

        return questionSetMapper.uncollectQuestionSet(questionSetId, userId) > 0;
    }

    /**
     * 获取用户收藏的题集
     */
    public List<QuestionSetDTO> getUserCollectedSets(Long userId) {
        return questionSetMapper.findCollectedSetsByUserId(userId).stream()
                .map(questionSet -> {
                    QuestionSetDTO dto = convertToDTO(questionSet);
                    dto.setQuestionCount(questionSetMapper.getQuestionCountBySetId(questionSet.getId()));
                    dto.setCollectionCount(questionSetMapper.getCollectionCountBySetId(questionSet.getId()));
                    dto.setIsCollected(true); // 这些都是用户收藏的
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取题集中的题目ID列表（用于面试流程）
     */
    public List<Long> getQuestionIdsBySetId(Long questionSetId) {
        return questionSetMapper.findQuestionsBySetId(questionSetId).stream()
                .map(Question::getId)
                .collect(Collectors.toList());
    }

    /**
     * 更新题集信息
     */
    @Transactional
    public QuestionSetDTO updateQuestionSet(Long id, String name, String description) {
        Optional<QuestionSet> existingSet = questionSetMapper.findById(id);
        if (existingSet.isEmpty()) {
            throw new RuntimeException("题集不存在");
        }

        QuestionSet questionSet = existingSet.get();
        questionSet.setName(name);
        questionSet.setDescription(description);

        int result = questionSetMapper.update(questionSet);
        if (result > 0) {
            return convertToDTO(questionSet);
        } else {
            throw new RuntimeException("更新题集失败");
        }
    }

    /**
     * 删除题集
     */
    @Transactional
    public boolean deleteQuestionSet(Long id) {
        if (!questionSetMapper.findById(id).isPresent()) {
            throw new RuntimeException("题集不存在");
        }
        return questionSetMapper.deleteById(id) > 0;
    }

    /**
     * 转换为DTO
     */
    private QuestionSetDTO convertToDTO(QuestionSet questionSet) {
        return new QuestionSetDTO(
                questionSet.getId(),
                questionSet.getName(),
                questionSet.getDescription(),
                questionSet.getCreatedAt(),
                questionSet.getUpdatedAt()
        );
    }

    /**
     * 转换Question为QuestionDTO
     */
    private QuestionDTO convertQuestionToDTO(Question question) {
        return new QuestionDTO(
                question.getId(),
                question.getText(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}