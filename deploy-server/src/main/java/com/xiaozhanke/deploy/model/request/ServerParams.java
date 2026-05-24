package com.xiaozhanke.deploy.model.request;

import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import com.xiaozhanke.deploy.validation.ConditionalNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 服务器信息参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "服务器信息参数")
@ConditionalNotBlank(
        selected = "authType",
        values = {"PASSWORD"},
        required = {"password"},
        message = "密码认证方式下密码不能为空"
)
@ConditionalNotBlank(
        selected = "authType",
        values = {"KEY", "KEY_WITH_PASS"},
        required = {"privateKeyPath"},
        message = "密钥认证方式下私钥路径不能为空"
)
@ConditionalNotBlank(
        selected = "authType",
        values = {"KEY_WITH_PASS"},
        required = {"privateKeyPassword"},
        message = "带密码的密钥认证方式下私钥密码不能为空"
)
public class ServerParams {
    /**
     * 服务器名称
     */
    @Schema(description = "服务器名称")
    private String name;

    /**
     * 服务器描述
     */
    @Schema(description = "服务器描述")
    private String description;

    /**
     * 主机地址
     */
    @Schema(description = "主机地址", example = "localhost")
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /**
     * 端口号
     */
    @Schema(description = "端口号", minimum = "0", maximum = "65535", example = "8080")
    @NotNull(message = "端口号不能为空")
    @Min(value = 0, message = "端口号不能小于0")
    @Max(value = 65535, message = "端口号不能大于65535")
    private Integer port;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 主目录
     */
    @Schema(description = "主目录", example = "/home/root")
    private String homeDir;

    /**
     * 认证方式
     */
    @Schema(description = "认证方式")
    @NotNull(message = "认证方式不能为空")
    private SshAuthTypeEnum authType;

    /**
     * 密码（如果使用密码认证）
     */
    @Schema(description = "密码（如果使用密码认证）")
    private String password;

    /**
     * 私钥路径（如果使用密钥认证）
     */
    @Schema(description = "私钥路径（如果使用密钥认证）")
    private String privateKeyPath;

    /**
     * 私钥密码（如果私钥有密码保护）
     */
    @Schema(description = "私钥密码（如果私钥有密码保护）")
    private String privateKeyPassword;

    /**
     * 密钥交换算法
     */
    @Schema(description = "密钥交换算法")
    private String kexAlgorithms;

    /**
     * 加密算法
     */
    @Schema(description = "加密算法")
    private String cipherAlgorithms;

    /**
     * MAC 算法
     */
    @Schema(description = "MAC 算法")
    private String macAlgorithms;

    /**
     * 服务器主机密钥算法
     */
    @Schema(description = "服务器主机密钥算法")
    private String serverHostKeyAlgorithms;

    /**
     * 连接超时时间（毫秒）
     */
    @Schema(description = "连接超时时间（毫秒）", example = "30000")
    @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
    @Max(value = 60000, message = "连接超时时间不能大于60000毫秒")
    private Integer connectionTimeout;

    /**
     * 是否启用压缩
     */
    @Schema(description = "是否启用压缩")
    private Boolean compressionEnabled;

    /**
     * 是否启用严格的主机密钥检查
     */
    @Schema(description = "是否启用严格的主机密钥检查")
    private Boolean strictHostKeyChecking;

    /**
     * 是否启用 X11 转发
     */
    @Schema(description = "是否启用 X11 转发")
    private Boolean x11ForwardingEnabled;

    /**
     * 是否启用端口转发
     */
    @Schema(description = "是否启用端口转发")
    private Boolean portForwardingEnabled;
}
