package com.xiaozhanke.deploy.security.config;

import com.xiaozhanke.deploy.security.user.SecurityUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * JWT 配置类
 *
 * @author xiaozhanke
 */
@Slf4j
@Configuration
public class JwtConfig {

    /**
     * 生成 JWKSource，提供 RSA 密钥用于 JWT 令牌的签名和验证
     *
     * @return JWKSource 实例
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 配置 JWT 解码器，基于 JWKSource 实现
     *
     * @param jwkSource JWKSource 实例
     * @return JwtDecoder 组件
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * 生成 RSA 密钥对，用于 JWT 令牌签名与验证
     *
     * @return 生成的密钥对
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            // 设置密钥长度为 2048 位
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("RSA 密钥生成失败", e);
        }
        return keyPair;
    }

    /**
     * 配置 OAuth2 令牌自定义逻辑
     * 该方法用于在 JWT 令牌中加入额外的 claims
     *
     * @return OAuth2TokenCustomizer 组件
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            Authentication principal = context.getPrincipal();
            if (principal.getPrincipal() instanceof SecurityUser securityUser) {
                // ID Token 处理，将 OIDC 用户信息存入 claims
                if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                    OidcUserInfo oidcUserInfo = OidcUserInfo.builder()
                            // 设置用户唯一标识
                            .subject(securityUser.getUsername())
                            // 设置用户名
                            .name(securityUser.getDisplayName())
                            // 设置邮箱
                            .email(securityUser.getEmail())
                            // 标记邮箱已验证
                            .emailVerified(true)
                            // 设置手机号
                            .phoneNumber(securityUser.getPhone())
                            // 标记手机号已验证
                            .phoneNumberVerified(true)
                            .build();
                    // 将 OIDC 用户信息 claims 添加到 ID Token
                    context.getClaims().claims(claims -> claims.putAll(oidcUserInfo.getClaims()));
                }
                // Access Token 处理，存入用户角色信息
                if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                    context.getClaims().claims((claims) -> {
                        // 解析用户权限，并去除 "ROLE_" 前缀
                        Set<String> roles = AuthorityUtils.authorityListToSet(securityUser.getAuthorities())
                                .stream()
                                .map(c -> c.replaceFirst("^ROLE_", ""))
                                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                        // 存入 roles 字段
                        claims.put("roles", roles);
                    });
                }
            }
        };
    }
}
