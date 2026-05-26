package com.xiaozhanke.deploy.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.entity.ServerRecord;
import org.junit.jupiter.api.Test;

/**
 * 验证 ServerRecord 在内存 / 序列化路径里携带的敏感字段不会被 toString 或 Jackson 无意泄露。
 *
 * <p>覆盖的两层：
 * <ul>
 *   <li>{@link ServerRecordDto}：服务层内部传递凭据用，{@code @ToString.Exclude} +
 *       {@code @JsonIgnore} 防止日志和 JSON 误带</li>
 *   <li>{@link ServerRecord}：JPA 实体，进入懒加载日志或调试输出时也不能暴露</li>
 * </ul>
 *
 * <p>{@code ServerRecordVo} 已彻底移除 password / privateKeyPassword 字段，从类型系统层面
 * 杜绝出站泄露，无需也无法再用运行时断言覆盖。
 *
 * @author xiaozhanke
 */
class ServerRecordSecretMaskingTest {

    private static final String SENSITIVE_PASSWORD = "p@ssword-leak-canary";
    private static final String SENSITIVE_PASSPHRASE = "passphrase-leak-canary";

    @Test
    void dtoToStringMustNotLeakSecrets() {
        ServerRecordDto dto = new ServerRecordDto();
        dto.setName("test");
        dto.setHost("127.0.0.1");
        dto.setUsername("user");
        dto.setAuthType(SshAuthTypeEnum.PASSWORD);
        dto.setPassword(SENSITIVE_PASSWORD);
        dto.setPrivateKeyPassword(SENSITIVE_PASSPHRASE);

        String rendered = dto.toString();
        assertThat(rendered)
                .doesNotContain(SENSITIVE_PASSWORD)
                .doesNotContain(SENSITIVE_PASSPHRASE);
    }

    @Test
    void dtoJsonSerializationMustNotLeakSecrets() throws Exception {
        ServerRecordDto dto = new ServerRecordDto();
        dto.setPassword(SENSITIVE_PASSWORD);
        dto.setPrivateKeyPassword(SENSITIVE_PASSPHRASE);

        String json = new ObjectMapper().writeValueAsString(dto);
        assertThat(json)
                .doesNotContain(SENSITIVE_PASSWORD)
                .doesNotContain(SENSITIVE_PASSPHRASE);
    }

    @Test
    void entityToStringMustNotLeakSecrets() {
        ServerRecord entity = new ServerRecord();
        entity.setPassword(SENSITIVE_PASSWORD);
        entity.setPrivateKeyPassword(SENSITIVE_PASSPHRASE);

        String rendered = entity.toString();
        assertThat(rendered)
                .doesNotContain(SENSITIVE_PASSWORD)
                .doesNotContain(SENSITIVE_PASSPHRASE);
    }
}
