package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH SFTP 上传消息体
 *
 * @author xiaozhanke
 */
@Schema(description = "SSH SFTP 上传消息体")
@Data
public class SshSftpUploadMessage {
    /**
     * 本地文件路径
     */
    @Schema(description = "本地文件路径")
    @NotBlank(message = "本地文件路径不能为空")
    private String localPath;

    /**
     * 远程目录路径
     */
    @Schema(description = "远程目录路径")
    @NotBlank(message = "远程目录路径不能为空")
    private String remoteDir;
}
