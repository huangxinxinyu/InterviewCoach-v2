package com.xinyu.InterviewCoach_v2.config;

import com.xinyu.InterviewCoach_v2.util.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（因为我们使用JWT）
                .csrf(csrf -> csrf.disable())

                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置会话管理（无状态）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开端点 - 不需要认证
                        .requestMatchers(
                                "/api/users/login",           // 登录接口
                                "/api/users/register",
                                "/api/users/check-email/**", // 检查邮箱是否存在
                                "/api/users/validate-token", // Token验证（可选择是否公开）
                                "/error"                     // 错误页面
                        ).permitAll()

                        // 管理员专用端点
                        .requestMatchers(
                                "/api/users/count",          // 获取用户总数
                                "/api/users/role/**"         // 根据角色查询用户
                        ).hasRole("ADMIN")

                        // 需要认证的端点
                        .requestMatchers(
                                "/api/users/**"              // 其他用户相关操作
                        ).authenticated()

                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的域名
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许发送认证信息
        configuration.setAllowCredentials(true);

        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}