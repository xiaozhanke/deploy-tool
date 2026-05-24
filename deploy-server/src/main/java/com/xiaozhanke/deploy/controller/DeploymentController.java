package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.DeploymentParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.DeploymentRecordVo;
import com.xiaozhanke.deploy.service.DeploymentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 部署接口
 *
 * @author xiaozhanke
 */
@Tag(name = "deployments", description = "部署接口")
@RestController
@RequestMapping("/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    /**
     * 创建部署记录
     *
     * @param params 创建部署记录参数
     * @return 保存后的部署记录信息
     */
    @Operation(summary = "创建部署记录", description = "创建部署记录")
    @PostMapping
    public ResponseEntity<DeploymentRecordVo> addDeployment(@Validated @RequestBody DeploymentParams params) {
        DeploymentRecordVo createdRecord = deploymentService.createDeployment(params);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdRecord.getId()).toUri();
        return ResponseEntity.created(location).body(createdRecord);
    }

    /**
     * 查询所有部署记录列表
     *
     * @param params 查询参数
     * @param sort   排序参数
     * @return 部署记录列表
     */
    @Operation(summary = "查询部署记录列表", description = "查询所有部署记录列表")
    @GetMapping("/list")
    public List<DeploymentRecordVo> queryList(DeploymentParams params,
                                              @Parameter(description = "排序参数", example = "{\"sort\": \"updateTime,desc\"}") Sort sort) {
        return deploymentService.queryList(params, sort);
    }

    /**
     * 分页查询部署记录列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询部署记录列表", description = "分页查询部署记录列表")
    @GetMapping("/page")
    public PageResult<DeploymentRecordVo> queryPage(DeploymentParams params,
                                                    @Parameter(description = "分页参数", example = "{\"page\": 0, \"size\": 20, \"sort\": \"updateTime,desc\"}")
                                                    @PageableDefault(size = 20, sort = "updateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return deploymentService.queryPage(params, pageable);
    }

    /**
     * 获取部署记录
     *
     * @param id 部署记录 Id
     * @return 部署记录信息
     */
    @Operation(summary = "获取部署记录", description = "根据 Id 获取部署记录")
    @GetMapping("/{id}")
    public DeploymentRecordVo getDeployment(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        return deploymentService.queryDeployment(id);
    }

    /**
     * 更新部署记录
     *
     * @param id     部署记录 Id
     * @param params 部署参数
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "更新部署记录", description = "更新部署记录")
    @PutMapping("/{id}")
    public DeploymentRecordVo updateDeployment(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id,
                                               @Validated @RequestBody DeploymentParams params) {
        return deploymentService.updateDeployment(id, params);
    }

    /**
     * 删除部署记录
     *
     * @param id 部署记录 Id
     */
    @Operation(summary = "删除部署记录", description = "删除部署记录")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeployment(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        deploymentService.deleteDeployment(id);
    }

    /**
     * 启动应用
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "启动应用", description = "启动后端应用")
    @PostMapping("/{id}/actions/start")
    public DeploymentRecordVo startApplication(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        return deploymentService.startApplication(id);
    }

    /**
     * 停止应用
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "停止应用", description = "停止后端应用")
    @PostMapping("/{id}/actions/stop")
    public DeploymentRecordVo stopApplication(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        return deploymentService.stopApplication(id);
    }

    /**
     * 重启应用
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "重启应用", description = "重启后端应用")
    @PostMapping("/{id}/actions/restart")
    public DeploymentRecordVo restartApplication(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        return deploymentService.restartApplication(id);
    }

    /**
     * 获取应用状态
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "获取应用状态", description = "获取后端应用运行状态")
    @GetMapping("/{id}/status")
    public DeploymentRecordVo getApplicationStatus(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id) {
        return deploymentService.getApplicationStatus(id);
    }

    /**
     * 更新应用包
     *
     * @param id           部署记录 Id
     * @param fileRecordId 文件记录 Id
     * @return 更新后的部署记录信息
     */
    @Operation(summary = "更新应用", description = "更新应用包")
    @PutMapping("/{id}/package")
    public DeploymentRecordVo updateApplication(@Parameter(description = "部署记录 Id", required = true) @PathVariable String id,
                                                @Parameter(description = "文件记录 Id", required = true) @RequestParam String fileRecordId) {
        return deploymentService.updateApplication(id, fileRecordId);
    }
}