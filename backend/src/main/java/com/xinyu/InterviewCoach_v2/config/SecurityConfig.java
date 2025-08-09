package com.xinyu.InterviewCoach_v2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置类
 * 阶段1：暂时开放所有接口，专注于密码加密功能测试
 *
 * TODO: 后续需要实现JWT认证和基于角色的权限控制
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护（API不需要）
                .csrf(csrf -> csrf.disable())

                // 暂时允许所有请求访问
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )

                // 禁用默认登录页面（我们是API系统）
                .formLogin(form -> form.disable())

                // 禁用HTTP Basic认证
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}