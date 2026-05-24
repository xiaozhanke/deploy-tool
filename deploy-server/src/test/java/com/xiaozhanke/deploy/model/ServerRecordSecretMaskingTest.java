package com.xiaozhanke.deploy.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.entity.ServerRecord;
import com.xiaozhanke.deploy.model.vo.ServerRecordVo;
import org.junit.jupiter.api.Test;

/**
 * 验证 ServerRecord 三件套（DTO / 实体 / VO）的敏感字段不会被 toString 或 JSON 序列化无意泄露。
 *
 * <p>之前 SshService 的 debug 日志直接 {@code log.debug("server: {}", server)}，Lombok 默认 {@code toString}
 * 会把 password / privateKeyPassword 原样拼进去；Controller 返回的 VO 经 Jackson 序列化也会把这两个字段
 * 暴露给前端。本测试覆盖三层接口，确保 {@code @ToString.Exclude} 与 {@code @JsonIgnore} 同时生效。
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
    void voJsonSerializationMustNotLeakSecrets() throws Exception {
        ServerRecordVo vo = new ServerRecordVo();
        vo.setPassword(SENSITIVE_PASSWORD);
        vo.setPrivateKeyPassword(SENSITIVE_PASSPHRASE);

        String json = new ObjectMapper().writeValueAsString(vo);
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
