// AIQueueProperties.java - AI队列配置属性类
package com.xinyu.InterviewCoach_v2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI队列配置属性
 * 映射application.yml中的queue.ai配置
 */
@Component
@ConfigurationProperties(prefix = "queue.ai")
public class AIQueueProperties {

    private boolean enabled = true;
    private Streams streams = new Streams();
    private Consumer consumer = new Consumer();
    private Processors processors = new Processors();
    private Topics topics = new Topics();

    // 主类getter/setter
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Streams getStreams() {
        return streams;
    }

    public void setStreams(Streams streams) {
        this.streams = streams;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Processors getProcessors() {
        return processors;
    }

    public void setProcessors(Processors processors) {
        this.processors = processors;
    }

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    /**
     * Redis Stream配置
     */
    public static class Streams {
        private String requests = "ai:requests";
        private String responses = "ai:responses";

        public String getRequests() {
            return requests;
        }

        public void setRequests(String requests) {
            this.requests = requests;
        }

        public String getResponses() {
            return responses;
        }

        public void setResponses(String responses) {
            this.responses = responses;
        }
    }

    /**
     * 消费者配置
     */
    public static class Consumer {
        private String groupName = "ai-service-group";
        private String consumerName = "ai-consumer";
        private int maxMessages = 10;
        private long blockTimeout = 2000;
        private boolean autoAck = false;

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getConsumerName() {
            return consumerName;
        }

        public void setConsumerName(String consumerName) {
            this.consumerName = consumerName;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public long getBlockTimeout() {
            return blockTimeout;
        }

        public void setBlockTimeout(long blockTimeout) {
            this.blockTimeout = blockTimeout;
        }

        public boolean isAutoAck() {
            return autoAck;
        }

        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }
    }

    /**
     * 处理器配置
     */
    public static class Processors {
        private int threadPoolSize = 5;
        private int maxRetries = 3;
        private int retryDelaySeconds = 5;

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryDelaySeconds() {
            return retryDelaySeconds;
        }

        public void setRetryDelaySeconds(int retryDelaySeconds) {
            this.retryDelaySeconds = retryDelaySeconds;
        }
    }

    /**
     * Topic配置
     */
    public static class Topics {
        private TopicConfig questionGeneration = new TopicConfig(true, "high");
        private TopicConfig feedbackGeneration = new TopicConfig(true, "high");
        private EmbeddingTopicConfig embeddingCalculation = new EmbeddingTopicConfig(true, "medium", 10);
        private TopicConfig finalEvaluation = new TopicConfig(true, "low");

        public TopicConfig getQuestionGeneration() {
            return questionGeneration;
        }

        public void setQuestionGeneration(TopicConfig questionGeneration) {
            this.questionGeneration = questionGeneration;
        }

        public TopicConfig getFeedbackGeneration() {
            return feedbackGeneration;
        }

        public void setFeedbackGeneration(TopicConfig feedbackGeneration) {
            this.feedbackGeneration = feedbackGeneration;
        }

        public EmbeddingTopicConfig getEmbeddingCalculation() {
            return embeddingCalculation;
        }

        public void setEmbeddingCalculation(EmbeddingTopicConfig embeddingCalculation) {
            this.embeddingCalculation = embeddingCalculation;
        }

        public TopicConfig getFinalEvaluation() {
            return finalEvaluation;
        }

        public void setFinalEvaluation(TopicConfig finalEvaluation) {
            this.finalEvaluation = finalEvaluation;
        }

        /**
         * 基础Topic配置
         */
        public static class TopicConfig {
            private boolean enabled;
            private String priority;

            public TopicConfig() {}

            public TopicConfig(boolean enabled, String priority) {
                this.enabled = enabled;
                this.priority = priority;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getPriority() {
                return priority;
            }

            public void setPriority(String priority) {
                this.priority = priority;
            }
        }

        /**
         * Embedding专用Topic配置
         */
        public static class EmbeddingTopicConfig extends TopicConfig {
            private int batchSize;

            public EmbeddingTopicConfig() {
                super();
            }

            public EmbeddingTopicConfig(boolean enabled, String priority, int batchSize) {
                super(enabled, priority);
                this.batchSize = batchSize;
            }

            public int getBatchSize() {
                return batchSize;
            }

            public void setBatchSize(int batchSize) {
                this.batchSize = batchSize;
            }
        }
    }
}