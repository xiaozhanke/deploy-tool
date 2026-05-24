package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.ServerParams;
import com.xiaozhanke.deploy.model.vo.ServerRecordVo;
import com.xiaozhanke.deploy.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;

/**
 * 服务器信息接口
 *
 * @author xiaozhanke
 */
@Tag(name = "servers", description = "服务器信息接口")
@RestController
@RequestMapping("/servers")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    /**
     * 添加服务器
     *
     * @param params 服务器信息参数
     * @return 保存后的服务器信息
     */
    @Operation(summary = "添加服务器", description = "添加服务器信息")
    @PostMapping
    public ResponseEntity<ServerRecordVo> addServer(@Validated @RequestBody ServerParams params) {
        ServerRecordVo createdRecord = serverService.addServer(params);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdRecord.getId()).toUri();
        return ResponseEntity.created(location).body(createdRecord);
    }

    /**
     * 更新服务器
     *
     * @param id     服务器 Id
     * @param params 服务器信息参数
     * @return 更新后的服务器信息
     */
    @Operation(summary = "更新服务器", description = "更新服务器信息")
    @PutMapping("/{id}")
    public ServerRecordVo updateServer(@Parameter(description = "服务器 Id", required = true) @PathVariable String id, @Validated @RequestBody ServerParams params) {
        return serverService.updateServer(id, params);
    }

    /**
     * 删除服务器
     *
     * @param id 服务器 Id
     */
    @Operation(summary = "删除服务器", description = "删除服务器信息")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServer(@Parameter(description = "服务器 Id", required = true) @PathVariable String id) {
        serverService.deleteServer(id);
    }

    /**
     * 获取服务器
     *
     * @param id 服务器 Id
     * @return 服务器信息
     */
    @Operation(summary = "获取服务器", description = "获取服务器信息")
    @GetMapping("/{id}")
    public ServerRecordVo getServer(@Parameter(description = "服务器 Id", required = true) @PathVariable String id) {
        return serverService.queryServer(id);
    }

    /**
     * 查询服务器所有列表
     *
     * @return 服务器列表
     */
    @Operation(summary = "查询服务器列表", description = "查询服务器所有列表")
    @GetMapping
    public List<ServerRecordVo> queryList() {
        return serverService.queryList();
    }

    /**
     * 测试服务器连接
     *
     * @param params 服务器信息参数
     * @return 连接测试结果
     */
    @Operation(summary = "测试服务器连接", description = "测试服务器连接")
    @PostMapping("/test-connection")
    public boolean testConnection(@Parameter(description = "服务器信息") @Validated @RequestBody ServerParams params) {
        return serverService.testConnection(params);
    }
} 