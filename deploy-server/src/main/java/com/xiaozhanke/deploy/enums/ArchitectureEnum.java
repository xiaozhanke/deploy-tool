package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 芯片架构枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum ArchitectureEnum {
    /**
     * X86 架构
     */
    X86("X86 架构"),

    /**
     * x64 架构
     */
    X64("x64 架构"),

    /**
     * ARM 架构
     */
    ARM("ARM 架构"),

    /**
     * AARCH64 架构
     */
    AARCH64("AARCH64 架构"),

    /**
     * 未知架构
     */
    UNKNOWN("未知架构");

    private final String description;
}