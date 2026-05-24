package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件适用范围枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum FileScopeEnum {
    /**
     * 环境安装
     */
    ENVIRONMENT("环境安装"),

    /**
     * 配置文件
     */
    CONFIGURATION("配置文件"),

    /**
     * 后端应用
     */
    APPLICATION_BACKEND("后端应用"),

    /**
     * 前端应用
     */
    APPLICATION_FRONTEND("前端应用");

    private final String description;
}
