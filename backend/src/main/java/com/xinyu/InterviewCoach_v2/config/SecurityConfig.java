package com.xinyu.InterviewCoach_v2.config;

import com.xinyu.InterviewCoach_v2.util.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                                "/api/users/register",        // 注册接口
                                "/api/users/check-email/**",  // 检查邮箱是否存在
                                "/api/users/validate-token",  // Token验证
                                "/error"                      // 错误页面
                        ).permitAll()

                        // 题目相关 - 只读操作允许认证用户访问
                        .requestMatchers(HttpMethod.GET,
                                "/api/questions",             // 查询所有题目
                                "/api/questions/*",           // 根据ID查询题目
                                "/api/questions/search",      // 搜索题目
                                "/api/questions/page",        // 分页查询
                                "/api/questions/count",       // 获取题目总数
                                "/api/questions/count/search", // 根据关键词统计
                                "/api/questions/latest",      // 获取最新题目
                                "/api/questions/random",      // 随机获取题目
                                "/api/questions/exists"       // 检查题目是否存在
                        ).authenticated()

                        // 题目相关 - 写操作只允许管理员
                        .requestMatchers(HttpMethod.POST, "/api/questions").hasRole("ADMIN")    // 创建题目
                        .requestMatchers(HttpMethod.PUT, "/api/questions/*").hasRole("ADMIN")   // 更新题目
                        .requestMatchers(HttpMethod.DELETE, "/api/questions/*").hasRole("ADMIN") // 删除题目

                        // 标签相关 - 只读操作允许认证用户访问
                        .requestMatchers(HttpMethod.GET,
                                "/api/tags",                  // 查询所有标签
                                "/api/tags/*",                // 根据ID查询标签
                                "/api/tags/name/*",           // 根据名称查询标签
                                "/api/tags/search",           // 搜索标签
                                "/api/tags/page",             // 分页查询
                                "/api/tags/count",            // 获取标签总数
                                "/api/tags/count/search",     // 根据关键词统计
                                "/api/tags/exists",           // 检查标签是否存在
                                "/api/tags/most-used",        // 获取最常用标签
                                "/api/tags/unused",           // 获取未使用标签
                                "/api/tags/by-question/*"     // 根据题目ID查询标签
                        ).authenticated()

                        // 标签相关 - 写操作只允许管理员
                        .requestMatchers(HttpMethod.POST, "/api/tags").hasRole("ADMIN")         // 创建标签
                        .requestMatchers(HttpMethod.PUT, "/api/tags/*").hasRole("ADMIN")        // 更新标签
                        .requestMatchers(HttpMethod.DELETE, "/api/tags/*").hasRole("ADMIN")     // 删除标签
                        .requestMatchers(HttpMethod.POST, "/api/tags/batch").hasRole("ADMIN")   // 批量创建标签

                        // 题目标签关联 - 只读操作允许认证用户访问
                        .requestMatchers(HttpMethod.GET,
                                "/api/question-tags/questions/*/tags",           // 获取题目的标签
                                "/api/question-tags/tags/*/questions",           // 获取标签的题目
                                "/api/question-tags/questions/by-tag-name",      // 根据标签名称查询题目
                                "/api/question-tags/tags/hot",                   // 获取热门标签
                                "/api/question-tags/tags/orphan",                // 获取孤立标签
                                "/api/question-tags/questions/untagged",         // 获取无标签题目
                                "/api/question-tags/questions/*/tags/count",     // 获取题目标签数量
                                "/api/question-tags/tags/*/questions/count",     // 获取标签题目数量
                                "/api/question-tags/relations/count",            // 统计关联总数
                                "/api/question-tags/questions/*/tags/*/exists"   // 检查关联是否存在
                        ).authenticated()

                        // 题目标签关联 - 写操作只允许管理员
                        .requestMatchers(HttpMethod.POST,
                                "/api/question-tags/questions/*/tags/*",         // 添加单个标签
                                "/api/question-tags/questions/*/tags",           // 批量添加标签
                                "/api/question-tags/questions/*/tags/by-name",   // 通过名称添加标签
                                "/api/question-tags/questions/by-all-tags",      // 查询包含所有标签的题目
                                "/api/question-tags/questions/by-any-tags"       // 查询包含任一标签的题目
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/question-tags/questions/*/tags"            // 设置题目标签
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/question-tags/questions/*/tags/*",         // 移除单个标签
                                "/api/question-tags/questions/*/tags",           // 批量移除标签
                                "/api/question-tags/questions/*/tags/all",       // 移除所有标签
                                "/api/question-tags/tags/orphan/cleanup"         // 清理孤立标签
                        ).hasRole("ADMIN")

                        // 题集相关 - 只读操作允许认证用户访问
                        .requestMatchers(HttpMethod.GET,
                                "/api/question-sets",                 // 查询所有题集
                                "/api/question-sets/*",               // 根据ID查询题集
                                "/api/question-sets/*/question-ids",  // 获取题集的题目ID列表
                                "/api/question-sets/collections"      // 获取用户收藏的题集
                        ).authenticated()

                        // 题集相关 - 用户可以收藏/取消收藏
                        .requestMatchers(HttpMethod.POST, "/api/question-sets/*/collect").authenticated()   // 收藏题集
                        .requestMatchers(HttpMethod.DELETE, "/api/question-sets/*/collect").authenticated() // 取消收藏题集

                        // 题集相关 - 写操作只允许管理员
                        .requestMatchers(HttpMethod.POST, "/api/question-sets").hasRole("ADMIN")            // 创建题集
                        .requestMatchers(HttpMethod.PUT, "/api/question-sets/*").hasRole("ADMIN")           // 更新题集
                        .requestMatchers(HttpMethod.DELETE, "/api/question-sets/*").hasRole("ADMIN")        // 删除题集
                        .requestMatchers(HttpMethod.POST, "/api/question-sets/*/questions").hasRole("ADMIN") // 向题集添加题目
                        .requestMatchers(HttpMethod.PUT, "/api/question-sets/*/questions").hasRole("ADMIN")  // 设置题集内容
                        .requestMatchers(HttpMethod.DELETE, "/api/question-sets/*/questions/*").hasRole("ADMIN") // 从题集移除题目

                        // 聊天和面试相关 - 需要认证
                        .requestMatchers("/api/chat/**").authenticated()


                        // 模板相关 - 只读操作允许认证用户访问
                        .requestMatchers(HttpMethod.GET,
                                "/api/templates",                 // 查询所有模板
                                "/api/templates/*",               // 根据ID查询模板
                                "/api/templates/*/parsed",        // 获取解析后的模板
                                "/api/templates/name/*",          // 根据名称查询模板
                                "/api/templates/search",          // 搜索模板
                                "/api/templates/page",            // 分页查询
                                "/api/templates/count",           // 获取模板总数
                                "/api/templates/count/search",    // 根据关键词统计
                                "/api/templates/exists",          // 检查模板是否存在
                                "/api/templates/latest"           // 获取最新模板
                        ).authenticated()

                        // 模板相关 - 写操作只允许管理员
                        .requestMatchers(HttpMethod.POST, "/api/templates").hasRole("ADMIN")            // 创建模板
                        .requestMatchers(HttpMethod.PUT, "/api/templates/*").hasRole("ADMIN")           // 更新模板
                        .requestMatchers(HttpMethod.DELETE, "/api/templates/*").hasRole("ADMIN")        // 删除模板

                        // 用户管理 - 管理员专用端点
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