package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.LoginRequest;
import com.xiaozhanke.deploy.model.vo.PlatformUserVo;
import com.xiaozhanke.deploy.service.PlatformUserService;
import com.xiaozhanke.deploy.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * @author xiaozhanke
 */
@Tag(name = "auth", description = "认证接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PlatformUserService platformUserService;
    private final AuthenticationHelper authenticationHelper;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(PlatformUserService platformUserService, AuthenticationHelper authenticationHelper,
                          AuthenticationManager authenticationManager) {
        this.platformUserService = platformUserService;
        this.authenticationHelper = authenticationHelper;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     */
    @Operation(summary = "用户登录", description = "通过用户名和密码进行认证，成功后建立会话")
    @PostMapping("/login")
    public void login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        // 使用提供的凭据创建未经身份认证的 UsernamePasswordAuthenticationToken
        UsernamePasswordAuthenticationToken authenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.getUsername(), loginRequest.getPassword());
        // 调用 AuthenticationManager 来认证用户身份
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        // 创建空的安全上下文
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        // 保存认证结果
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "结束当前用户的会话")
    @PostMapping("/logout")
    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // 清理安全上下文，使会话无效
        securityContextLogoutHandler.logout(request, response, authentication);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 当前登录用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    @GetMapping("/me")
    public PlatformUserVo currentUser() {
        String currentUserName = authenticationHelper.getCurrentUserName()
                .orElseThrow(() -> new IllegalStateException("用户未认证"));
        return platformUserService.queryUserByUsername(currentUserName);
    }

}
