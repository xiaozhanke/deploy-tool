package com.xiaozhanke.deploy.model.entity;

import com.xiaozhanke.deploy.enums.ApplicationTypeEnum;
import com.xiaozhanke.deploy.enums.DeploymentStatusEnum;
import com.xiaozhanke.deploy.model.base.BasePo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 部署记录 PO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "deployment_record")
@Comment("部署记录表")
public class DeploymentRecord extends BasePo {
    /**
     * 部署 Id
     */
    @Comment("部署 Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 服务器记录
     */
    @Comment("服务器记录")
    @ManyToOne
    @JoinColumn(name = "server_record_id", nullable = false)
    private ServerRecord serverRecord;

    /**
     * 文件记录
     */
    @Comment("文件记录")
    @ManyToOne
    @JoinColumn(name = "file_record_id", nullable = false)
    private FileRecord fileRecord;

    /**
     * 应用类型
     */
    @Comment("应用类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationTypeEnum applicationType;

    /**
     * 部署路径
     */
    @Comment("部署路径")
    @Column(nullable = false)
    private String deploymentPath;

    /**
     * 配置文件夹路径
     */
    @Comment("配置文件夹路径")
    @Column
    private String deploymentConfigPath;

    /**
     * 部署端口
     */
    @Comment("部署端口")
    @Column
    private Integer port;

    /**
     * 程序参数
     */
    @Comment("程序参数")
    @Column
    private String programArgs;

    /**
     * 激活的配置文件
     */
    @Comment("激活的配置文件")
    @Column
    private String activeProfiles;

    /**
     * 部署状态
     */
    @Comment("部署状态")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeploymentStatusEnum status;

    /**
     * 错误信息
     */
    @Comment("错误信息")
    @Column
    private String errorMessage;

    /**
     * 部署时间
     */
    @Comment("部署时间")
    @Column(nullable = false)
    private LocalDateTime deployTime;

    /**
     * 最后启动时间
     */
    @Comment("最后启动时间")
    @Column
    private LocalDateTime lastStartTime;

    /**
     * 最后停止时间
     */
    @Comment("最后停止时间")
    @Column
    private LocalDateTime lastStopTime;

    /**
     * 进程 Id
     */
    @Comment("进程 Id")
    @Column
    private String processId;

    /**
     * 是否正在运行
     */
    @Comment("是否正在运行")
    @Column(name = "is_running")
    private Boolean running;
}