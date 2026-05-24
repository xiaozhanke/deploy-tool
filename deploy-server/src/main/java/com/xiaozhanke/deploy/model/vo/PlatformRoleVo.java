package com.xiaozhanke.deploy.model.vo;

import com.xiaozhanke.deploy.model.base.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 角色信息 VO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色信息")
public class PlatformRoleVo extends BaseVo {

    /**
     * 角色 Id
     */
    @Schema(description = "角色 Id")
    private String id;

    /**
     * 角色名
     */
    @Schema(description = "角色名")
    private String name;

    /**
     * 角色描述
     */
    @Schema(description = "角色描述")
    private String description;
}
