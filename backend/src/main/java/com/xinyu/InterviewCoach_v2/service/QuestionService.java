package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 题目业务逻辑层 - 改进后使用DTOConverter
 */
@Service
public class QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private DTOConverter dtoConverter;

    /**
     * 创建新题目
     */
    @Transactional
    public QuestionDTO createQuestion(QuestionDTO questionDTO) {
        // 验证输入
        if (!questionDTO.isValid()) {
            throw new RuntimeException("题目内容不能为空");
        }

        // 检查题目是否已存在（防重复）
        String processedText = questionDTO.getProcessedText();
        if (questionMapper.existsByText(processedText)) {
            throw new RuntimeException("该题目已存在");
        }

        Question question = dtoConverter.convertFromQuestionDTO(questionDTO);
        question.setText(processedText);

        int result = questionMapper.insert(question);
        if (result > 0) {
            return dtoConverter.convertToQuestionDTO(question);
        } else {
            throw new RuntimeException("创建题目失败");
        }
    }

    /**
     * 根据ID查询题目
     */
    public Optional<QuestionDTO> getQuestionById(Long id) {
        return questionMapper.findById(id)
                .map(dtoConverter::convertToQuestionDTO);
    }

    /**
     * 查询所有题目
     */
    public List<QuestionDTO> getAllQuestions() {
        List<Question> questions = questionMapper.findAll();
        return dtoConverter.convertToQuestionDTOList(questions);
    }

    /**
     * 根据关键词搜索题目
     */
    public List<QuestionDTO> searchQuestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllQuestions();
        }
        List<Question> questions = questionMapper.findByKeyword(keyword.trim());
        return dtoConverter.convertToQuestionDTOList(questions);
    }

    /**
     * 分页查询题目
     */
    public List<QuestionDTO> getQuestionsWithPagination(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int offset = (page - 1) * size;
        List<Question> questions = questionMapper.findWithPagination(size, offset);
        return dtoConverter.convertToQuestionDTOList(questions);
    }

    /**
     * 更新题目
     */
    @Transactional
    public QuestionDTO updateQuestion(Long id, QuestionDTO questionDTO) {
        // 验证输入
        if (!questionDTO.isValid()) {
            throw new RuntimeException("题目内容不能为空");
        }

        Optional<Question> existingQuestion = questionMapper.findById(id);
        if (existingQuestion.isEmpty()) {
            throw new RuntimeException("题目不存在");
        }

        String processedText = questionDTO.getProcessedText();

        // 检查新内容是否与其他题目重复（排除自己）
        Question existing = existingQuestion.get();
        if (!existing.getText().equals(processedText) && questionMapper.existsByText(processedText)) {
            throw new RuntimeException("该题目内容已存在");
        }

        // 更新题目内容
        existing.setText(processedText);

        int result = questionMapper.update(existing);
        if (result > 0) {
            return dtoConverter.convertToQuestionDTO(existing);
        } else {
            throw new RuntimeException("更新题目失败");
        }
    }

    /**
     * 删除题目
     */
    @Transactional
    public boolean deleteQuestion(Long id) {
        if (!questionMapper.findById(id).isPresent()) {
            throw new RuntimeException("题目不存在");
        }
        return questionMapper.deleteById(id) > 0;
    }

    /**
     * 获取题目总数
     */
    public long getQuestionCount() {
        return questionMapper.count();
    }

    /**
     * 根据关键词统计题目数量
     */
    public long getQuestionCountByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getQuestionCount();
        }
        return questionMapper.countByKeyword(keyword.trim());
    }

    /**
     * 获取最新的题目
     */
    public List<QuestionDTO> getLatestQuestions(int limit) {
        if (limit < 1) limit = 10;
        List<Question> questions = questionMapper.findLatest(limit);
        return dtoConverter.convertToQuestionDTOList(questions);
    }

    /**
     * 随机获取题目
     */
    public List<QuestionDTO> getRandomQuestions(int limit) {
        if (limit < 1) limit = 10;
        List<Question> questions = questionMapper.findRandom(limit);
        return dtoConverter.convertToQuestionDTOList(questions);
    }

    /**
     * 检查题目是否存在
     */
    public boolean questionExists(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return questionMapper.existsByText(text.trim());
    }
}