package com.xiaozhanke.deploy.model.request;

import com.xiaozhanke.deploy.enums.ApplicationTypeEnum;
import com.xiaozhanke.deploy.enums.DeploymentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部署参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "部署参数")
public class DeploymentParams {
    /**
     * 服务器 Id
     */
    @Schema(description = "服务器 Id")
    @NotBlank(message = "服务器 Id 不能为空")
    private String serverRecordId;

    /**
     * 文件记录 Id
     */
    @Schema(description = "文件记录 Id")
    @NotBlank(message = "文件记录 Id 不能为空")
    private String fileRecordId;

    /**
     * 应用类型
     */
    @Schema(description = "应用类型")
    @NotNull(message = "应用类型不能为空")
    private ApplicationTypeEnum applicationType;

    /**
     * 部署路径
     */
    @Schema(description = "部署路径")
    @NotBlank(message = "部署路径不能为空")
    private String deploymentPath;

    /**
     * 配置文件夹路径
     */
    @Schema(description = "配置文件夹路径")
    private String deploymentConfigPath;

    /**
     * 部署端口
     */
    @Schema(description = "部署端口", minimum = "0", maximum = "65535")
    @Min(value = 0, message = "端口号不能小于0")
    @Max(value = 65535, message = "端口号不能大于65535")
    private Integer port;

    /**
     * 程序参数
     */
    @Schema(description = "程序参数")
    private String programArgs;

    /**
     * 激活的配置文件
     */
    @Schema(description = "激活的配置文件")
    private String activeProfiles;

    /**
     * 部署状态
     */
    @Schema(description = "部署状态")
    @NotNull(message = "部署状态不能为空")
    private DeploymentStatusEnum status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 最后启动时间
     */
    @Schema(description = "最后启动时间")
    private LocalDateTime lastStartTime;

    /**
     * 最后停止时间
     */
    @Schema(description = "最后停止时间")
    private LocalDateTime lastStopTime;

    /**
     * 进程 Id
     */
    @Schema(description = "进程 Id")
    private String processId;

    /**
     * 是否正在运行
     */
    @Schema(description = "是否正在运行")
    private Boolean running;
}