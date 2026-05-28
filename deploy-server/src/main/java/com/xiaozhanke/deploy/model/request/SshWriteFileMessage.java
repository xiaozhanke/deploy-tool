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

    /**
     * 是否使用 sudo 提权落盘。
     *
     * <p>目标目录归属 root（典型 {@code /etc/nginx/conf.d}）时启用：后端先 SFTP 写到
     * {@code /tmp} 临时文件（路径与内容都过 SFTP 协议字段，无任何 shell 解释），再 exec
     * {@code sudo -n mv} 提权移动到目标位置。前提是远端登录用户配置了 NOPASSWD sudo。
     *
     * <p>不启用时（默认）走原有纯 SFTP 直写，写不进去时由远端 SFTP 报权限错误。
     */
    @Schema(description = "是否使用 sudo 提权落盘（写 root 目录时启用，远端需 NOPASSWD sudo）", example = "false")
    private boolean useSudo = false;
}
