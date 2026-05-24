package com.xiaozhanke.deploy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JSch Exec 通道命令执行结果
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "Exec 通道命令执行结果")
public class SshExecResult {
    /**
     * 命令执行状态码
     */
    @Schema(description = "命令执行状态码", example = "0")
    private int exitCode;
    /**
     * 命令执行输出
     */
    @Schema(description = "命令执行输出")
    private String result;

    public SshExecResult(int exitCode, String result) {
        this.exitCode = exitCode;
        this.result = result;
    }
}
