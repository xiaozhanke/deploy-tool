package com.xiaozhanke.deploy.util;

import com.xiaozhanke.deploy.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 远程（SFTP）路径与文件名安全校验工具。
 *
 * <p>SFTP 服务端运行在 POSIX 主机上，统一用 {@code /} 作为分隔符。Java 端的 {@link java.nio.file.Paths}
 * 会按当前 OS 选择分隔符，在 Windows 开发机上反而会把 {@code "/"} 与 {@code "\\"} 都纳入考虑，
 * 因此这里手写 POSIX 风格的规范化与穿越检测，不依赖 NIO。
 *
 * <p>校验目标：
 * <ul>
 *   <li>{@link #assertSafeFileName(String)} —— 上传时从 HTTP 拿到的 originalFilename 不能包含 {@code ..}、{@code /}、{@code \}
 *       或控制字符，避免 {@code "remoteDir/" + name} 跳出目标目录</li>
 *   <li>{@link #safeJoin(String, String)} —— 把已校验的 baseDir 和 fileName 拼成 POSIX 路径，
 *       兜底校验拼接结果仍在 baseDir 之内，防止 baseDir 自身被植入 {@code ..} 段</li>
 *   <li>{@link #assertNoTraversalSegments(String)} —— prepareRemoteDirectory 这种逐段 mkdir 的场景必须先确认整条路径没有 {@code ..} 段</li>
 * </ul>
 *
 * @author xiaozhanke
 */
public final class PathSafetyUtils {

    private PathSafetyUtils() {
    }

    /**
     * 校验上传文件名安全：不允许 {@code ..}、{@code /}、{@code \}、空白、NUL、换行等可改变路径解析的字符。
     *
     * @param fileName 原始文件名
     * @throws BusinessException 若文件名非法
     */
    public static void assertSafeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不能为空");
        }
        if (fileName.contains("..") || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new BusinessException(String.format("文件名包含非法路径片段: %s", fileName));
        }
        if (fileName.indexOf('\0') >= 0 || fileName.indexOf('\n') >= 0 || fileName.indexOf('\r') >= 0) {
            throw new BusinessException(String.format("文件名包含控制字符: %s", fileName));
        }
    }

    /**
     * 校验 baseDir 不含 {@code ..} 段或控制字符，prepareRemoteDirectory 这种按段 mkdir 的逻辑必须先过这一关。
     *
     * @param posixPath POSIX 风格目录路径
     * @throws BusinessException 若路径含非法段
     */
    public static void assertNoTraversalSegments(String posixPath) {
        if (!StringUtils.hasText(posixPath)) {
            throw new BusinessException("远程目录不能为空");
        }
        if (posixPath.indexOf('\0') >= 0 || posixPath.indexOf('\n') >= 0 || posixPath.indexOf('\r') >= 0) {
            throw new BusinessException(String.format("远程目录包含控制字符: %s", posixPath));
        }
        for (String segment : posixPath.split("/")) {
            if ("..".equals(segment)) {
                throw new BusinessException(String.format("远程目录包含非法路径片段 '..': %s", posixPath));
            }
        }
    }

    /**
     * 把 baseDir 与已校验的 fileName 用 POSIX 分隔符拼起来，并兜底确认拼接结果未跳出 baseDir。
     *
     * @param baseDir  远程目标目录（POSIX 风格，绝对或相对均可）
     * @param fileName 已通过 {@link #assertSafeFileName(String)} 的纯文件名
     * @return 形如 {@code baseDir/fileName} 的拼接结果
     */
    public static String safeJoin(String baseDir, String fileName) {
        assertSafeFileName(fileName);
        assertNoTraversalSegments(baseDir);
        String normalizedBase = baseDir.endsWith("/") ? baseDir.substring(0, baseDir.length() - 1) : baseDir;
        String joined = normalizedBase + "/" + fileName;
        // 兜底：再确认拼接结果与 baseDir 同根
        if (!joined.startsWith(normalizedBase + "/")) {
            throw new BusinessException(String.format("拼接路径跳出目标目录: base=%s, name=%s", baseDir, fileName));
        }
        return joined;
    }
}
