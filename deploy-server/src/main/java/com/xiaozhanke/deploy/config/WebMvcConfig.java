package com.xiaozhanke.deploy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMVC 配置
 *
 * @author xiaozhanke
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置接口路径匹配
     *
     * @param configurer 路径匹配配置对象
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 自动为所有 @RestController 添加 "/api" 前缀，保持接口一致性
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class)
                // 排除 org.springdoc 这个 OpenAPI 的包
                .and(handlerType -> !handlerType.getPackageName().startsWith("org.springdoc")));
    }
}
