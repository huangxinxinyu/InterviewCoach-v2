package com.xinyu.InterviewCoach_v2.util;

import com.xinyu.InterviewCoach_v2.service.UserService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // JWT Token格式为 "Bearer token"
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                logger.warn("无法获取JWT Token的用户名: " + e.getMessage());
            }
        }

        // 如果能够获取用户名且SecurityContext中没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 验证token
            if (jwtUtil.validateToken(jwtToken, username)) {

                // 从token中获取用户信息
                String role = jwtUtil.getRoleFromToken(jwtToken);
                Long userId = jwtUtil.getUserIdFromToken(jwtToken);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // 设置请求详情
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 在Spring Security上下文中设置认证信息
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // 可以在request中设置用户信息，方便后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
            }
        }

        filterChain.doFilter(request, response);
    }
}