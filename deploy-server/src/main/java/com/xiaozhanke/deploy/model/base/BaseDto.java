package com.xiaozhanke.deploy.model.base;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 基础 DTO 类
 *
 * @author xiaozhanke
 */
@Data
public class BaseDto {
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-05-20 13:14:00")
    private LocalDateTime createTime;

    /**
     * 创建用户
     */
    @Schema(description = "创建用户", example = "admin")
    private String createUser;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2025-05-21 13:14:00")
    private LocalDateTime updateTime;

    /**
     * 更新用户
     */
    @Schema(description = "更新用户", example = "admin")
    private String updateUser;
}
