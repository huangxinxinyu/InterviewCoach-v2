package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.*;
import com.xinyu.InterviewCoach_v2.service.ChatService;
import com.xinyu.InterviewCoach_v2.service.SessionService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 对话控制层
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 启动新的面试会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            SessionResponse response = chatService.startSession(userId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse(response.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("启动面试失败: " + e.getMessage()));
        }
    }

    /**
     * 发送消息到指定会话
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long sessionId,
                                         @RequestBody SendMessageRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            // 验证消息内容
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("消息内容不能为空"));
            }

            ChatResponse response = chatService.processUserMessage(userId, sessionId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse(response.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> getSessionMessages(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            List<MessageDTO> messages = chatService.getSessionMessages(userId, sessionId);
            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("获取消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 结束面试会话
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            ChatResponse response = chatService.endSession(userId, sessionId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse(response.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("结束会话失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的活跃会话
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<?> getActiveSession(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            var activeSession = sessionService.getActiveSessionByUserId(userId);

            if (activeSession.isPresent()) {
                return ResponseEntity.ok(activeSession.get());
            } else {
                return ResponseEntity.ok(new SuccessResponse("当前没有活跃会话"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("获取活跃会话失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的所有会话历史
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            List<SessionDTO> sessions = sessionService.getSessionsByUserId(userId);
            return ResponseEntity.ok(sessions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("获取会话历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            // 验证会话所有权
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("无权访问此会话"));
            }

            var session = sessionService.getSessionById(sessionId);

            if (session.isPresent()) {
                return ResponseEntity.ok(session.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("获取会话失败: " + e.getMessage()));
        }
    }

    /**
     * 强制结束用户所有活跃会话
     */
    @PostMapping("/sessions/end-all")
    public ResponseEntity<?> endAllActiveSessions(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            boolean success = sessionService.endAllActiveSessionsByUserId(userId);

            if (success) {
                return ResponseEntity.ok(new SuccessResponse("所有活跃会话已结束"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("结束会话失败"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("结束会话失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否有活跃会话
     */
    @GetMapping("/sessions/has-active")
    public ResponseEntity<?> hasActiveSession(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户未认证"));
            }

            boolean hasActive = sessionService.hasActiveSession(userId);
            return ResponseEntity.ok(Map.of("hasActiveSession", hasActive));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("检查活跃会话失败: " + e.getMessage()));
        }
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return jwtUtil.getUserIdFromToken(token);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 错误响应DTO
     */
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * 成功响应DTO
     */
    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}