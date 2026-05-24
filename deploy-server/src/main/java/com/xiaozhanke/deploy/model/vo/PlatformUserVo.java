package com.xiaozhanke.deploy.model.vo;

import com.xiaozhanke.deploy.model.base.BaseVo;
import com.xiaozhanke.deploy.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 用户信息 VO 类
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户信息")
public class PlatformUserVo extends BaseVo {

    /**
     * 用户 Id
     */
    @Schema(description = "用户 Id")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 用户显示名
     */
    @Schema(description = "用户显示名")
    private String displayName;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String phone;

    /**
     * 电子邮箱
     */
    @Schema(description = "电子邮箱")
    private String email;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 用户状态
     */
    @Schema(description = "用户状态")
    private UserStatusEnum status;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色")
    private Set<PlatformRoleVo> roles;
}
