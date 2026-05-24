package com.xiaozhanke.deploy.core.websocket;

import com.xiaozhanke.deploy.enums.FileOperationEnum;
import com.jcraft.jsch.SftpProgressMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * @author xiaozhanke
 */
@Slf4j
public class WebSocketSftpProgressMonitor implements SftpProgressMonitor {

    private final String sessionId;

    private final long fileSize;

    private final SimpMessagingTemplate messagingTemplate;

    private final FileOperationEnum operationType;

    private long transferred;

    public WebSocketSftpProgressMonitor(String sessionId, long fileSize, SimpMessagingTemplate messagingTemplate, FileOperationEnum operationType) {
        this.sessionId = sessionId;
        this.fileSize = fileSize;
        this.messagingTemplate = messagingTemplate;
        this.operationType = operationType;
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        this.transferred = 0;
        log.info("会话 [{}] SFTP 开始文件{}传输: {} -> {}, 总大小: {} 字节", sessionId, operationType.getDescription(), src, dest, fileSize);
    }

    @Override
    public boolean count(long count) {
        transferred += count;
        int progress = (int) ((transferred * 100) / fileSize);
        messagingTemplate.convertAndSend(String.format("/topic/ssh/sessions/%s/sftp/%s", this.sessionId, operationType.name().toLowerCase()), progress);
        return true;
    }

    @Override
    public void end() {
        log.info("会话 [{}] SFTP 文件{}传输完成, 总共传输: {} 字节", sessionId, operationType.getDescription(), transferred);
    }
}
