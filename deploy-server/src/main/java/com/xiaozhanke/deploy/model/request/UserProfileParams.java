package com.xiaozhanke.deploy.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户信息参数
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "用户信息参数")
public class UserProfileParams {
    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 用户显示名
     */
    @Schema(description = "用户显示名")
    @NotBlank(message = "用户显示名不能为空")
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
}
