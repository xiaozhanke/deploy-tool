package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.UserParams;
import com.xiaozhanke.deploy.model.request.UserPasswordParams;
import com.xiaozhanke.deploy.model.request.UserProfileParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.PlatformUserVo;
import com.xiaozhanke.deploy.service.PlatformUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * 用户信息接口
 *
 * @author xiaozhanke
 */
@Tag(name = "users", description = "用户信息接口")
@RestController
@RequestMapping("/users")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    public PlatformUserController(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }

    /**
     * 创建用户
     *
     * @param params 创建用户参数
     * @return 保存后的用户信息
     */
    @Operation(summary = "创建用户", description = "创建用户")
    @PostMapping
    public ResponseEntity<PlatformUserVo> addUser(@Validated @RequestBody UserParams params) {
        PlatformUserVo createdRecord = platformUserService.createUser(params);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdRecord.getId()).toUri();
        return ResponseEntity.created(location).body(createdRecord);
    }

    /**
     * 分页查询用户列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表")
    @GetMapping
    public PageResult<PlatformUserVo> queryPage(@Validated PlatformUserVo params,
                                                @Parameter(description = "分页参数", example = "{\"page\": 0, \"size\": 20, \"sort\": \"updateTime,desc\"}")
                                                @PageableDefault(size = 20, sort = "updateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return platformUserService.queryPage(params, pageable);
    }

    /**
     * 获取用户
     *
     * @param id 用户 Id
     * @return 用户信息
     */
    @Operation(summary = "获取用户", description = "根据 Id 获取用户")
    @GetMapping("/{id}")
    public PlatformUserVo getUser(@Parameter(description = "用户 Id", required = true) @PathVariable String id) {
        return platformUserService.queryUser(id);
    }

    /**
     * 更新用户
     *
     * @param id     用户 Id
     * @param params 用户参数
     * @return 更新后的用户信息
     */
    @Operation(summary = "更新用户", description = "更新用户")
    @PutMapping("/{id}")
    public PlatformUserVo updateUser(@Parameter(description = "用户 Id", required = true) @PathVariable String id,
                                     @Validated @RequestBody UserParams params) {
        return platformUserService.updateUser(id, params);
    }

    /**
     * 删除用户
     *
     * @param id 用户 Id
     */
    @Operation(summary = "删除用户", description = "删除用户")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@Parameter(description = "用户 Id", required = true) @PathVariable String id) {
        platformUserService.deleteUser(id);
    }

    /**
     * 修改当前用户基本信息
     *
     * @param params 用户信息参数
     * @return 保存后的用户信息
     */
    @Operation(summary = "修改当前用户信息", description = "修改当前用户基本信息")
    @PutMapping("/me/profiles")
    public PlatformUserVo updateUserProfile(@Validated @RequestBody UserProfileParams params) {
        return platformUserService.updateCurrentUserProfile(params);
    }

    /**
     * 修改当前用户登录密码
     *
     * @param params 修改密码参数
     * @return 保存后的用户信息
     */
    @Operation(summary = "修改当前用户密码", description = "修改当前用户登录密码")
    @PutMapping("/me/password")
    public PlatformUserVo updateUserPassword(@Validated @RequestBody UserPasswordParams params) {
        return platformUserService.updateCurrentUserPassword(params);
    }
}
