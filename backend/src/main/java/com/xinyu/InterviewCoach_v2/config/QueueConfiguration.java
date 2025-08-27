package com.xinyu.InterviewCoach_v2.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列相关配置
 * 配置AI处理线程池和异步任务
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "queue.ai.enabled", havingValue = "true", matchIfMissing = true)
public class QueueConfiguration {

    /**
     * AI处理专用线程池
     * 用于处理AI相关的异步任务（OpenAI API调用、embedding计算等）
     */
    @Bean("aiProcessorExecutor")
    public Executor aiProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：保持3个线程处理高优先级AI任务
        executor.setCorePoolSize(3);

        // 最大线程数：高峰期可扩展到8个线程
        executor.setMaxPoolSize(8);

        // 队列容量：缓冲50个AI任务
        executor.setQueueCapacity(50);

        // 线程名前缀：便于日志追踪
        executor.setThreadNamePrefix("ai-processor-");

        // 拒绝策略：调用者线程执行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间：60秒后回收
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 优雅关闭：等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }

    /**
     * 通用异步处理线程池
     * 用于处理非AI的异步任务（数据更新、缓存操作等）
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("task-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);

        // 拒绝策略：抛弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();

        return executor;
    }

    /**
     * 定时任务线程池
     * 用于队列监控、缓存清理等定时任务
     */
    @Bean("scheduledTaskExecutor")
    public Executor scheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scheduled-");
        executor.setKeepAliveSeconds(120);

        executor.initialize();

        return executor;
    }

    /**
     * 新增：WebSocket响应生产者线程池（修复缺失的线程池）
     * 用于处理WebSocket消息推送到队列的异步任务
     */
    @Bean("websocketProducerExecutor")
    public Executor websocketProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 轻量级配置：WebSocket消息推送到队列是轻量操作
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(15);
        executor.setThreadNamePrefix("ws-producer-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);

        // 拒绝策略：调用者线程执行，确保消息不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();

        return executor;
    }
}