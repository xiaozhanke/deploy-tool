package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH Shell 消息体
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "SSH Shell 消息体")
public class SshShellMessage {
    /**
     * 任务 Id
     */
    @Schema(description = "任务 Id")
    @NotBlank(message = "任务 Id 不能为空")
    private String taskId;

    /**
     * 命令
     */
    @Schema(description = "命令")
    @NotBlank(message = "命令不能为空")
    private String command;
}
