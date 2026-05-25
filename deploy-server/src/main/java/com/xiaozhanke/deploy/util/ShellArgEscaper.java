package com.xiaozhanke.deploy.util;

import com.xiaozhanke.deploy.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * POSIX shell 参数转义工具。
 *
 * <p>DeploymentService 拼 nohup / kill 命令时把用户提交的 {@code programArgs}、{@code activeProfiles}、
 * {@code processId}、部署路径、jar 文件名直接 {@code String.format} 进命令字符串，遇到包含 {@code ;}、
 * {@code &&}、反引号、{@code $()} 的输入就能注入额外命令。统一用 {@link #singleQuote(String)} 套单引号，
 * 内部的 {@code '} 转义为 {@code '\''}（关闭单引号 + 加转义单引号 + 重新打开单引号）即可让 bash 把
 * 整段当字面值看待。
 *
 * <p>另外强制黑名单：换行 / 回车字符可能截断命令上下文（部分历史 shell 实现），统一拒绝。
 *
 * @author xiaozhanke
 */
public final class ShellArgEscaper {

    private ShellArgEscaper() {
    }

    /**
     * 把任意字符串安全地包装为 POSIX shell 单引号字面值。空输入返回空单引号串 {@code ''}。
     *
     * @param value 用户提交字符串
     * @return 形如 {@code 'foo'} / {@code 'a'\''b'} 的字面值
     * @throws BusinessException 若输入含换行 / 回车
     */
    public static String singleQuote(String value) {
        if (value == null) {
            return "''";
        }
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0) {
            throw new BusinessException("命令参数包含换行 / 回车 / NUL 等控制字符，禁止注入命令");
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    /**
     * 校验 processId 仅由数字组成。kill -15 接非数字会被 shell 解析或当 jobspec，可能造成意外行为。
     *
     * @param processId 进程号
     * @throws BusinessException 若 processId 不是纯数字
     */
    public static String requireNumericProcessId(String processId) {
        if (!StringUtils.hasText(processId) || !processId.chars().allMatch(Character::isDigit)) {
            throw new BusinessException(String.format("进程号必须是数字: %s", processId));
        }
        return processId;
    }
}
