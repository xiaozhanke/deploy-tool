package com.xiaozhanke.deploy.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * WebMVC 配置
 *
 * @author xiaozhanke
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源映射
     * 适用于前后端分离项目，确保 Vue Router 的 history 模式可以正确访问页面
     *
     * @param registry 资源处理注册对象
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理所有静态资源请求, 包括 /ui/** 和其他路径
        registry.addResourceHandler("/**", "/ui/**")
                // 定义静态文件的访问路径
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        // 处理 Vue Router history 模式与 Spring Boot 的静态资源处理机制未正确匹配问题
                        // 如果资源不存在，则返回 index.html，让 Vue 前端路由接管
                        return requestedResource.exists() && requestedResource.isReadable() ? requestedResource : new ClassPathResource("/static/index.html");
                    }
                });
    }

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