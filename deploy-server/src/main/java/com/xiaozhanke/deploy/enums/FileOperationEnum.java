package com.xiaozhanke.deploy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件操作类型枚举
 *
 * @author xiaozhanke
 */
@Getter
@AllArgsConstructor
public enum FileOperationEnum {

    /**
     * 上传
     */
    UPLOAD("上传"),

    /**
     * 下载
     */
    DOWNLOAD("下载");

    private final String description;
}
