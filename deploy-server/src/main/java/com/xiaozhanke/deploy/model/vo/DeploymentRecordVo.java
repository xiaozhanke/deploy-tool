package com.xiaozhanke.deploy.model.vo;

import com.xiaozhanke.deploy.enums.ApplicationTypeEnum;
import com.xiaozhanke.deploy.enums.DeploymentStatusEnum;
import com.xiaozhanke.deploy.model.base.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 部署记录 VO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部署记录信息")
public class DeploymentRecordVo extends BaseVo {
    /**
     * 部署 Id
     */
    @Schema(description = "部署 Id")
    private String id;

    /**
     * 服务器记录
     */
    @Schema(description = "服务器记录")
    private ServerRecordVo serverRecord;

    /**
     * 文件记录
     */
    @Schema(description = "文件记录")
    private FileRecordVo fileRecord;

    /**
     * 应用类型
     */
    @Schema(description = "应用类型")
    private ApplicationTypeEnum applicationType;

    /**
     * 部署路径
     */
    @Schema(description = "部署路径")
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
    private DeploymentStatusEnum status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 部署时间
     */
    @Schema(description = "部署时间")
    private LocalDateTime deployTime;

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