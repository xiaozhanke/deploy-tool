package com.xiaozhanke.deploy.core.ssh;

import com.xiaozhanke.deploy.constant.SshConstants;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * Shell 命令执行任务
 *
 * @author xiaozhanke
 */
@Data
public class ShellCommandTask {

    /**
     * 任务 Id
     */
    private final String taskId;

    /**
     * 原始命令
     */
    private final String originalCommand;

    /**
     * 回调接口
     */
    private final CommandResultCallback callback;

    /**
     * 命令是否已发送
     */
    private boolean commandSent = false;

    /**
     * 是否已处理完成
     */
    private boolean completed = false;

    public ShellCommandTask(String taskId, String command, CommandResultCallback callback) {
        this.taskId = taskId;
        this.originalCommand = command;
        this.callback = callback;
    }

    public String getFinalCommand() {
        String command = StringUtils.hasText(originalCommand) ? originalCommand : "pwd";
        return command.trim() + SshConstants.EXIT_CODE_COMMAND + "\n";
    }
}
