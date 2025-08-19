package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.entity.Answer;
import com.xinyu.InterviewCoach_v2.mapper.AnswerMapper;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 答案业务逻辑层
 */
@Service
public class AnswerService {

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 创建新答案
     */
    @Transactional
    public Answer createAnswer(Answer answer) {
        // 验证输入
        if (answer.getText() == null || answer.getText().trim().isEmpty()) {
            throw new RuntimeException("答案内容不能为空");
        }

        // 验证题目是否存在
        if (!questionMapper.findById(answer.getQuestionId()).isPresent()) {
            throw new RuntimeException("题目不存在");
        }

        // 处理答案文本
        answer.setText(answer.getText().trim());

        int result = answerMapper.insert(answer);
        if (result > 0) {
            return answer;
        } else {
            throw new RuntimeException("创建答案失败");
        }
    }

    /**
     * 根据ID查询答案
     */
    public Optional<Answer> getAnswerById(Long id) {
        return answerMapper.findById(id);
    }

    /**
     * 根据题目ID查询所有答案
     */
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return answerMapper.findByQuestionId(questionId);
    }

    /**
     * 查询所有答案
     */
    public List<Answer> getAllAnswers() {
        return answerMapper.findAll();
    }

    /**
     * 分页查询答案
     */
    public List<Answer> getAnswersWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return answerMapper.findWithPagination(size, offset);
    }

    /**
     * 根据关键词搜索答案
     */
    public List<Answer> searchAnswers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAnswers();
        }
        return answerMapper.findByKeyword(keyword.trim());
    }

    /**
     * 统计答案总数
     */
    public int getTotalAnswerCount() {
        return answerMapper.count();
    }

    /**
     * 统计指定题目的答案数量
     */
    public int getAnswerCountByQuestionId(Long questionId) {
        return answerMapper.countByQuestionId(questionId);
    }

    /**
     * 更新答案
     */
    @Transactional
    public Answer updateAnswer(Long id, Answer answer) {
        // 验证答案是否存在
        Optional<Answer> existingAnswer = answerMapper.findById(id);
        if (!existingAnswer.isPresent()) {
            throw new RuntimeException("答案不存在");
        }

        // 验证输入
        if (answer.getText() == null || answer.getText().trim().isEmpty()) {
            throw new RuntimeException("答案内容不能为空");
        }

        // 更新答案
        answer.setId(id);
        answer.setText(answer.getText().trim());

        int result = answerMapper.update(answer);
        if (result > 0) {
            return answerMapper.findById(id).orElse(null);
        } else {
            throw new RuntimeException("更新答案失败");
        }
    }

    /**
     * 删除答案
     */
    @Transactional
    public boolean deleteAnswer(Long id) {
        // 验证答案是否存在
        if (!answerMapper.findById(id).isPresent()) {
            throw new RuntimeException("答案不存在");
        }

        return answerMapper.deleteById(id) > 0;
    }

    /**
     * 根据题目ID删除所有答案
     */
    @Transactional
    public boolean deleteAnswersByQuestionId(Long questionId) {
        return answerMapper.deleteByQuestionId(questionId) >= 0;
    }
}