package com.xiaozhanke.deploy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 验证 {@code /websocket/**} 端点已收紧到需认证。
 *
 * <p>之前 SecurityConfig 把 WebSocket 端点配为 {@code permitAll()}，仅靠 STOMP CONNECT 时的
 * ChannelInterceptor 校验 Bearer Token，这意味着握手响应（升级到 ws 协议）对未认证客户端是 101 而非 401，
 * 也对 anonymous 用户暴露端点存在性。这里通过直接 GET {@code /websocket} 验证 HTTP 升级阶段就要求认证。
 *
 * <p>用 H2 内存库避免污染开发态 ./database/deploy；JWT keystore 路径指到 target/，由 mvn clean 兜底清理。
 *
 * @author xiaozhanke
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:ws-sec-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=never",
        "app.security.jwt.key-file=target/test-jwt-ws-key.json",
        "app.security.jwt.auto-create-on-missing=true"
})
class WebSocketSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
    void unauthenticatedWebSocketHandshakeIsRejected() throws Exception {
        MvcResult result = mockMvc().perform(get("/websocket")).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("未携带 Bearer Token 的 /websocket 必须被资源服务器拒绝")
                .isEqualTo(401);
    }

    @Test
    void authenticatedRequestPassesResourceServerFilter() throws Exception {
        Jwt jwt = Jwt.withTokenValue("stub-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("test-user")
                .claim("scope", "openid")
                .claims(claims -> claims.putAll(Map.of("authorities", List.of("ROLE_USER"))))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        MvcResult result = mockMvc().perform(get("/websocket")
                .header(HttpHeaders.AUTHORIZATION, "Bearer stub-token"))
                .andReturn();
        // 不是真正的 WebSocket Upgrade 请求，握手处理器会拒掉（400/404 之类），但绝对不应再是 401，否则说明 Bearer 校验失败
        assertThat(result.getResponse().getStatus())
                .as("携带合法 JWT 的 /websocket 应通过资源服务器过滤链")
                .isNotEqualTo(401);
    }
}
