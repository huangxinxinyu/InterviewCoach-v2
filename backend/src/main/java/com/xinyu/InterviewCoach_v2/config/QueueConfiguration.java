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
 * 队列相关配置 - 修复线程池资源管理
 * 配置AI处理线程池和异步任务
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "queue.ai.enabled", havingValue = "true", matchIfMissing = true)
public class QueueConfiguration {

    /**
     * AI处理专用线程池 - 修复资源管理
     * 用于处理AI相关的异步任务（OpenAI API调用、embedding计算等）
     */
    @Bean("aiProcessorExecutor")
    public Executor aiProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 修复：增加核心线程数，确保有足够资源处理并发请求
        executor.setCorePoolSize(5);  // 增加到5个核心线程

        // 修复：适当增加最大线程数
        executor.setMaxPoolSize(12);  // 增加到12个最大线程

        // 修复：增加队列容量，缓冲更多任务
        executor.setQueueCapacity(100);  // 增加到100个任务缓冲

        // 线程名前缀：便于日志追踪和问题排查
        executor.setThreadNamePrefix("ai-processor-");

        // 修复：改为AbortPolicy，快速失败并记录日志
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        // 线程空闲时间：减少到30秒，更快回收
        executor.setKeepAliveSeconds(30);

        // 允许核心线程超时，在空闲时回收资源
        executor.setAllowCoreThreadTimeOut(true);

        // 优雅关闭：等待任务完成，增加等待时间
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);  // 增加到60秒

        executor.initialize();

        return executor;
    }

    /**
     * WebSocket响应专用线程池 - 新增
     * 用于处理WebSocket消息推送任务
     */
    @Bean("websocketProducerExecutor")
    public Executor websocketProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // WebSocket推送任务相对轻量，核心线程数可以少一些
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix("ws-producer-");

        // WebSocket推送失败可以丢弃，使用DiscardPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());

        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 通用异步处理线程池 - 优化配置
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
     * 定时任务线程池 - 修复配置
     * 用于队列监控、缓存清理等定时任务
     */
    @Bean("scheduledExecutor")
    public Executor scheduledExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 定时任务通常不需要太多线程
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scheduled-");

        // 定时任务失败时使用调用者线程执行，保证任务执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setKeepAliveSeconds(120);  // 定时任务线程可以保持更长时间
        executor.setAllowCoreThreadTimeOut(false);  // 核心线程不超时

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Redis Stream监控线程池 - 新增
     * 专门用于监控Redis Stream状态和死信处理
     */
    @Bean("streamMonitorExecutor")
    public Executor streamMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("stream-monitor-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setKeepAliveSeconds(300);  // 监控线程保持5分钟
        executor.setAllowCoreThreadTimeOut(false);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        executor.initialize();
        return executor;
    }
}