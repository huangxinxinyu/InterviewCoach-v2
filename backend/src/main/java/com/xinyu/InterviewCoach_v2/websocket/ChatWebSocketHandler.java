package com.xinyu.InterviewCoach_v2.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.service.WebSocketService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // 新增：注入WebSocketService来统一管理连接
    @Autowired
    private WebSocketService webSocketService;

    // 存储会话连接: sessionId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessionConnections = new ConcurrentHashMap<>();

    // 存储用户连接: userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userConnections = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 从URL参数获取token和sessionId
            URI uri = session.getUri();
            String query = uri.getQuery();
            Map<String, String> params = parseQueryString(query);

            String token = params.get("token");
            String sessionIdStr = params.get("sessionId");

            if (token == null || sessionIdStr == null) {
                session.close(CloseStatus.BAD_DATA.withReason("缺少token或sessionId"));
                return;
            }

            // 验证JWT token
            if (!jwtUtil.isTokenValid(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("无效token"));
                return;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            Long sessionId = Long.parseLong(sessionIdStr);

            // 本地存储连接（保持兼容性）
            sessionConnections.put(sessionId, session);
            userConnections.put(userId, session);

            // 关键修复：注册到WebSocketService进行统一管理
            webSocketService.registerConnection(sessionId, userId, session);

            // 在session中存储元数据
            session.getAttributes().put("userId", userId);
            session.getAttributes().put("sessionId", sessionId);

            logger.info("WebSocket连接建立: userId={}, sessionId={}, wsSessionId={}",
                    userId, sessionId, session.getId());

            // 发送连接确认
            sendMessage(session, Map.of(
                    "type", "connection_established",
                    "sessionId", sessionId,
                    "message", "连接已建立"
            ));

        } catch (Exception e) {
            logger.error("WebSocket连接建立失败", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("连接建立失败"));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 客户端ping/pong心跳检测
        if (message instanceof PongMessage) {
            logger.debug("收到WebSocket pong: sessionId={}", session.getId());
            return;
        }

        if (message instanceof TextMessage textMessage) {
            try {
                Map<String, Object> data = objectMapper.readValue(textMessage.getPayload(), Map.class);
                String type = (String) data.get("type");

                if ("ping".equals(type)) {
                    // 回复pong
                    sendMessage(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                }

            } catch (Exception e) {
                logger.error("处理WebSocket消息失败", e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        Long sessionId = (Long) session.getAttributes().get("sessionId");

        // 本地清理连接（保持兼容性）
        if (sessionId != null) {
            sessionConnections.remove(sessionId);
        }
        if (userId != null) {
            userConnections.remove(userId);
        }

        // 关键修复：同时从WebSocketService中移除连接
        if (sessionId != null && userId != null) {
            webSocketService.removeConnection(sessionId, userId, session.getId());
        }

        logger.info("WebSocket连接关闭: userId={}, sessionId={}, reason={}",
                userId, sessionId, closeStatus.getReason());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 推送AI回复到指定会话（保持兼容性，但建议使用WebSocketService）
     */
    public void pushAIResponse(Long sessionId, String aiResponse, String currentState) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                Map<String, Object> response = Map.of(
                        "type", "ai_response",
                        "sessionId", sessionId,
                        "message", aiResponse,
                        "currentState", currentState,
                        "chatInputEnabled", !"AI_PROCESSING".equals(currentState),
                        "timestamp", System.currentTimeMillis()
                );

                sendMessage(wsSession, response);
                logger.debug("AI回复已推送: sessionId={}, state={}", sessionId, currentState);

            } catch (Exception e) {
                logger.error("推送AI回复失败: sessionId={}", sessionId, e);
            }
        } else {
            logger.warn("WebSocket会话不存在或已关闭，AI回复无法推送: sessionId={}", sessionId);
        }
    }

    /**
     * 推送会话状态更新（保持兼容性，但建议使用WebSocketService）
     */
    public void pushSessionStateUpdate(Long sessionId, String state, boolean chatEnabled) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                Map<String, Object> update = Map.of(
                        "type", "session_state_update",
                        "sessionId", sessionId,
                        "currentState", state,
                        "chatInputEnabled", chatEnabled,
                        "timestamp", System.currentTimeMillis()
                );

                sendMessage(wsSession, update);
                logger.debug("会话状态更新已推送: sessionId={}, state={}", sessionId, state);

            } catch (Exception e) {
                logger.error("推送会话状态更新失败: sessionId={}", sessionId, e);
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) throws Exception {
        String json = objectMapper.writeValueAsString(data);
        session.sendMessage(new TextMessage(json));
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    public boolean isSessionConnected(Long sessionId) {
        WebSocketSession wsSession = sessionConnections.get(sessionId);
        return wsSession != null && wsSession.isOpen();
    }
}