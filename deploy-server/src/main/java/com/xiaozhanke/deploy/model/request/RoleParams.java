package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "角色参数")
public class RoleParams {
    /**
     * 角色名
     */
    @Schema(description = "角色名")
    @NotBlank(message = "角色名不能为空")
    private String name;

    /**
     * 角色描述
     */
    @Schema(description = "角色描述")
    private String description;
}
