package com.xiaozhanke.deploy.model.entity;

import com.xiaozhanke.deploy.model.base.BasePo;
import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;

/**
 * 服务器记录 PO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "server_record")
@Comment("服务器记录表")
public class ServerRecord extends BasePo {
    /**
     * 服务器 Id
     */
    @Comment("服务器 Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 服务器名称
     */
    @Comment("服务器名称")
    @Column(nullable = false)
    private String name;

    /**
     * 服务器描述
     */
    @Comment("服务器描述")
    @Column
    private String description;

    /**
     * 主机地址
     */
    @Comment("主机地址")
    @Column(nullable = false)
    private String host;

    /**
     * 端口号
     */
    @Comment("端口号")
    @Column(nullable = false)
    private Integer port;

    /**
     * 用户名
     */
    @Comment("用户名")
    @Column(nullable = false)
    private String username;

    /**
     * 主目录
     */
    @Comment("主目录")
    @Column(nullable = false)
    private String homeDir;

    /**
     * 认证方式
     */
    @Comment("认证方式")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SshAuthTypeEnum authType;

    /**
     * 密码（如果使用密码认证）
     */
    @Comment("密码（如果使用密码认证）")
    @Column
    private String password;

    /**
     * 私钥路径（如果使用密钥认证）
     */
    @Comment("私钥路径（如果使用密钥认证）")
    @Column
    private String privateKeyPath;

    /**
     * 私钥密码（如果私钥有密码保护）
     */
    @Comment("私钥密码（如果私钥有密码保护）")
    @Column
    private String privateKeyPassword;

    /**
     * 密钥交换算法
     */
    @Comment("密钥交换算法")
    @Column
    private String kexAlgorithms;

    /**
     * 加密算法
     */
    @Comment("加密算法")
    @Column
    private String cipherAlgorithms;

    /**
     * MAC 算法
     */
    @Comment("MAC 算法")
    @Column
    private String macAlgorithms;

    /**
     * 服务器主机密钥算法
     */
    @Comment("服务器主机密钥算法")
    @Column
    private String serverHostKeyAlgorithms;

    /**
     * 连接超时时间（毫秒）
     */
    @Comment("连接超时时间（毫秒）")
    @Column
    private Integer connectionTimeout;

    /**
     * 是否启用压缩
     */
    @Comment("是否启用压缩")
    @Column
    private Boolean compressionEnabled;

    /**
     * 是否启用严格的主机密钥检查
     */
    @Comment("是否启用严格的主机密钥检查")
    @Column
    private Boolean strictHostKeyChecking;

    /**
     * 是否启用 X11 转发
     */
    @Comment("是否启用 X11 转发")
    @Column
    private Boolean x11ForwardingEnabled;

    /**
     * 是否启用端口转发
     */
    @Comment("是否启用端口转发")
    @Column
    private Boolean portForwardingEnabled;
} 