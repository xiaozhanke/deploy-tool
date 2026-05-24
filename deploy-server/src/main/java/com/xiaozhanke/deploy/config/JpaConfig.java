package com.xiaozhanke.deploy.config;

import com.xiaozhanke.deploy.util.AuthenticationHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA 持久化层相关配置
 * <p>
 * 这个配置类做了以下几件事：
 * 1. @EnableTransactionManagement: 启用了 Spring 的声明式事务管理。
 * 2. @EnableJpaAuditing: 开启了 Spring Data JPA 的审计功能，用于自动填充创建/修改信息。
 * 3. 定义了 AuditorAware 的 Bean，使其能够从 Spring Security 上下文中获取当前用户名。
 * </p>
 *
 * @author xiaozhanke
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    /**
     * 定义 JPA 审计功能的用户信息提供者 Bean
     *
     * @param authenticationHelper 自动注入的、用于获取当前认证信息的帮助类
     * @return AuditorAware<String> 的一个实例
     */
    @Bean
    public AuditorAware<String> auditorAware(AuthenticationHelper authenticationHelper) {
        return () -> authenticationHelper.getCurrentUserName()
                // 如果没有登录用户，提供一个默认的系统用户标识
                .or(() -> Optional.of("system"));
    }
}
