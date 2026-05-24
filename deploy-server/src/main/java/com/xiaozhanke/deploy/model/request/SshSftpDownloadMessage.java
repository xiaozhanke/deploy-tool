package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH SFTP 下载消息体
 *
 * @author xiaozhanke
 */
@Schema(description = "SSH SFTP 下载消息体")
@Data
public class SshSftpDownloadMessage {
    /**
     * 远程文件路径
     */
    @Schema(description = "远程文件路径")
    @NotBlank(message = "远程文件路径不能为空")
    private String remotePath;

    /**
     * 本地目录路径
     */
    @Schema(description = "本地目录路径")
    @NotBlank(message = "本地目录路径不能为空")
    private String localDir;
}
