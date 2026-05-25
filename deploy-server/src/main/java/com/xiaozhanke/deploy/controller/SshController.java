package com.xiaozhanke.deploy.controller;


import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.dto.SshExecResult;
import com.xiaozhanke.deploy.model.request.SshExecMessage;
import com.xiaozhanke.deploy.model.request.SshWriteFileMessage;
import com.xiaozhanke.deploy.service.ServerService;
import com.xiaozhanke.deploy.service.SshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * SSH 操作接口
 *
 * @author xiaozhanke
 */
@Tag(name = "ssh", description = "SSH 接口")
@RestController
@RequestMapping("/ssh")
public class SshController {

    private final SshService sshService;
    private final ServerService serverService;

    public SshController(SshService sshService, ServerService serverService) {
        this.sshService = sshService;
        this.serverService = serverService;
    }

    /**
     * 创建一个服务器连接会话
     *
     * @param serverId 服务器 Id
     * @return 会话 Id
     */
    @Operation(summary = "创建连接会话", description = "创建 JSch Session 连接会话并连接，返回会话 Id")
    @PostMapping("/sessions")
    public ResponseEntity<String> connect(@Parameter(description = "服务器 Id", required = true) @RequestParam String serverId) {
        ServerRecordDto server = serverService.getServerDto(serverId);
        String sessionId = sshService.connect(server);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{sessionId}").buildAndExpand(sessionId).toUri();
        return ResponseEntity.created(location).body(sessionId);
    }

    /**
     * 断开服务器连接会话
     *
     * @param sessionId 会话 Id
     */
    @Operation(summary = "断开连接会话", description = "断开 JSch Session 会话")
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId) {
        sshService.disconnect(sessionId);
    }

    /**
     * 创建一个交互式 Shell 通道，但不连接
     *
     * @param sessionId 会话 Id
     * @return 通道 Id
     */
    @Operation(summary = "创建 Shell 通道", description = "创建一个交互式 Shell 通道，但不连接，返回通道 Id")
    @PostMapping("/sessions/{sessionId}/shell")
    public ResponseEntity<String> shellConnect(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId) {
        String channelId = sshService.addShellChannel(sessionId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{channelId}").buildAndExpand(channelId).toUri();
        return ResponseEntity.created(location).body(channelId);
    }

    /**
     * 断开 Shell 通道
     *
     * @param sessionId 会话 Id
     * @param channelId 通道 Id
     */
    @Operation(summary = "断开 Shell 通道", description = "断开 Shell 通道连接并清理")
    @DeleteMapping("/sessions/{sessionId}/shell/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shellDisconnect(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId,
                                @Parameter(description = "通道 Id", required = true) @PathVariable String channelId) {
        sshService.disconnectAndCleanupChannel(sessionId, channelId);
    }

    /**
     * 创建一个 Exec 通道，连接并执行命令
     * Exec 通道为一次性通道，命令执行完就结束
     *
     * @param message 请求消息体, 包括会话 Id 和要执行的命令
     * @return 命令执行结果
     */
    @Operation(summary = "创建 Exec 通道，连接并执行命令", description = "创建一个 Exec 通道连接并执行命令，返回命令执行结果")
    @PostMapping("/sessions/{sessionId}/exec")
    public SshExecResult exec(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId,
                              @Validated @RequestBody SshExecMessage message) {
        String command = message.getCommand();
        return sshService.executeExecCommand(sessionId, command);
    }

    /**
     * 创建 SFTP 通道并上传文件
     *
     * @param file      文件
     * @param sessionId 会话 Id
     * @param remoteDir 远程目录路径
     * @return 无内容响应
     */
    @Operation(summary = "SFTP 上传文件", description = "创建 SFTP 通道并上传文件")
    @PostMapping(value = "/sessions/{sessionId}/sftp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sftpUploadFile(@Parameter(description = "文件", required = true) @RequestParam MultipartFile file,
                               @Parameter(description = "会话 Id", required = true) @PathVariable String sessionId,
                               @Parameter(description = "远程目录路径", required = true) @RequestParam String remoteDir) {
        sshService.uploadFile(sessionId, file, remoteDir);
    }

    /**
     * 创建 SFTP 通道并下载文件
     *
     * @param sessionId  会话 Id
     * @param remotePath 远程文件路径
     * @return 响应流
     */
    @Operation(summary = "SFTP 下载文件", description = "创建 SFTP 通道并下载文件")
    @GetMapping(value = "/sessions/{sessionId}/sftp", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> sftpDownloadFile(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId,
                                                                  @Parameter(description = "远程文件路径", required = true) @RequestParam String remotePath) {
        return sshService.downloadFile(sessionId, remotePath);
    }

    /**
     * 通过 SFTP 把文本内容覆盖写入远程文件
     *
     * <p>专供 code-editor 等"编辑后保存"场景使用，替代以前在前端拼 {@code cat <<EOF > path} 走 Exec 通道的写法，
     * 杜绝路径与内容被 shell 解释带来的命令注入面。
     *
     * @param sessionId 会话 Id
     * @param message   含远程文件路径与文本内容
     */
    @Operation(summary = "SFTP 写文件", description = "通过 SFTP 把文本内容覆盖写入指定远程文件")
    @PostMapping(value = "/sessions/{sessionId}/file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sftpWriteFile(@Parameter(description = "会话 Id", required = true) @PathVariable String sessionId,
                              @Validated @RequestBody SshWriteFileMessage message) {
        sshService.writeRemoteFile(sessionId, message.getRemotePath(), message.getContent());
    }

}
