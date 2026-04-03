package com.peatroxd.mtprototest.common.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({RateLimitProperties.class, AdminAccessProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAccessInterceptor adminAccessInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(AdminAccessInterceptor adminAccessInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.adminAccessInterceptor = adminAccessInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAccessInterceptor);
        registry.addInterceptor(rateLimitInterceptor);
    }
}
