package com.xiaozhanke.deploy.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xiaozhanke.deploy.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * 验证 {@link PathSafetyUtils} 的路径穿越识别能力。
 *
 * <p>覆盖三类常见绕过：
 * 1. {@code originalFilename = "../../etc/passwd"} —— SFTP 服务端会向上跳出目标目录；
 * 2. {@code originalFilename = "folder/file.jar"} —— 在合法目录内创建子目录，相对低危但仍越权；
 * 3. {@code remoteDir} 中夹带 {@code ".."} 段 —— prepareRemoteDirectory 按段 mkdir 时会被 sftp 解析。
 *
 * @author xiaozhanke
 */
class PathSafetyUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "../etc/passwd",
            "..\\windows\\system32",
            "folder/file.jar",
            "folder\\file.jar",
            "..",
            "../..",
            "name\0.jar",
            "name\n.jar",
            "name\r.jar"
    })
    void assertSafeFileNameRejectsTraversalAndControlChars(String unsafe) {
        assertThatThrownBy(() -> PathSafetyUtils.assertSafeFileName(unsafe))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void assertSafeFileNameRejectsBlank(String blank) {
        assertThatThrownBy(() -> PathSafetyUtils.assertSafeFileName(blank))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertSafeFileNameRejectsNull() {
        assertThatThrownBy(() -> PathSafetyUtils.assertSafeFileName(null))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.jar", "build.zip", "version-1.2.3.tar.gz", "中文名.txt", "name with space.jar"})
    void assertSafeFileNameAcceptsValidNames(String safe) {
        PathSafetyUtils.assertSafeFileName(safe);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/opt/app/../etc",
            "../relative",
            "/a/b/../../c",
            "/a/b/c\0d"
    })
    void assertNoTraversalSegmentsRejectsBadPaths(String unsafe) {
        assertThatThrownBy(() -> PathSafetyUtils.assertNoTraversalSegments(unsafe))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/opt/app", "/opt/app/", "opt/app", "/", "/single"})
    void assertNoTraversalSegmentsAcceptsValid(String safe) {
        PathSafetyUtils.assertNoTraversalSegments(safe);
    }

    @Test
    void safeJoinStripsTrailingSlashAndConcatenates() {
        assertThat(PathSafetyUtils.safeJoin("/opt/app", "build.jar")).isEqualTo("/opt/app/build.jar");
        assertThat(PathSafetyUtils.safeJoin("/opt/app/", "build.jar")).isEqualTo("/opt/app/build.jar");
    }

    @Test
    void safeJoinRejectsUnsafeFileName() {
        assertThatThrownBy(() -> PathSafetyUtils.safeJoin("/opt/app", "../etc/passwd"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void safeJoinRejectsUnsafeBaseDir() {
        assertThatThrownBy(() -> PathSafetyUtils.safeJoin("/opt/app/..", "build.jar"))
                .isInstanceOf(BusinessException.class);
    }
}
