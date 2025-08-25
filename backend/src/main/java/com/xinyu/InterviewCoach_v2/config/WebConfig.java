package com.xinyu.InterviewCoach_v2.config;

import com.xinyu.InterviewCoach_v2.interceptor.SessionCacheInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionCacheInterceptor sessionCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionCacheInterceptor)
                .addPathPatterns("/api/sessions/**", "/api/chat/**")
                .excludePathPatterns("/api/auth/**"); // 排除认证相关接口
    }
}