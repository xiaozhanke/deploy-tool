package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Nginx 配置参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "Nginx 配置参数")
public class NginxConfigParams {
    /**
     * 配置名称
     */
    @Schema(description = "配置名称")
    @NotBlank(message = "配置名不能为空")
    private String configName;

    /**
     * 前端主机地址
     */
    @Schema(description = "前端主机地址", defaultValue = "localhost")
    private String frontEndHost = "localhost";

    /**
     * 前端端口
     */
    @Schema(description = "前端端口", minimum = "0", maximum = "65535", example = "8080")
    @NotNull(message = "前端端口不能为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65535")
    private Integer frontEndPort;

    /**
     * 前端静态资源路径
     */
    @Schema(description = "前端静态资源路径")
    @NotBlank(message = "前端静态资源路径不能为空")
    private String frontEndStaticDir;

    /**
     * 后端主机地址
     */
    @Schema(description = "后端主机地址", defaultValue = "localhost")
    private String backEndHost = "localhost";

    /**
     * 后端端口
     */
    @Schema(description = "后端端口", minimum = "0", maximum = "65535", example = "8080")
    @NotNull(message = "后端端口不能为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65535")
    private Integer backEndPort;
}
