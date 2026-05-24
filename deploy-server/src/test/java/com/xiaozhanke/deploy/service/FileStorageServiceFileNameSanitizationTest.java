package com.xiaozhanke.deploy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xiaozhanke.deploy.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 验证 {@link FileStorageService#sanitizeUploadedFileName(String)} 在保存上传文件前的净化逻辑。
 *
 * <p>对应的"路径穿越"场景有两类：
 * 1. 文件名中含 {@code ..}（路径直接跳出 targetDir） —— 必须拒绝；
 * 2. 文件名携带子目录前缀（{@code folder/file.jar} / {@code C:\\Users\\foo.jar}） —— 剥离前缀只保留纯文件名，结果落到 targetDir 内。
 *
 * <p>对于"纯文件名"输入则保持原样返回，确保中文、空格、版本号点分等正常用例不被误杀。
 *
 * @author xiaozhanke
 */
class FileStorageServiceFileNameSanitizationTest {

    @Test
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> FileStorageService.sanitizeUploadedFileName(null))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> FileStorageService.sanitizeUploadedFileName(""))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> FileStorageService.sanitizeUploadedFileName("   "))
                .isInstanceOf(InvalidOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../.."})
    void rejectsPathTraversalThatCollapsesToParent(String unsafe) {
        // cleanPath 处理后无法消解，getFilename 仍含 ..，必须拒绝
        assertThatThrownBy(() -> FileStorageService.sanitizeUploadedFileName(unsafe))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("非法文件名");
    }

    @ParameterizedTest
    @ValueSource(strings = {"file\0name.jar", "file\nname.jar", "file\rname.jar"})
    void rejectsControlChars(String unsafe) {
        assertThatThrownBy(() -> FileStorageService.sanitizeUploadedFileName(unsafe))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void stripsDirectoryPrefixFromBrowserSubmittedPath() {
        // 老版 IE 把整条 Windows 路径塞进 filename，只保留尾部文件名后落在合法目录内即可
        assertThat(FileStorageService.sanitizeUploadedFileName("C:\\Users\\foo\\build.jar"))
                .isEqualTo("build.jar");
        // Unix 风格目录前缀也一并剥离
        assertThat(FileStorageService.sanitizeUploadedFileName("uploads/sub/build.jar"))
                .isEqualTo("build.jar");
        // 多层 ../ 前缀被 cleanPath 保留、getFilename 抽出末段——已脱离 .. 含义，落到 targetDir 内即可
        assertThat(FileStorageService.sanitizeUploadedFileName("../../passwd"))
                .isEqualTo("passwd");
        assertThat(FileStorageService.sanitizeUploadedFileName("../../etc/passwd"))
                .isEqualTo("passwd");
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.jar", "build.zip", "version-1.2.3.tar.gz", "中文.txt", "name with space.jar"})
    void acceptsNormalFileNames(String safe) {
        assertThat(FileStorageService.sanitizeUploadedFileName(safe)).isEqualTo(safe);
    }
}
