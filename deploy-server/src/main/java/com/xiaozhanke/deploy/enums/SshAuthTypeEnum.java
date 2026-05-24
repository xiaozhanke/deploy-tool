package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SSH 认证方式枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum SshAuthTypeEnum {
    /**
     * 密码认证
     */
    PASSWORD("密码认证"),

    /**
     * 密钥认证
     */
    KEY("密钥认证"),

    /**
     * 带密码的密钥认证
     */
    KEY_WITH_PASS("带密码的密钥认证");

    private final String description;
} 