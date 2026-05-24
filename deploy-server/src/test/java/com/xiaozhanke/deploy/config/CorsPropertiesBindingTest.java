package com.xiaozhanke.deploy.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 验证 {@link CorsProperties} 通过 application*.yml 注入生效。
 *
 * <p>用 ApplicationContextRunner 避免拉起完整 Spring 上下文，专注于配置绑定本身。
 *
 * @author xiaozhanke
 */
class CorsPropertiesBindingTest {

    @Test
    void bindsAllowedOriginsFromProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(EnableProps.class)
                .withPropertyValues(
                        "app.security.cors.allowed-origins[0]=https://localhost:5173",
                        "app.security.cors.allowed-origins[1]=https://example.com"
                )
                .run(context -> {
                    CorsProperties props = context.getBean(CorsProperties.class);
                    assertThat(props.allowedOrigins())
                            .containsExactly("https://localhost:5173", "https://example.com");
                });
    }

    @Test
    void defaultsToEmptyListWhenUnset() {
        new ApplicationContextRunner()
                .withUserConfiguration(EnableProps.class)
                .run(context -> {
                    CorsProperties props = context.getBean(CorsProperties.class);
                    assertThat(props.allowedOrigins()).isEmpty();
                });
    }

    @EnableConfigurationProperties(CorsProperties.class)
    static class EnableProps {
    }
}
