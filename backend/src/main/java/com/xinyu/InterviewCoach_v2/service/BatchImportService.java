package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.dto.request.admin.BatchImportRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.admin.QuestionImportDTO;
import com.xinyu.InterviewCoach_v2.entity.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BatchImportService {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TagService tagService;

    @Autowired
    private QuestionTagService questionTagService;

    @Autowired
    private AnswerService answerService;

    /**
     * 批量导入题目和答案 - 支持问题答案一体化上传
     */
    @Transactional
    public Map<String, Object> batchImportQuestions(BatchImportRequestDTO request) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        int answerCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 处理每个题目
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuestionImportDTO importDTO = request.getQuestions().get(i);
                System.out.println("=== 处理第" + (i+1) + "个题目 ===");
                System.out.println("题目文本长度: " + importDTO.getText().length());
                System.out.println("题目前50字符: " + importDTO.getText().substring(0, Math.min(50, importDTO.getText().length())));
                if (importDTO.getAnswers() != null) {
                    System.out.println("答案数量: " + importDTO.getAnswers().size());
                    for (int j = 0; j < importDTO.getAnswers().size(); j++) {
                        System.out.println("答案" + (j+1) + "长度: " + importDTO.getAnswers().get(j).length());
                    }
                }

                try {
                    // 检查是否重复
                    if (isQuestionExists(importDTO.getText())) {
                        skipCount++;
                        continue;
                    }

                    // 创建题目
                    QuestionDTO questionDTO = new QuestionDTO();
                    questionDTO.setText(importDTO.getText());
                    QuestionDTO createdQuestion = questionService.createQuestion(questionDTO);

                    // 处理标签
                    if (importDTO.getTags() != null && !importDTO.getTags().isEmpty()) {
                        for (String tagName : importDTO.getTags()) {
                            try {
                                // 尝试获取现有标签，不存在则创建
                                TagDTO tag = tagService.getTagByName(tagName).orElseGet(() -> {
                                    TagDTO newTag = new TagDTO(tagName);
                                    return tagService.createTag(newTag);
                                });

                                // 关联题目和标签
                                questionTagService.addTagToQuestion(createdQuestion.getId(), tag.getId());
                            } catch (Exception e) {
                                // 标签处理失败不影响题目创建
                                System.err.println("处理标签失败: " + tagName + ", 错误: " + e.getMessage());
                            }
                        }
                    }

                    // 处理答案 - 新增逻辑
                    if (importDTO.getAnswers() != null && !importDTO.getAnswers().isEmpty()) {
                        for (String answerText : importDTO.getAnswers()) {
                            try {
                                if (answerText != null && !answerText.trim().isEmpty()) {
                                    // 创建答案实体
                                    Answer answer = new Answer();
                                    answer.setQuestionId(createdQuestion.getId());
                                    answer.setText(answerText.trim());

                                    // 保存答案
                                    answerService.createAnswer(answer);
                                    answerCount++;
                                }
                            } catch (Exception e) {
                                // 答案处理失败不影响题目创建，记录警告
                                System.err.println("处理答案失败: " + answerText + ", 错误: " + e.getMessage());
                                errors.add("题目 " + (i + 1) + " 的答案处理失败: " + e.getMessage());
                            }
                        }
                    }

                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + (i + 1) + "个题目导入失败: " + e.getMessage());
                }
            }

            // 构建返回结果
            result.put("success", failCount == 0);
            result.put("total", request.getQuestions().size());
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("failCount", failCount);
            result.put("answerCount", answerCount); // 新增：答案导入数量统计
            result.put("errors", errors);

            String message;
            if (failCount == 0) {
                message = String.format("导入完成！成功: %d题目, %d答案, 跳过: %d",
                        successCount, answerCount, skipCount);
            } else {
                message = String.format("部分成功！成功: %d题目, %d答案, 跳过: %d, 失败: %d",
                        successCount, answerCount, skipCount, failCount);
            }
            result.put("message", message);

        } catch (Exception e) {
            System.err.println("批量导入过程发生异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "批量导入失败: " + e.getMessage());
            result.put("errors", List.of(e.getMessage()));
        }

        return result;
    }

    /**
     * 检查题目是否已存在
     */
    private boolean isQuestionExists(String text) {
        try {
            // 简单的重复检查：搜索前50个字符
            String searchText = text.length() > 50 ? text.substring(0, 50) : text;
            List<QuestionDTO> existingQuestions = questionService.searchQuestions(searchText);
            return existingQuestions.stream()
                    .anyMatch(q -> q.getText().trim().equalsIgnoreCase(text.trim()));
        } catch (Exception e) {
            return false;
        }
    }
}