package com.xiaozhanke.deploy.security.token;

import com.xiaozhanke.deploy.security.user.SecurityUser;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 将 JWT 解析为 Spring Security 的认证令牌
 * 该转换器从 JWT 中提取用户信息和角色，并构造 {@link UsernamePasswordAuthenticationToken}
 *
 * @author xiaozhanke
 */
public class JwtToSecurityUserConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {
    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 解析用户名
        String username = jwt.getSubject();
        // 获取角色列表
        List<String> authoritiesList = jwt.getClaimAsStringList("roles");
        // 转换角色列表为 Spring Security 兼容的 GrantedAuthority 集合
        Collection<? extends GrantedAuthority> authorities = authoritiesList == null ? Collections.emptyList() :
                authoritiesList.stream()
                        // 角色名添加 "ROLE_" 前缀
                        .map(role -> "ROLE_" + role)
                        // 转换为 SimpleGrantedAuthority
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        // 构造 SecurityUser
        SecurityUser securityUser = new SecurityUser(username, null, null, null, null, authorities);

        // 返回 Spring Security 认证令牌
        return new UsernamePasswordAuthenticationToken(securityUser, null, authorities);
    }
}
