package com.xiaozhanke.deploy.config;

import com.xiaozhanke.deploy.security.token.JwtToSecurityUserConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.function.Function;

/**
 * Spring Security 配置类
 *
 * @author xiaozhanke
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    public SecurityConfig(AuthenticationEntryPoint authenticationEntryPoint,
                          AccessDeniedHandler accessDeniedHandler,
                          CorsProperties corsProperties) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsProperties = corsProperties;
    }

    /**
     * 配置 OAuth2 授权服务器的安全过滤链
     *
     * @param http 安全配置
     * @return 安全过滤链
     * @throws Exception 异常处理
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // 自定义 OIDC 用户信息
        Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = (context) -> {
            OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();

            return new OidcUserInfo(principal.getToken().getClaims());
        };

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                // 启用 OpenID Connect 1.0
                                .oidc(oidc -> oidc
                                        .userInfoEndpoint(userInfo -> userInfo
                                                .userInfoMapper(userInfoMapper)
                                        )
                                )
                )
                .authorizeHttpRequests(authorize -> authorize
                        // 所有请求都需要认证
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        // 禁用 X-Frame-Options，改用 CSP
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        // 配置内容安全策略 (CSP)
                        .contentSecurityPolicy(csp -> csp
                                // 允许来自 'self' 的页面嵌入
                                .policyDirectives("frame-ancestors 'self'")
                        )
                )
                // 开启 CORS 支持
                .cors(Customizer.withDefaults())
                // 关闭 CSRF 防护
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher())
                )
                // 使用基于 HttpSession 的有状态会话管理策略
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    /**
     * 默认的安全过滤链
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 匹配规则：只处理认证API和UI资源
                .securityMatcher("/ui/**", "/.well-known/**", "/api/auth/login", "/api/auth/logout")
                .authorizeHttpRequests(authorize -> authorize
                        // 允许公共资源访问
                        .requestMatchers("/ui/**", "/.well-known/**", "/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        // 禁用 X-Frame-Options，改用 CSP
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        // 配置内容安全策略 (CSP)
                        .contentSecurityPolicy(csp -> csp
                                // 允许来自 'self' 的页面嵌入
                                .policyDirectives("frame-ancestors 'self'")
                        )
                )
                // 开启 CSRF 防护
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/auth/login")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // 登出
                .logout(logout -> logout
                        // 登出端点 URI
                        .logoutUrl("/api/auth/logout").permitAll()
                        // 显示配置清理 JSESSIONID 和 XSRF-TOKEN 的 Cookie
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        // 使用 Clear-Site-Data 清理 Cookie
                        .addLogoutHandler(new HeaderWriterLogoutHandler(
                                new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.COOKIES)))
                        // 登出成功返回 HTTP 状态码
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                )
                // 使用基于 HttpSession 的有状态会话管理策略
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                .sessionManagement(session -> session
                        // 限制最大并发会话数为 1
                        .maximumSessions(1)
                )
                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    /**
     * 配置 OAuth2 资源服务器的安全过滤链
     */
    @Bean
    @Order(1)
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/actuator/**", "/websocket/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        // 仅管理员可访问 actuator
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // WebSocket 端点收紧到需认证。STOMP CONNECT 时再走 ChannelInterceptor 二次校验 Bearer Token。
                        .requestMatchers("/websocket/**").authenticated()
                        .anyRequest().authenticated())
                // 禁用 Http Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用表单登出
                .logout(AbstractHttpConfigurer::disable)
                // 关闭 CSRF 防护
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用匿名身份访问
                .anonymous(AbstractHttpConfigurer::disable)
                // 会话管理, 创建策略设为无状态
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 资源服务器
                .oauth2ResourceServer(resourceServer -> resourceServer
                        // 配置 JWT
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new JwtToSecurityUserConverter())
                        )
                        // 异常处理
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    /**
     * 暴露 AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * 配置 CORS 规则。
     *
     * <p>允许的 Origin 列表完全由 {@code app.security.cors.allowed-origins} 驱动，
     * 不再绑死开发 profile —— 任意环境（dev/staging/pro）都通过同一配置 key 决定放行哪些前端域名。
     * pro 默认空集合，即生产侧若同域部署可保持默认；如需放行外部前端，请在对应 profile 显式列出。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的源（空列表代表禁止跨域）
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        // 允许的方法
        configuration.setAllowedMethods(Collections.singletonList("*"));
        // 允许的头部
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        // 允许凭据
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用此配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 使 Spring Security 能够感知到 HttpSession 的创建和销毁事件。
     * 这是实现并发会话控制所必需的。
     *
     * @return HttpSessionEventPublisher 实例
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

}
