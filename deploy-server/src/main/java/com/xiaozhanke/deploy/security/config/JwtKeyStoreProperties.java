package com.xiaozhanke.deploy.security.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 签名密钥持久化配置
 *
 * <p>之前的实现是每次启动随机生成 RSA 密钥，会导致重启后旧 JWT 立即失效，
 * 已登录用户被强制登出。本配置允许把 RSA 密钥序列化为 JWK JSON 文件后落盘，
 * 跨进程重启保持 keyID 与签名验证一致。
 *
 * @param keyFile              JWK JSON 文件路径（相对启动工作目录或绝对路径）
 * @param keyId                序列化进 JWK 的 kid 字段；首次生成时使用，加载时优先使用文件内已有 kid
 * @param autoCreateOnMissing  文件不存在时是否自动生成新密钥并写入；dev 环境默认 true，pro 默认 false 以避免悄悄丢失审计能力
 * @author xiaozhanke
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtKeyStoreProperties(
        @NotBlank String keyFile,
        @NotBlank String keyId,
        boolean autoCreateOnMissing) {
}
