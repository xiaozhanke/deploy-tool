package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部署状态枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum DeploymentStatusEnum {
    /**
     * 部署中
     */
    DEPLOYING("部署中"),

    /**
     * 部署成功
     */
    SUCCESS("部署成功"),

    /**
     * 部署失败
     */
    FAILED("部署失败");

    private final String description;
}