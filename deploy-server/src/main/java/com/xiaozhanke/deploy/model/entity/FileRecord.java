package com.xiaozhanke.deploy.model.entity;

import com.xiaozhanke.deploy.model.base.BasePo;
import com.xiaozhanke.deploy.enums.ArchitectureEnum;
import com.xiaozhanke.deploy.enums.FileScopeEnum;
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
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;

/**
 * 文件记录 PO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "file_record")
@Comment("文件记录表")
public class FileRecord extends BasePo {

    /**
     * 文件 Id
     */
    @Comment("文件 Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 文件名
     */
    @Comment("文件名")
    @Column(nullable = false)
    private String fileName;

    /**
     * 文件相对路径
     */
    @Comment("文件相对路径")
    @Column(nullable = false)
    private String relativePath;

    /**
     * 文件大小（字节）
     */
    @Comment("文件大小（字节）")
    @Column
    private Long fileSize;

    /**
     * 内容类型
     */
    @Comment("内容类型")
    @Column
    private String contentType;

    /**
     * 使用范围
     */
    @Comment("使用范围")
    @Column
    @Enumerated(EnumType.STRING)
    private FileScopeEnum scope;

    /**
     * 文件分组 Id
     */
    @Comment("文件分组 Id")
    @Column
    private String groupId;

    /**
     * 构件 Id
     */
    @Comment("构件 Id")
    @Column
    private String artifactId;

    /**
     * 版本
     */
    @Comment("版本")
    @Column
    private String version;

    /**
     * 芯片架构
     */
    @Comment("芯片架构")
    @Column
    @Enumerated(EnumType.STRING)
    private ArchitectureEnum architecture;

    /**
     * 文件描述
     */
    @Comment("文件描述")
    @Column
    private String description;
}
