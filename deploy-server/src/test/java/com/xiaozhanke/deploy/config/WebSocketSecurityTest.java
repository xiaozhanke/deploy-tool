package com.xiaozhanke.deploy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 验证 {@code /websocket} 端点的认证策略——HTTP 升级阶段放行，鉴权下沉到 STOMP CONNECT 帧。
 *
 * <p>浏览器原生 WebSocket API 不允许在握手（HTTP Upgrade GET）请求里自定义 {@code Authorization} header,
 * 这是 RFC 6455 / WHATWG WebSocket Living Standard 的硬限制；Bearer Token 只能通过 STOMP 应用层 CONNECT 帧
 * 携带。因此 {@code /websocket/**} 必须在 HTTP 层 permitAll，由
 * {@link WebSocketConfig#configureClientInboundChannel} 注册的 {@link org.springframework.messaging.support.ChannelInterceptor}
 * 在 CONNECT 帧拒掉未携带/无效 token 的连接——101 升级仅相当于建立了 TCP 通道，没有 STOMP CONNECT 就无法
 * 订阅 / 发布任何业务目的地。
 *
 * <p>用 H2 内存库避免污染开发态 ./database/deploy；JWT keystore 路径指到 target/，由 mvn clean 兜底清理。
 *
 * @author xiaozhanke
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/ws_sec_test?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
        "spring.datasource.username=${MYSQL_USER:root}",
        "spring.datasource.password=${MYSQL_PASSWORD:123456}",
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
    void webSocketHandshakeIsNotBlockedByResourceServerFilter() throws Exception {
        // 未携带 Bearer 也不能在 HTTP 层被 401 拦下——否则浏览器 WebSocket 客户端永远走不到 STOMP CONNECT 阶段。
        // 真实未认证攻击者拿到的只是一条不能订阅或发布任何目的地的 TCP 通道，ChannelInterceptor 会立刻拒掉
        // 他后续发的 CONNECT 帧。
        MvcResult result = mockMvc().perform(get("/websocket")).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("/websocket 升级阶段不应被 SecurityFilterChain 401，鉴权在 STOMP 层进行")
                .isNotEqualTo(401);
    }
}
