package com.xiaozhanke.deploy.model.vo;

import com.xiaozhanke.deploy.model.base.BaseVo;
import com.xiaozhanke.deploy.enums.ArchitectureEnum;
import com.xiaozhanke.deploy.enums.FileScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 文件记录 VO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文件记录")
public class FileRecordVo extends BaseVo {

    /**
     * 文件 Id
     */
    @Schema(description = "文件 Id")
    private String id;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件相对路径
     */
    @Schema(description = "文件相对路径")
    private String relativePath;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 内容类型
     */
    @Schema(description = "文件类型")
    private String contentType;

    /**
     * 使用范围
     */
    @Schema(description = "使用范围")
    private FileScopeEnum scope;

    /**
     * 文件分组 Id
     */
    @Schema(description = "文件分组 Id")
    private String groupId;

    /**
     * 构件 Id
     */
    @Schema(description = "构件 Id")
    private String artifactId;

    /**
     * 版本
     */
    @Schema(description = "版本")
    private String version;

    /**
     * 芯片架构
     */
    @Schema(description = "芯片架构")
    private ArchitectureEnum architecture;

    /**
     * 文件描述
     */
    @Schema(description = "文件描述")
    private String description;
}
