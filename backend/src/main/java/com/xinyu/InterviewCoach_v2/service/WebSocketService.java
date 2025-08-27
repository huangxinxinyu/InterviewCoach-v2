// WebSocketService.java - WebSocket消息推送服务
package com.xinyu.InterviewCoach_v2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息推送服务
 * 负责管理WebSocket连接和推送消息
 */
@Service
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired
    private ObjectMapper objectMapper;

    // 存储会话连接映射: sessionId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessionConnections = new ConcurrentHashMap<>();

    // 存储用户连接映射: userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userConnections = new ConcurrentHashMap<>();

    // 存储WebSocket会话元数据: wsSessionId -> metadata
    private final Map<String, Map<String, Object>> sessionMetadata = new ConcurrentHashMap<>();

    /**
     * 注册新的WebSocket连接
     */
    public void registerConnection(Long sessionId, Long userId, WebSocketSession wsSession) {
        try {
            // 移除旧连接（如果存在）
            removeOldConnections(sessionId, userId);

            // 注册新连接
            sessionConnections.put(sessionId, wsSession);
            userConnections.put(userId, wsSession);

            // 存储连接元数据
            Map<String, Object> metadata = Map.of(
                    "sessionId", sessionId,
                    "userId", userId,
                    "connectedAt", System.currentTimeMillis(),
                    "lastActivity", System.currentTimeMillis()
            );
            sessionMetadata.put(wsSession.getId(), metadata);

            // 在WebSocket session中存储业务信息
            wsSession.getAttributes().put("sessionId", sessionId);
            wsSession.getAttributes().put("userId", userId);

            logger.info("WebSocket连接已注册: sessionId={}, userId={}, wsSessionId={}",
                    sessionId, userId, wsSession.getId());

        } catch (Exception e) {
            logger.error("注册WebSocket连接失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }

    /**
     * 移除WebSocket连接
     */
    public void removeConnection(Long sessionId, Long userId, String wsSessionId) {
        try {
            if (sessionId != null) {
                sessionConnections.remove(sessionId);
            }
            if (userId != null) {
                userConnections.remove(userId);
            }
            if (wsSessionId != null) {
                sessionMetadata.remove(wsSessionId);
            }

            logger.info("WebSocket连接已移除: sessionId={}, userId={}, wsSessionId={}",
                    sessionId, userId, wsSessionId);

        } catch (Exception e) {
            logger.error("移除WebSocket连接失败", e);
        }
    }

    /**
     * 推送AI回复到指定会话
     */
    public boolean pushAIResponse(Long sessionId, String aiResponse, String currentState) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);

        if (wsSession == null || !wsSession.isOpen()) {
            logger.warn("WebSocket会话不存在或已关闭: sessionId={}", sessionId);
            return false;
        }

        try {
            Map<String, Object> message = Map.of(
                    "type", "ai_response",
                    "sessionId", sessionId,
                    "message", aiResponse,
                    "currentState", currentState,
                    "chatInputEnabled", !"AI_PROCESSING".equals(currentState),
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            wsSession.sendMessage(new TextMessage(jsonMessage));

            // 更新最后活动时间
            updateLastActivity(wsSession.getId());

            logger.debug("AI回复已推送: sessionId={}, state={}, messageLength={}",
                    sessionId, currentState, aiResponse.length());

            return true;

        } catch (Exception e) {
            logger.error("推送AI回复失败: sessionId={}", sessionId, e);
            // 连接异常时清理
            removeConnection(sessionId, null, wsSession.getId());
            return false;
        }
    }

    /**
     * 推送会话状态更新
     */
    public boolean pushSessionStateUpdate(Long sessionId, String state, boolean chatEnabled) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);

        if (wsSession == null || !wsSession.isOpen()) {
            logger.warn("WebSocket会话不存在，状态更新无法推送: sessionId={}", sessionId);
            return false;
        }

        try {
            Map<String, Object> message = Map.of(
                    "type", "session_state_update",
                    "sessionId", sessionId,
                    "currentState", state,
                    "chatInputEnabled", chatEnabled,
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            wsSession.sendMessage(new TextMessage(jsonMessage));

            updateLastActivity(wsSession.getId());

            logger.debug("会话状态更新已推送: sessionId={}, state={}, chatEnabled={}",
                    sessionId, state, chatEnabled);

            return true;

        } catch (Exception e) {
            logger.error("推送会话状态更新失败: sessionId={}", sessionId, e);
            removeConnection(sessionId, null, wsSession.getId());
            return false;
        }
    }

    /**
     * 推送系统通知给用户
     */
    public boolean pushUserNotification(Long userId, String message, String type) {
        WebSocketSession wsSession = userConnections.get(userId);

        if (wsSession == null || !wsSession.isOpen()) {
            logger.warn("用户WebSocket连接不存在: userId={}", userId);
            return false;
        }

        try {
            Map<String, Object> notification = Map.of(
                    "type", "notification",
                    "userId", userId,
                    "message", message,
                    "notificationType", type,
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = objectMapper.writeValueAsString(notification);
            wsSession.sendMessage(new TextMessage(jsonMessage));

            updateLastActivity(wsSession.getId());

            logger.debug("用户通知已推送: userId={}, type={}", userId, type);

            return true;

        } catch (Exception e) {
            logger.error("推送用户通知失败: userId={}", userId, e);
            removeConnection(null, userId, wsSession.getId());
            return false;
        }
    }

    /**
     * 推送AI处理状态（如：正在思考中）
     */
    public boolean pushAIProcessingStatus(Long sessionId, String status, String details) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);

        if (wsSession == null || !wsSession.isOpen()) {
            return false;
        }

        try {
            Map<String, Object> message = Map.of(
                    "type", "ai_processing_status",
                    "sessionId", sessionId,
                    "status", status,
                    "progress", details != null ? details : "",
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            wsSession.sendMessage(new TextMessage(jsonMessage));

            updateLastActivity(wsSession.getId());

            logger.debug("AI处理状态已推送: sessionId={}, status={}", sessionId, status);

            return true;

        } catch (Exception e) {
            logger.error("推送AI处理状态失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    /**
     * 检查会话是否有WebSocket连接
     */
    public boolean isSessionConnected(Long sessionId) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * 检查用户是否有WebSocket连接
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession wsSession = userConnections.get(userId);
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * 获取连接统计信息
     */
    public Map<String, Object> getConnectionStats() {
        int activeSessionConnections = 0;
        int activeUserConnections = 0;

        // 统计活跃的会话连接
        for (WebSocketSession session : sessionConnections.values()) {
            if (session.isOpen()) {
                activeSessionConnections++;
            }
        }

        // 统计活跃的用户连接
        for (WebSocketSession session : userConnections.values()) {
            if (session.isOpen()) {
                activeUserConnections++;
            }
        }

        return Map.of(
                "totalSessionConnections", sessionConnections.size(),
                "activeSessionConnections", activeSessionConnections,
                "totalUserConnections", userConnections.size(),
                "activeUserConnections", activeUserConnections,
                "totalMetadata", sessionMetadata.size()
        );
    }

    /**
     * 清理无效连接
     */
    public void cleanupInactiveConnections() {
        long currentTime = System.currentTimeMillis();
        long maxInactiveTime = 300000; // 5分钟无活动就清理

        Set<String> toRemove = new HashSet<>();

        for (Map.Entry<String, Map<String, Object>> entry : sessionMetadata.entrySet()) {
            String wsSessionId = entry.getKey();
            Map<String, Object> metadata = entry.getValue();

            Long lastActivity = (Long) metadata.get("lastActivity");
            if (lastActivity != null && (currentTime - lastActivity) > maxInactiveTime) {
                toRemove.add(wsSessionId);

                Long sessionId = (Long) metadata.get("sessionId");
                Long userId = (Long) metadata.get("userId");
                removeConnection(sessionId, userId, wsSessionId);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.info("清理无效WebSocket连接: count={}", toRemove.size());
        }
    }

    /**
     * 广播系统消息给所有连接的用户
     */
    public int broadcastSystemMessage(String message, String type) {
        int sentCount = 0;

        Map<String, Object> notification = Map.of(
                "type", "system_broadcast",
                "message", message,
                "notificationType", type,
                "timestamp", System.currentTimeMillis()
        );

        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);

            for (WebSocketSession wsSession : userConnections.values()) {
                if (wsSession.isOpen()) {
                    try {
                        wsSession.sendMessage(new TextMessage(jsonMessage));
                        sentCount++;
                    } catch (Exception e) {
                        logger.warn("广播消息失败: wsSessionId={}", wsSession.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("广播系统消息失败", e);
        }

        logger.info("系统消息广播完成: type={}, sentCount={}", type, sentCount);
        return sentCount;
    }

    // ===== 私有辅助方法 =====

    /**
     * 移除旧连接（处理重复连接）
     */
    private void removeOldConnections(Long sessionId, Long userId) {
        // 移除相同sessionId的旧连接
        WebSocketSession oldSessionConnection = sessionConnections.get(sessionId);
        if (oldSessionConnection != null && oldSessionConnection.isOpen()) {
            try {
                oldSessionConnection.close();
            } catch (Exception e) {
                logger.warn("关闭旧会话连接失败: sessionId={}", sessionId, e);
            }
        }

        // 移除相同userId的旧连接
        WebSocketSession oldUserConnection = userConnections.get(userId);
        if (oldUserConnection != null && oldUserConnection.isOpen()) {
            try {
                oldUserConnection.close();
            } catch (Exception e) {
                logger.warn("关闭旧用户连接失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 更新连接的最后活动时间
     */
    private void updateLastActivity(String wsSessionId) {
        Map<String, Object> metadata = sessionMetadata.get(wsSessionId);
        if (metadata != null) {
            // 创建新的metadata map（因为原map可能是不可变的）
            Map<String, Object> updatedMetadata = new HashMap<>(metadata);
            updatedMetadata.put("lastActivity", System.currentTimeMillis());
            sessionMetadata.put(wsSessionId, updatedMetadata);
        }
    }

    /**
     * 发送JSON消息的通用方法
     */
    private boolean sendJsonMessage(WebSocketSession wsSession, Map<String, Object> data) {
        if (wsSession == null || !wsSession.isOpen()) {
            return false;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(data);
            wsSession.sendMessage(new TextMessage(jsonMessage));
            updateLastActivity(wsSession.getId());
            return true;
        } catch (Exception e) {
            logger.error("发送WebSocket消息失败: wsSessionId={}", wsSession.getId(), e);
            return false;
        }
    }

    /**
     * 检查并清理单个连接
     */
    private void validateAndCleanConnection(WebSocketSession wsSession, Long sessionId, Long userId) {
        if (wsSession == null || !wsSession.isOpen()) {
            removeConnection(sessionId, userId, wsSession != null ? wsSession.getId() : null);
        }
    }
}