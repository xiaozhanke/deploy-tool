package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.core.ssh.CommandResultCallback;
import com.xiaozhanke.deploy.model.request.SshSftpDownloadMessage;
import com.xiaozhanke.deploy.model.request.SshSftpUploadMessage;
import com.xiaozhanke.deploy.model.request.SshShellMessage;
import com.xiaozhanke.deploy.service.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

/**
 * WebSocket 中 SSH 相关消息处理接口
 *
 * @author xiaozhanke
 */
@Slf4j
@Controller
public class WebSocketSshController {

    private final SshService sshService;

    public WebSocketSshController(SshService sshService) {
        this.sshService = sshService;
    }

    /**
     * 执行交互式 Shell 命令
     *
     * @param sessionId 会话 Id
     * @param channelId 通道 Id
     * @param message   WebSocket 消息体，包含要执行的命令 command
     */
    @MessageMapping("/ssh/sessions/{sessionId}/shell/{channelId}")
    public void handleShellCommand(@DestinationVariable String sessionId, @DestinationVariable String channelId, @Validated SshShellMessage message) {
        String command = message.getCommand();
        String taskId = message.getTaskId();
        sshService.executeShellCommand(sessionId, channelId, taskId, command, new CommandResultCallback() {

            @Override
            public void onSuccess(String sessionId, String channelId, String output) {
                // TODO: 持久化执行结果
                log.info("命令执行成功: 会话 [{}] 通道 [{}] 命令 '{}', 执行输出:\n{}", sessionId, channelId, command, output);
            }

            @Override
            public void onFailure(String sessionId, String channelId, String error, int exitCode) {
                // TODO: 持久化执行结果
                log.info("命令执行失败: 会话 [{}] 通道 [{}] 命令 '{}', 退出码[{}], 执行输出:\n{}", sessionId, channelId, command, exitCode, error);
            }
        });
    }

    /**
     * 执行 SFTP 文件上传
     *
     * @param sessionId 会话 Id
     * @param message   WebSocket 消息体，包含本地文件路径和远程目录路径
     */
    @MessageMapping("/ssh/sessions/{sessionId}/sftp/upload")
    public void handleSftpUpload(@DestinationVariable String sessionId, @Validated SshSftpUploadMessage message) {
        sshService.uploadFile(sessionId, message.getLocalPath(), message.getRemoteDir());
    }

    /**
     * 执行 SFTP 文件下载
     *
     * @param sessionId 会话 Id
     * @param message   WebSocket 消息体，包含远程文件路径和本地目录路径
     */
    @MessageMapping("/ssh/sessions/{sessionId}/sftp/download")
    public void handleSftpDownload(@DestinationVariable String sessionId, @Validated SshSftpDownloadMessage message) {
        sshService.downloadFile(sessionId, message.getRemotePath(), message.getLocalDir());
    }

}
