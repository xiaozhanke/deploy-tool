package com.xiaozhanke.deploy.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 与 WebSocket 跨域允许列表配置。
 *
 * <p>之前 WebSocketConfig 写死 {@code setAllowedOrigins("*")}，配合 {@code allowCredentials=true} 既被现代浏览器拒绝，
 * 又把 WebSocket 暴露给任意页面。本配置把允许的 Origin 列表外置到 application*.yml：dev 默认放行本地 Vite，
 * 生产侧默认空列表（仅同源），新增环境（test、staging）按需追加。
 *
 * @param allowedOrigins 允许的 Origin 列表，空集合表示禁止跨域（同源仍然放行）
 * @author xiaozhanke
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
