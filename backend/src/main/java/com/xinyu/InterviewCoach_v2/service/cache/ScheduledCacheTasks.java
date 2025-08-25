package com.xinyu.InterviewCoach_v2.service.cache;

import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时缓存任务
 * 负责缓存预热、清理等定时任务
 */
@Component
public class ScheduledCacheTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledCacheTasks.class);

    @Autowired
    private AIResponseCacheManager aiCacheManager;

    @Autowired
    private QuestionMapper questionMapper;

    @Value("${cache.ai.preload.enabled:true}")
    private boolean preloadEnabled;

    @Value("${cache.ai.preload.hot-questions:20}")
    private int hotQuestionsCount;

    /**
     * 预加载热门题目的第一题问法
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void preloadHotQuestions() {
        if (!preloadEnabled) {
            logger.info("缓存预加载已禁用，跳过预加载任务");
            return;
        }

        logger.info("开始预加载热门题目缓存");

        try {
            // 获取最新的热门题目（这里简单用最新的题目代替）
            List<Question> hotQuestions = questionMapper.findLatest(hotQuestionsCount);

            int preloadedCount = 0;
            for (Question question : hotQuestions) {
                // 为每种面试模式预生成第一题问法
                for (SessionMode mode : SessionMode.values()) {
                    try {
                        // 检查是否已经有缓存
                        if (aiCacheManager.getCachedFirstQuestion(question.getId(), mode).isEmpty()) {
                            // 这里可以调用AI生成并缓存，但要控制频率避免API限制
                            // 实际实现中可能需要更复杂的预加载策略
                            logger.debug("题目{}的{}模式需要预加载", question.getId(), mode);
                        }
                        preloadedCount++;
                    } catch (Exception e) {
                        logger.warn("预加载题目失败: questionId={}, mode={}", question.getId(), mode, e);
                    }
                }

                // 添加延迟，避免API调用过于频繁
                Thread.sleep(100);
            }

            logger.info("热门题目缓存预加载完成，处理了{}个缓存项", preloadedCount);

        } catch (Exception e) {
            logger.error("预加载热门题目缓存失败", e);
        }
    }

    /**
     * 清理过期和低质量缓存
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredCache() {
        logger.info("开始执行定时缓存清理任务");

        try {
            aiCacheManager.cleanupCache();
            logger.info("定时缓存清理任务完成");
        } catch (Exception e) {
            logger.error("定时缓存清理任务失败", e);
        }
    }

    /**
     * 缓存统计日志
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 每小时
    public void logCacheStats() {
        try {
            var stats = aiCacheManager.getCacheStats();
            logger.info("缓存统计: 总缓存条目={}, 第一题缓存={}, 反馈缓存={}, embedding缓存={}",
                    stats.get("totalKeys"),
                    stats.get("firstQuestionKeys"),
                    stats.get("feedbackKeys"),
                    stats.get("embeddingKeys"));
        } catch (Exception e) {
            logger.warn("获取缓存统计失败", e);
        }
    }
}