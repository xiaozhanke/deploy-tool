package com.xiaozhanke.deploy.config;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 *
 * @author xiaozhanke
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$", Pattern.CASE_INSENSITIVE);

    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
    }

    /**
     * 配置消息代理，为客户端提供消息路由能力
     *
     * @param registry 消息代理注册对象，定义消息的路由规则，如主题 (topic) 和队列 (queue)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的内存消息代理
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置应用前缀，客户端发送消息需要以 /app 开头
        registry.setApplicationDestinationPrefixes("/app");
        // 设置用户目标前缀（用于点对点消息）
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点，使客户端可以通过 WebSocket 连接
     *
     * @param registry STOMP 端点注册对象，定义 WebSocket 连接的入口地址和跨域设置
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                // 允许所有跨域访问
                .setAllowedOrigins("*");
    }

    /**
     * 添加参数解析器
     *
     * @param argumentResolvers 参数解析器列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 使得在 @MessageMapping 注解的方法中可以通过 @AuthenticationPrincipal 获取当前认证用户。
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    /**
     * 配置客户端入站通道
     *
     * @param registration 通道注册对象
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 从 STOMP header 中提取认证信息，并将其设置到 SecurityContextHolder，使得后续的消息处理可以获取到当前用户。
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Access authentication header(s) and invoke accessor.setUser(user)
                    String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
                    if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                        throw new InvalidBearerTokenException("Missing or invalid Authorization header");
                    }
                    Matcher matcher = AUTHORIZATION_PATTERN.matcher(authorization);
                    if (!matcher.matches()) {
                        throw new InvalidBearerTokenException("Bearer token is malformed");
                    }
                    String token = matcher.group("token");
                    BearerTokenAuthenticationToken authenticationToken = new BearerTokenAuthenticationToken(token);
                    Authentication authentication = jwtAuthenticationProvider.authenticate(authenticationToken);
                    if (authentication == null || !authentication.isAuthenticated()) {
                        throw new InvalidBearerTokenException("Invalid token");
                    }
                    accessor.setUser(authentication);
                }
                return message;
            }
        });
    }

    /**
     * 定义 WebSocket 消息的授权规则。
     */
    @Bean
    public MessageMatcherDelegatingAuthorizationManager.Builder messageAuthorizationManagerBuilder() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                .simpTypeMatchers(SimpMessageType.CONNECT).authenticated()
                .simpTypeMatchers(SimpMessageType.UNSUBSCRIBE, SimpMessageType.DISCONNECT).authenticated()
                .simpSubscribeDestMatchers("/topic/**", "/queue/**").authenticated()
                .simpMessageDestMatchers("/app/**").authenticated()
                .anyMessage().denyAll();
    }
}
