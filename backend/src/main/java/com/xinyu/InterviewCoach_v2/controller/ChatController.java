package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.dto.request.chat.SendMessageRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.chat.StartInterviewRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.chat.ChatMessageResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.chat.InterviewSessionResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.service.ChatService;
import com.xinyu.InterviewCoach_v2.service.SessionService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对话控制层 - 重构后使用统一的DTO
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
    public ResponseEntity<?> startInterview(@Valid @RequestBody StartInterviewRequestDTO request,
                                            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            InterviewSessionResponseDTO response = chatService.startInterview(userId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO(response.getMessage(), "START_INTERVIEW_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("启动面试失败: " + e.getMessage(), "START_INTERVIEW_ERROR"));
        }
    }

    /**
     * 发送消息到指定会话
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long sessionId,
                                         @Valid @RequestBody SendMessageRequestDTO request,
                                         HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            ChatMessageResponseDTO response = chatService.processMessage(userId, sessionId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO(response.getMessage(), "SEND_MESSAGE_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("发送消息失败: " + e.getMessage(), "SEND_MESSAGE_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            List<MessageDTO> messages = chatService.getSessionMessages(userId, sessionId);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(messages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("获取消息历史失败: " + e.getMessage(), "GET_MESSAGES_ERROR"));
        }
    }

    /**
     * 结束面试会话
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<?> endInterview(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            ChatMessageResponseDTO response = chatService.endInterview(userId, sessionId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO(response.getMessage(), "END_INTERVIEW_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("结束会话失败: " + e.getMessage(), "END_INTERVIEW_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            var activeSession = sessionService.getActiveSessionByUserId(userId);

            if (activeSession.isPresent()) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>(activeSession.get()));
            } else {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("当前没有活跃会话", null));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("获取活跃会话失败: " + e.getMessage(), "GET_ACTIVE_SESSION_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            List<SessionDTO> sessions = sessionService.getSessionsByUserId(userId);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(sessions));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("获取会话历史失败: " + e.getMessage(), "GET_SESSIONS_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            // 验证会话所有权
            if (!sessionService.validateSessionOwnership(sessionId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiErrorResponseDTO("无权访问此会话", "FORBIDDEN"));
            }

            var session = sessionService.getSessionById(sessionId);

            if (session.isPresent()) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>(session.get()));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("获取会话失败: " + e.getMessage(), "GET_SESSION_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            boolean success = sessionService.endAllActiveSessionsByUserId(userId);

            if (success) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("所有活跃会话已结束"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("结束会话失败", "END_ALL_SESSIONS_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("结束会话失败: " + e.getMessage(), "END_ALL_SESSIONS_ERROR"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiErrorResponseDTO("用户未认证", "UNAUTHORIZED"));
            }

            boolean hasActive = sessionService.hasActiveSession(userId);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(Map.of("hasActiveSession", hasActive)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("检查活跃会话失败: " + e.getMessage(), "CHECK_ACTIVE_SESSION_ERROR"));
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
}