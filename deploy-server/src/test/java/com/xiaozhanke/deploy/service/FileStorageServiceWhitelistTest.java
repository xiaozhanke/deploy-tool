package com.xiaozhanke.deploy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.xiaozhanke.deploy.config.FileStorageProperties;
import com.xiaozhanke.deploy.exception.InvalidOperationException;
import com.xiaozhanke.deploy.model.mapper.FileRecordPoVoMapper;
import com.xiaozhanke.deploy.model.request.FileParams;
import com.xiaozhanke.deploy.repository.FileRecordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 验证 FileStorageService 在启用白名单后的上传准入策略。
 *
 * <p>白名单为空时应放行所有扩展名（兼容存量），任何配置后立即对不在列表中的扩展名拒绝写盘并抛
 * {@link InvalidOperationException}。匹配规则忽略大小写、按"以扩展名结尾"判定，因此 {@code .tar.gz}
 * 这种复合后缀也能被识别为合法上传。
 *
 * @author xiaozhanke
 */
class FileStorageServiceWhitelistTest {

    private FileStorageProperties allowJarOnly;
    private FileStorageProperties allowComposite;
    private FileStorageProperties allowAll;

    @BeforeEach
    void initProperties() {
        allowJarOnly = new FileStorageProperties(List.of(".jar"));
        allowComposite = new FileStorageProperties(List.of(".jar", ".tar.gz", ".zip"));
        allowAll = new FileStorageProperties(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.jar", "App.JAR", "service-1.0.0.jar"})
    void whitelistAcceptsAllowedExtensionsCaseInsensitively(String fileName) {
        assertThat(allowJarOnly.isAllowed(fileName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"shell.sh", "config.yml", "evil.jsp", "danger.exe", "noext"})
    void whitelistRejectsExtensionsOutsideList(String fileName) {
        assertThat(allowJarOnly.isAllowed(fileName)).isFalse();
    }

    @Test
    void whitelistAcceptsCompoundExtensions() {
        assertThat(allowComposite.isAllowed("bundle.tar.gz")).isTrue();
        assertThat(allowComposite.isAllowed("Bundle.TAR.GZ")).isTrue();
        assertThat(allowComposite.isAllowed("just.gz")).isFalse();
    }

    @Test
    void emptyWhitelistAllowsEverything() {
        assertThat(allowAll.isAllowed("anything.foo")).isTrue();
        assertThat(allowAll.isAllowed("no-extension")).isTrue();
    }

    @Test
    void compactConstructorNormalizesEntries() {
        // 缺少前导点 / 大小写不一致 / 含空白都应被归一化
        FileStorageProperties props = new FileStorageProperties(List.of("JAR", " .Zip ", "tar.gz", " "));
        assertThat(props.allowedExtensions()).containsExactly(".jar", ".zip", ".tar.gz");
    }

    @Test
    void storeFileRejectsExtensionOutsideWhitelist() throws Exception {
        FileStorageService service = new FileStorageService(
                mock(FileRecordRepository.class),
                mock(FileRecordPoVoMapper.class),
                allowJarOnly);

        MockMultipartFile evil = new MockMultipartFile(
                "file",
                "exploit.jsp",
                "application/octet-stream",
                "<% Runtime.getRuntime().exec(...); %>".getBytes());

        FileParams params = new FileParams();
        params.setRelativePath("/upload");

        assertThatThrownBy(() -> service.storeFile(evil, params))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("非法文件类型");
    }
}
