package com.xinyu.InterviewCoach_v2.interceptor;

import com.xinyu.InterviewCoach_v2.service.cache.RedisSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 会话缓存拦截器
 * 自动刷新活跃会话的TTL
 */
@Component
public class SessionCacheInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SessionCacheInterceptor.class);

    @Autowired
    private RedisSessionManager redisSessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 检查请求路径是否涉及会话操作
        String requestURI = request.getRequestURI();

        // 只处理会话相关的API请求
        if (requestURI.contains("/sessions/") || requestURI.contains("/chat/")) {
            try {
                // 从路径参数或请求参数中提取sessionId
                Long sessionId = extractSessionId(request);
                if (sessionId != null && redisSessionManager.existsInCache(sessionId)) {
                    // 刷新会话TTL
                    redisSessionManager.refreshSessionTtl(sessionId);
                    logger.debug("自动刷新会话TTL: sessionId={}", sessionId);
                }
            } catch (Exception e) {
                // 不影响正常请求处理
                logger.warn("拦截器处理异常: {}", e.getMessage());
            }
        }

        return true;
    }

    /**
     * 从请求中提取sessionId
     */
    private Long extractSessionId(HttpServletRequest request) {
        // 1. 尝试从路径参数获取
        String uri = request.getRequestURI();
        if (uri.contains("/sessions/")) {
            String[] parts = uri.split("/sessions/");
            if (parts.length > 1) {
                String sessionIdStr = parts[1].split("/")[0];
                try {
                    return Long.parseLong(sessionIdStr);
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }

        // 2. 尝试从请求参数获取
        String sessionIdParam = request.getParameter("sessionId");
        if (sessionIdParam != null) {
            try {
                return Long.parseLong(sessionIdParam);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        // 3. 尝试从Header获取
        String sessionIdHeader = request.getHeader("X-Session-Id");
        if (sessionIdHeader != null) {
            try {
                return Long.parseLong(sessionIdHeader);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        return null;
    }
}