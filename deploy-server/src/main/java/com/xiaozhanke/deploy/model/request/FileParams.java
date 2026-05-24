package com.xiaozhanke.deploy.model.request;

import com.xiaozhanke.deploy.enums.ArchitectureEnum;
import com.xiaozhanke.deploy.enums.FileScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "文件参数")
public class FileParams {

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件相对路径
     */
    @Schema(description = "文件相对路径", example = "/")
    private String relativePath;

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
