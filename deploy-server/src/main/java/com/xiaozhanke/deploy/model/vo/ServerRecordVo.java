package com.xiaozhanke.deploy.model.vo;

import com.xiaozhanke.deploy.model.base.BaseVo;
import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 服务器记录 VO 类
 *
 * <p>VO 不携带任何凭据字段：password 与 privateKeyPassword 在 PO 上仍然保留以供建立连接使用，
 * 但绝不通过 VO 出站；如需在内部传递凭据请走 {@link com.xiaozhanke.deploy.model.dto.ServerRecordDto}。
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "服务器记录信息")
public class ServerRecordVo extends BaseVo {
    /**
     * 服务器 Id
     */
    @Schema(description = "服务器 Id")
    private String id;

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
    private String host;

    /**
     * 端口号
     */
    @Schema(description = "端口号", minimum = "0", maximum = "65535", example = "8080")
    private Integer port;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
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
    private SshAuthTypeEnum authType;

    /**
     * 私钥路径（如果使用密钥认证）
     */
    @Schema(description = "私钥路径（如果使用密钥认证）")
    private String privateKeyPath;

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
