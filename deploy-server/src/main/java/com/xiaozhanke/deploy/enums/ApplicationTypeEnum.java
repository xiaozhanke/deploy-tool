package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用类型枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum ApplicationTypeEnum {
    /**
     * 前端应用
     */
    FRONTEND("前端应用"),

    /**
     * 后端应用
     */
    BACKEND("后端应用");

    private final String description;
}