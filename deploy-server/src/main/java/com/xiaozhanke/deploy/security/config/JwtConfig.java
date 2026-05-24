package com.xiaozhanke.deploy.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.xiaozhanke.deploy.security.user.SecurityUser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(JwtKeyStoreProperties.class)
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtKeyStoreProperties properties;

    /**
     * 生成 JWKSource，提供 RSA 密钥用于 JWT 令牌的签名和验证
     *
     * @return JWKSource 实例
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = loadOrCreateRsaKey();
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
     * 优先从配置文件路径加载已持久化的 RSA 密钥；不存在时根据 autoCreateOnMissing 选项决定是否新建并落盘
     */
    private RSAKey loadOrCreateRsaKey() {
        Path keyPath = Paths.get(properties.keyFile()).toAbsolutePath();

        if (Files.exists(keyPath)) {
            try {
                String json = Files.readString(keyPath);
                RSAKey loaded = RSAKey.parse(json);
                if (loaded.toPrivateKey() == null) {
                    throw new IllegalStateException("JWK 文件缺少私钥: " + keyPath);
                }
                log.info("已加载 JWT 签名密钥: {} (kid={})", keyPath, loaded.getKeyID());
                return loaded;
            } catch (Exception e) {
                throw new IllegalStateException("解析 JWT 密钥文件失败: " + keyPath, e);
            }
        }

        if (!properties.autoCreateOnMissing()) {
            throw new IllegalStateException(
                    "JWT 密钥文件不存在: " + keyPath
                            + "（生产环境请预先生成；如允许自动生成请将 app.security.jwt.auto-create-on-missing 置为 true）");
        }

        RSAKey generated = generateNewRsaKey();
        try {
            Path parent = keyPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(keyPath, generated.toJSONString(),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            log.warn("JWT 密钥文件不存在，已自动生成并保存: {} (kid={})", keyPath, generated.getKeyID());
        } catch (IOException e) {
            throw new IllegalStateException("写入 JWT 密钥文件失败: " + keyPath, e);
        }
        return generated;
    }

    /**
     * 生成新的 2048 位 RSA 密钥对，并使用配置 keyId 标识
     */
    private RSAKey generateNewRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(properties.keyId())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("RSA 密钥生成失败", e);
        }
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
