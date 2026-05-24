package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /**
     * 初始
     */
    INITIALIZED("初始"),
    /**
     * 正常
     */
    ACTIVE("正常"),
    /**
     * 锁定
     */
    LOCKED("锁定"),
    /**
     * 停用
     */
    DISABLED("停用");

    private final String description;
}
