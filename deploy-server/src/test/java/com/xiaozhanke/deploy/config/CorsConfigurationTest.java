package com.xiaozhanke.deploy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 验证 {@code CorsConfigurationSource} 现在由 {@code app.security.cors.allowed-origins} 驱动，
 * 不再绑定 dev profile，使 staging/pro 也能通过配置放行特定前端域名。
 *
 * <p>之前 CORS Bean 加了 {@code @Profile("dev")}，pro 启动时整张 CORS 表是缺失的，
 * 跨域前端只能靠反向代理同源化兜底。本测试在非 dev profile 下注入 origin，验证 OPTIONS 预检：
 * 在白名单内的 origin 应被允许；不在的 origin 应被 Spring CORS 拒绝。
 *
 * <p>用 H2 内存库避免污染开发态 ./database/deploy；JWT keystore 路径指到 target/，由 mvn clean 兜底清理。
 *
 * @author xiaozhanke
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:cors-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=never",
        "app.security.jwt.key-file=target/test-jwt-cors-key.json",
        "app.security.jwt.auto-create-on-missing=true",
        "app.security.cors.allowed-origins[0]=https://web.example.com"
})
class CorsConfigurationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private MockMvc mockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                    .apply(springSecurity())
                    .build();
        }
        return mockMvc;
    }

    @Test
    void preflightFromAllowedOriginIsAccepted() throws Exception {
        MvcResult result = mockMvc().perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "https://web.example.com")
                .header("Access-Control-Request-Method", "POST"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("https://web.example.com");
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        MvcResult result = mockMvc().perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST"))
                .andReturn();
        // Spring CORS 对非白名单 origin 直接返回 403，并不会附带 Access-Control-Allow-Origin
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
