package com.xiaozhanke.deploy.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 验证 JWT 签名密钥跨进程重启的持久化与稳定性。
 *
 * <p>之前的实现每次启动都会随机生成 RSA 密钥，重启即让所有已签发 JWT 失效。
 * 本测试用 ApplicationContextRunner 模拟两次启动，覆盖：自动创建、kid 稳定、缺失即失败三条路径。
 *
 * @author xiaozhanke
 */
class JwtKeyPersistenceTest {

    @Test
    void autoCreateKeyFileWhenMissing(@TempDir Path tempDir) {
        Path keyFile = tempDir.resolve("jwt-key.json");
        runner(keyFile, "auto-create", true).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(Files.exists(keyFile)).isTrue();
            assertThat(currentKid(context.getBean(JWKSource.class))).isEqualTo("auto-create");
        });
    }

    @Test
    void keyIdRemainsStableAcrossRestarts(@TempDir Path tempDir) {
        Path keyFile = tempDir.resolve("jwt-key.json");
        ApplicationContextRunner runner = runner(keyFile, "restart-stable", true);

        StringBuilder kidFromFirst = new StringBuilder();
        runner.run(context -> kidFromFirst.append(currentKid(context.getBean(JWKSource.class))));

        runner.run(context -> {
            String kidFromSecond = currentKid(context.getBean(JWKSource.class));
            assertThat(kidFromSecond).isEqualTo(kidFromFirst.toString());
        });
    }

    @Test
    void failFastWhenFileMissingAndAutoCreateDisabled(@TempDir Path tempDir) {
        Path keyFile = tempDir.resolve("absent.json");
        runner(keyFile, "fail-fast", false).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT 密钥文件不存在");
        });
    }

    private static ApplicationContextRunner runner(Path keyFile, String keyId, boolean autoCreate) {
        return new ApplicationContextRunner()
                .withUserConfiguration(JwtConfig.class)
                .withPropertyValues(
                        "app.security.jwt.key-file=" + keyFile.toAbsolutePath(),
                        "app.security.jwt.key-id=" + keyId,
                        "app.security.jwt.auto-create-on-missing=" + autoCreate);
    }

    @SuppressWarnings("unchecked")
    private static String currentKid(Object source) throws Exception {
        JWKSource<SecurityContext> typed = (JWKSource<SecurityContext>) source;
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
        List<JWK> jwks = typed.get(selector, null);
        assertThat(jwks).isNotEmpty();
        return jwks.get(0).getKeyID();
    }
}
