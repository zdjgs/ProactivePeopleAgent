package com.proactiveperson.app.config;

import com.proactiveperson.common.exception.UnauthorizedException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ApiSecurityConfig implements WebMvcConfigurer {

    private final ApiSecurityProperties properties;

    public ApiSecurityConfig(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!properties.isAuthEnabled()) {
                    return true;
                }
                if (!StringUtils.hasText(properties.getToken())) {
                    throw new UnauthorizedException("已启用 API 鉴权但未配置 pp.api.token / PP_API_TOKEN");
                }
                String provided = request.getHeader("X-API-Key");
                if (!properties.getToken().equals(provided)) {
                    throw new UnauthorizedException();
                }
                return true;
            }
        }).addPathPatterns("/api/chat", "/api/chat/**");
    }
}
