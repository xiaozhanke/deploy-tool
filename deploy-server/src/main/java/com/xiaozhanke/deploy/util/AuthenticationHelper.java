package com.xiaozhanke.deploy.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 认证信息的帮助类
 *
 * @author xiaozhanke
 */
@Component
public class AuthenticationHelper {

    // 获取当前线程上配置的 SecurityContextHolderStrategy
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    /**
     * 获取当前认证经过验证的、有效的 Authentication 对象
     *
     * @return 一个包含有效 Authentication 的 Optional，如果未认证或认证无效则为空 Optional。
     */
    private Optional<Authentication> getAuthentication() {
        // 从当前策略中获取 SecurityContext
        SecurityContext context = securityContextHolderStrategy.getContext();
        return Optional.ofNullable(context.getAuthentication())
                // 使用 filter 链式处理所有验证条件
                .filter(authentication -> authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken)
                );
    }

    /**
     * 获取当前认证用户的核心信息
     *
     * @return 包含 UserDetails 实现的 Optional，如果未认证或 principal 类型不匹配则为空 Optional。
     */
    public Optional<UserDetails> getCurrentUserDetails() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast);
    }

    /**
     * 获取当前用户名
     *
     * @return 包含用户名的 Optional，如果未认证则为空 Optional。
     */
    public Optional<String> getCurrentUserName() {
        return getCurrentUserDetails().map(UserDetails::getUsername);
    }
}
