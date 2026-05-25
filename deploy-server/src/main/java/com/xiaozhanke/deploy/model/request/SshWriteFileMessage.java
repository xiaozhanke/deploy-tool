package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SFTP 写文件消息体
 *
 * <p>code-editor 保存远程文件时使用：通过 SFTP 直接 put 文本内容到 {@link #remotePath}，
 * 避免拼装 {@code cat <<EOF > path} 这类 shell 命令带来的注入风险。
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "SFTP 写文件消息体")
public class SshWriteFileMessage {
    /**
     * 远程文件绝对路径（POSIX 风格）
     */
    @Schema(description = "远程文件绝对路径，POSIX 分隔符 /", example = "/etc/nginx/conf.d/example.conf")
    @NotBlank(message = "远程文件路径不能为空")
    private String remotePath;

    /**
     * 文件内容（UTF-8）。配置类小文件场景为主，限制 1 MB 防止误传整库内容造成内存压力。
     */
    @Schema(description = "文件内容（UTF-8），最大 1MB")
    @NotNull(message = "文件内容不能为空")
    @Size(max = 1024 * 1024, message = "文件内容超过 1MB 上限")
    private String content;
}
