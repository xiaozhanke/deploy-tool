package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH Exec 消息体
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "SSH Exec 消息体")
public class SshExecMessage {
    /**
     * 命令
     */
    @Schema(description = "命令")
    @NotBlank(message = "命令不能为空")
    private String command;
}
