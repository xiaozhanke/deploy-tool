package com.xiaozhanke.deploy.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传白名单配置。
 *
 * <p>之前 {@code FileStorageService.storeFile} / {@code updateRaw} 接受任意扩展名，攻击者可以伪装成图片上传
 * {@code .jsp}、{@code .php}、可执行脚本等危险类型，再通过其他通道触发执行。本配置把允许的扩展名外置到
 * {@code application.yml}，缺失或显式空列表代表"放行所有"——明确退化路径，方便存量环境平滑迁移；
 * 实际启动后由 {@link com.xiaozhanke.deploy.service.FileStorageService} 调用 {@link #isAllowed(String)} 校验。
 *
 * <p>扩展名匹配规则：忽略大小写、按"文件名以扩展名结尾"判断，因此 {@code .tar.gz} 这类复合后缀也能命中；
 * 配置项写法保持"前导点 + 小写"，例如 {@code .jar}、{@code .tar.gz}。
 *
 * @param allowedExtensions 允许的文件扩展名列表（含前导点）；空列表表示不做白名单限制
 * @author xiaozhanke
 */
@ConfigurationProperties(prefix = "app.file")
public record FileStorageProperties(List<String> allowedExtensions) {

    public FileStorageProperties {
        allowedExtensions = allowedExtensions == null
                ? List.of()
                : allowedExtensions.stream()
                        .filter(ext -> ext != null && !ext.isBlank())
                        .map(ext -> ext.trim().toLowerCase(Locale.ROOT))
                        .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                        .toList();
    }

    /**
     * 判断给定文件名是否在白名单内。
     *
     * @param fileName 已经过 {@code sanitizeUploadedFileName} 清洗的纯文件名
     * @return 白名单为空表示放行所有；否则只有以白名单中任意扩展名结尾的文件才算合法
     */
    public boolean isAllowed(String fileName) {
        if (allowedExtensions.isEmpty()) {
            return true;
        }
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return allowedExtensions.stream().anyMatch(lower::endsWith);
    }
}
