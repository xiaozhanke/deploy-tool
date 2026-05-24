package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.RoleParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.PlatformRoleVo;
import com.xiaozhanke.deploy.service.PlatformRoleService;
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
 * 角色信息接口
 *
 * @author xiaozhanke
 */
@Tag(name = "roles", description = "角色信息接口")
@RestController
@RequestMapping("/roles")
public class PlatformRoleController {

    private final PlatformRoleService platformRoleService;

    public PlatformRoleController(PlatformRoleService platformRoleService) {
        this.platformRoleService = platformRoleService;
    }

    /**
     * 创建角色
     *
     * @param params 创建角色参数
     * @return 保存后的角色信息
     */
    @Operation(summary = "创建角色", description = "创建角色")
    @PostMapping
    public ResponseEntity<PlatformRoleVo> addRole(@Validated @RequestBody RoleParams params) {
        PlatformRoleVo createdRecord = platformRoleService.createRole(params);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdRecord.getId()).toUri();
        return ResponseEntity.created(location).body(createdRecord);
    }

    /**
     * 分页查询角色列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Operation(summary = "查询角色列表", description = "分页查询角色列表")
    @GetMapping
    public PageResult<PlatformRoleVo> queryPage(@Validated PlatformRoleVo params,
                                                @Parameter(description = "分页参数", example = "{\"page\": 0, \"size\": 20, \"sort\": \"updateTime,desc\"}")
                                                @PageableDefault(size = 20, sort = "updateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return platformRoleService.queryPage(params, pageable);
    }

    /**
     * 获取角色
     *
     * @param id 角色 Id
     * @return 角色信息
     */
    @Operation(summary = "获取角色", description = "根据 Id 获取角色")
    @GetMapping("/{id}")
    public PlatformRoleVo getRole(@Parameter(description = "角色 Id", required = true) @PathVariable String id) {
        return platformRoleService.queryRole(id);
    }

    /**
     * 更新角色
     *
     * @param id     角色 Id
     * @param params 角色参数
     * @return 更新后的角色信息
     */
    @Operation(summary = "更新角色", description = "更新角色")
    @PutMapping("/{id}")
    public PlatformRoleVo updateRole(@Parameter(description = "角色 Id", required = true) @PathVariable String id,
                                     @Validated @RequestBody RoleParams params) {
        return platformRoleService.updateRole(id, params);
    }

    /**
     * 删除角色
     *
     * @param id 角色 Id
     */
    @Operation(summary = "删除角色", description = "删除角色")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@Parameter(description = "角色 Id", required = true) @PathVariable String id) {
        platformRoleService.deleteRole(id);
    }
}
