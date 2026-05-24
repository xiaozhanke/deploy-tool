package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.constant.SshConstants;
import com.xiaozhanke.deploy.core.ssh.CommandResultCallback;
import com.xiaozhanke.deploy.core.ssh.ShellCommandTask;
import com.xiaozhanke.deploy.core.websocket.WebSocketSftpProgressMonitor;
import com.xiaozhanke.deploy.enums.FileOperationEnum;
import com.xiaozhanke.deploy.enums.SshAuthTypeEnum;
import com.xiaozhanke.deploy.exception.BusinessException;
import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.dto.SshExecResult;
import com.xiaozhanke.deploy.model.request.ServerParams;
import com.xiaozhanke.deploy.util.PathSafetyUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SSH 服务类
 * 提供 SSH 连接、命令执行和文件传输功能, 管理 SSH 会话和通道, 允许并发操作
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class SshService {
    // 存储会话的Map, key为sessionId, value为JSch Session对象
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    // 存储通道的Map, key为ChannelKey(包含sessionId和channelId), value为Channel对象
    private final Map<ChannelKey, Channel> channels = new ConcurrentHashMap<>();
    // 存储命令队列的Map, key为ChannelKey(包含sessionId和channelId), value为待执行的命令队列
    private final Map<ChannelKey, Queue<ShellCommandTask>> commandQueues = new ConcurrentHashMap<>();
    // 存储输出缓冲区的Map, key为ChannelKey(包含sessionId和channelId), value为命令输出内容
    private final Map<ChannelKey, StringBuilder> outputBuffers = new ConcurrentHashMap<>();
    // 用于会话映射修改（连接/断开）的锁
    private final ReentrantLock sessionLock = new ReentrantLock();
    // WebSocket消息模板, 用于向前端推送消息
    private final SimpMessagingTemplate messagingTemplate;

    public SshService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 建立 SSH 连接到服务器
     *
     * @param server 连接详情
     * @return 建立连接的唯一会话 Id
     * @throws BusinessException 如果连接失败
     */
    public String connect(ServerRecordDto server) {
        sessionLock.lock();
        try {
            log.info("开始 SSH 连接到服务器 {}@{} -p {}", server.getUsername(), server.getHost(), server.getPort());
            Session session = createSession(server);
            int connectTimeout = server.getConnectionTimeout() != null ? server.getConnectionTimeout() : SshConstants.DEFAULT_CONNECT_TIMEOUT;
            session.connect(connectTimeout);
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, session);
            log.info("SSH 连接成功, 连接会话 Id: {}", sessionId);
            return sessionId;
        } catch (JSchException e) {
            String errorMessage = String.format("SSH 连接到服务器 %s@%s -p%s 失败: %s", server.getUsername(), server.getHost(), server.getPort(), e.getMessage());
            throw new BusinessException(errorMessage, e);
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * 测试 SSH 连接详情是否有效, 不建立持久会话
     *
     * @param params 连接详情
     * @return 如果连接测试成功则返回 true
     * @throws BusinessException 如果连接测试失败, 包含详细信息
     */
    public boolean testConnection(ServerParams params) {
        Session testSession = null;
        try {
            log.info("开始测试 SSH 连接到服务器 {}@{} -p {}", params.getUsername(), params.getHost(), params.getPort());
            ServerRecordDto serverRecordDto = new ServerRecordDto();
            BeanUtils.copyProperties(params, serverRecordDto);
            testSession = createSession(serverRecordDto);
            int connectTimeout = params.getConnectionTimeout() != null ? params.getConnectionTimeout() : SshConstants.DEFAULT_CONNECT_TIMEOUT;
            testSession.connect(connectTimeout);
            log.info("测试 SSH 连接成功, 连接到服务器 {}@{} -p {}", params.getUsername(), params.getHost(), params.getPort());
            return true;
        } catch (JSchException e) {
            String errorMessage = String.format("测试 SSH 连接失败: %s", e.getMessage());
            throw new BusinessException(errorMessage, e);
        } finally {
            if (testSession != null && testSession.isConnected()) {
                testSession.disconnect();
                log.debug("测试连接会话已断开");
            }
        }
    }

    /**
     * 关闭 SSH 连接以及所有关联的通道
     *
     * @param sessionId 要关闭的会话 Id
     */
    public void disconnect(String sessionId) {
        sessionLock.lock();
        try {
            Session session = sessions.remove(sessionId);
            if (session != null) {
                log.info("开始关闭 SSH 连接会话 [{}]", sessionId);
                // 先把 keySet 拷贝到 ArrayList 做快照——ConcurrentHashMap 的 keySet 视图弱一致，
                // 边遍历边 channels.remove 可能漏掉新插入的条目，快照后处理可避免
                List<ChannelKey> keysToRemove = new ArrayList<>();
                new ArrayList<>(channels.keySet()).forEach(key -> {
                    if (key.sessionId.equals(sessionId)) {
                        keysToRemove.add(key);
                    }
                });

                // 断开并清理与此会话关联的每个通道
                log.debug("会话 [{}] 准备断开 {} 个关联通道", sessionId, keysToRemove.size());
                keysToRemove.forEach(key -> disconnectAndCleanupChannel(key.sessionId, key.channelId));

                // 断开会话本身
                if (session.isConnected()) {
                    session.disconnect();
                    log.debug("JSch Session 对象已断开连接 ");
                }
                log.info("成功关闭 SSH 连接会话 [{}]", sessionId);
            } else {
                log.debug("尝试关闭不存在或已关闭的会话 [{}]", sessionId);
            }
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * 断开指定通道并清理其关联资源
     *
     * @param sessionId 会话 Id
     * @param channelId 要断开的通道 Id
     */
    public void disconnectAndCleanupChannel(String sessionId, String channelId) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        log.debug("尝试断开并清理会话 [{}] 的通道 [{}]", sessionId, channelId);

        // 移除并断开通道本身
        Channel channel = channels.remove(key);
        if (channel != null) {
            if (channel.isConnected()) {
                channel.disconnect();
                log.info("已断开会话 [{}] 的通道 [{}]", sessionId, channelId);
            } else {
                log.debug("通道 [{}] 在尝试断开时已不处于连接状态 ", channelId);
            }
        } else {
            log.debug("尝试清理时在 Map 中未找到会话 [{}] 的通道 [{}]", sessionId, channelId);
        }

        // 清理关联资源（队列、缓冲区）
        cleanupChannelResources(sessionId, channelId);
    }

    /**
     * 为给定会话打开一个新的交互式 Shell 通道
     * 启动后台线程读取输出并处理命令执行
     *
     * @param sessionId 会话 Id
     * @return 新 Shell 通道的唯一通道 Id
     * @throws BusinessException 如果会话无效或通道创建失败
     */
    public String addShellChannel(String sessionId) {
        log.info("请求为会话 [{}] 创建 Shell 通道", sessionId);
        Session session = getSession(sessionId);

        try {
            ChannelShell channel = (ChannelShell) session.openChannel(SshConstants.ChannelType.SHELL);
            // 请求伪终端, 对交互式Shell很重要
            channel.setPty(true);
            // 设置终端尺寸, 避免命令过长自动添加换行符
            channel.setPtySize(400, 80, 960, 720);

            String channelId = String.valueOf(channel.getId());
            ChannelKey key = new ChannelKey(sessionId, channelId);

            log.debug("会话 [{}] Shell 通道已创建, 准备存储和连接通道 [{}]", sessionId, channelId);

            // 先存储引用, 再连接
            channels.put(key, channel);
            outputBuffers.put(key, new StringBuilder());
            commandQueues.put(key, new ConcurrentLinkedQueue<>());

            log.debug("通道 [{}] 的元数据已存入 Map", channelId);

            // 连接通道
            int channelConnectTimeout = SshConstants.DEFAULT_CHANNEL_CONNECT_TIMEOUT;
            channel.connect(channelConnectTimeout);
            log.info("会话 [{}] Shell 通道 [{}] 连接成功", sessionId, channelId);

            // 启动输出读取线程
            startOutputReader(sessionId, channelId, channel);
            return channelId;
        } catch (JSchException e) {
            // 尝试清理可能部分创建的资源
            // 如果 openChannel 成功但 connect 失败, channels Map 中可能已有条目
            channels.entrySet().removeIf(entry -> entry.getKey().sessionId.equals(sessionId) && !entry.getValue().isConnected());
            // 清理关联的队列和缓冲区, 即使通道 Id 可能不准确
            cleanupChannelResources(sessionId, String.valueOf(channels.size() + 1));
            String errorMessage = String.format("为会话 [%s] 创建或连接 Shell 通道失败: %s", sessionId, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } catch (Exception e) {
            // 捕获其他潜在异常
            String errorMessage = String.format("为会话 [%s] 创建或连接 Shell 通道失败: %s", sessionId, e.getMessage());
            throw new BusinessException(errorMessage, e);
        }
    }

    /**
     * 创建 Exec 通道并执行命令
     * 结果直接返回
     *
     * @param sessionId 会话 Id
     * @param command   要执行的命令
     * @return 包含命令执行状态码和输出的 ExecResult 对象
     * @throws BusinessException 如果发生 SSH 或 IO 异常
     */
    public SshExecResult executeExecCommand(String sessionId, String command) {
        log.info("请求在会话 [{}] 上创建 Exec 通道并执行命令: '{}'", sessionId, command);
        // 获取会话对象
        Session session = getSession(sessionId);
        ChannelExec channel = null;
        int exitStatus;
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();
        try {
            // 创建 Exec 通道
            channel = (ChannelExec) session.openChannel(SshConstants.ChannelType.EXEC);
            log.debug("会话 [{}] Exec 通道 [{}] 创建成功, 准备设置命令: '{}'", sessionId, channel.getId(), command);
            channel.setCommand(command);

            // 获取标准输出和错误输出的流
            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getErrStream();

            // 连接通道以启动命令
            channel.connect();
            log.debug("会话 [{}] Exec 通道 [{}] 连接成功, 命令正在执行...", sessionId, channel.getId());

            try (InputStreamReader reader = new InputStreamReader(stdout, StandardCharsets.UTF_8); BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    outputBuffer.append(line).append(System.lineSeparator());
                }
                log.debug("会话 [{}] Exec 通道 [{}] 标准输出读取完成:\n{}", sessionId, channel.getId(), outputBuffer);
            }

            try (InputStreamReader reader = new InputStreamReader(stderr, StandardCharsets.UTF_8); BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    errorBuffer.append(line).append(System.lineSeparator());
                }
                log.debug("会话 [{}] Exec 通道 [{}] 错误输出读取完成:\n{}", sessionId, channel.getId(), errorBuffer);
            }

            // 获取命令的退出状态码
            exitStatus = channel.getExitStatus();
            log.info("会话 [{}] Exec 通道 [{}] 命令 '{}' 执行完成, 退出状态码: {}", sessionId, channel.getId(), command, exitStatus);
        } catch (JSchException e) {
            String errorMessage = String.format("会话 [%s] 创建或连接 '%s' 命令的 Exec 通道时发生异常: %s", sessionId, command, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } catch (IOException e) {
            String errorMessage = String.format("会话 [%s] Exec 通道读取命令 '%s' 输出时发生异常: %s", sessionId, command, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } finally {
            if (channel != null) {
                channel.disconnect();
                log.debug("会话 [{}] Exec 通道 [{}] 已断开", sessionId, channel.getId());
            }
        }
        // 返回命令执行结果，包括标准输出和错误输出
        return new SshExecResult(exitStatus, outputBuffer.append(System.lineSeparator()).append(errorBuffer).toString());
    }

    /**
     * 在现有的 Shell 通道上执行命令
     * 命令被添加到该通道的队列中并顺序执行
     * 结果通过回调报告
     *
     * @param sessionId 会话 Id
     * @param channelId 通道 Id
     * @param command   要执行的命令
     * @param callback  处理命令成功或失败的回调
     * @throws BusinessException 如果会话或通道无效
     */
    public void executeShellCommand(String sessionId, String channelId, String taskId, String command, CommandResultCallback callback) {
        log.info("请求在会话[{}] 通道 [{}] 上执行命令: '{}'", sessionId, channelId, command);
        // 检查会话存在且连接
        getSession(sessionId);
        // 检查通道存在、连接且类型正确
        ChannelShell channel = getShellChannel(sessionId, channelId);

        log.debug("会话 [{}] 和 通道 [{}] 状态检查通过", sessionId, channelId);

        // 获取命令队列
        ChannelKey key = new ChannelKey(sessionId, channelId);
        Queue<ShellCommandTask> queue = commandQueues.get(key);
        if (queue == null) {
            String errorMessage = String.format("会话 [%s] 通道 [%s] 的命令执行队列丢失", sessionId, channelId);
            callback.onFailure(sessionId, channelId, errorMessage, -1);
            throw new BusinessException(errorMessage);
        }

        // 创建并添加任务到队列
        ShellCommandTask task = new ShellCommandTask(taskId, command, callback);
        log.debug("为会话 [{}] 通道 [{}] 创建命令任务 [{}]", sessionId, channelId, task.getTaskId());
        boolean added = queue.offer(task);
        if (!added) {
            String errorMessage = String.format("无法将命令任务 [%s] 添加到会话 [%s] 通道[%s] 的命令执行队列中 (队列可能已满或异常)", task.getTaskId(), sessionId, channelId);
            callback.onFailure(sessionId, channelId, errorMessage, -1);
            throw new BusinessException(errorMessage);
        }

        log.info("命令任务 [{}] 已添加到 会话 [{}] 通道[{}] 的命令执行队列中, 当前队列大小: {}", task.getTaskId(), sessionId, channelId, queue.size());

        // 尝试处理队列中的下一个命令
        processNextCommand(sessionId, channelId, channel);
    }

    /**
     * 上传本地文件到远程服务器使用 SFTP
     *
     * @param sessionId 会话 Id
     * @param localPath 本地文件路径
     * @param remoteDir 远程目标目录路径
     * @throws BusinessException 如果会话无效、文件未找到或 SFTP 操作失败
     */
    public void uploadFile(String sessionId, String localPath, String remoteDir) {
        File localFile = validateLocalFile(localPath);
        String originalFilename = localFile.getName();
        PathSafetyUtils.assertSafeFileName(originalFilename);
        PathSafetyUtils.assertNoTraversalSegments(remoteDir);

        String operationDesc = String.format("上传文件 '%s' -> '%s'", localPath, PathSafetyUtils.safeJoin(remoteDir, originalFilename));
        executeSftpOperation(sessionId, operationDesc, channel -> {
            String targetDir = prepareRemoteDirectory(channel, remoteDir);
            String remoteFilePath = PathSafetyUtils.safeJoin(targetDir, originalFilename);

            try (InputStream in = new FileInputStream(localFile)) {
                channel.put(in, remoteFilePath,
                        new WebSocketSftpProgressMonitor(sessionId, localFile.length(), messagingTemplate, FileOperationEnum.UPLOAD),
                        ChannelSftp.OVERWRITE);
            }
        });
    }

    /**
     * 上传文件
     *
     * @param sessionId 会话 Id
     * @param file      文件
     * @param remoteDir 远程目标目录路径
     */
    public void uploadFile(String sessionId, MultipartFile file, String remoteDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        PathSafetyUtils.assertSafeFileName(originalFilename);
        PathSafetyUtils.assertNoTraversalSegments(remoteDir);

        String operationDesc = String.format("上传文件 '%s' -> '%s'", originalFilename, PathSafetyUtils.safeJoin(remoteDir, originalFilename));
        executeSftpOperation(sessionId, operationDesc, channel -> {
            String targetDir = prepareRemoteDirectory(channel, remoteDir);
            String remoteFilePath = PathSafetyUtils.safeJoin(targetDir, originalFilename);

            try (InputStream in = file.getInputStream()) {
                channel.put(in, remoteFilePath, new WebSocketSftpProgressMonitor(sessionId, file.getSize(), messagingTemplate, FileOperationEnum.UPLOAD), ChannelSftp.OVERWRITE);
            }
        });
    }

    /**
     * 使用 SFTP 直接从远程服务器下载文件并用响应流返回
     *
     * @param sessionId  会话 Id
     * @param remotePath 远程文件路径
     * @return 响应流
     */
    public ResponseEntity<StreamingResponseBody> downloadFile(String sessionId, String remotePath) {
        String remoteFileName = Paths.get(remotePath).getFileName().toString();

        String operationDesc = String.format("下载文件 '%s' -> 响应流", remotePath);

        StreamingResponseBody body = outputStream -> executeSftpOperation(sessionId, operationDesc, channel -> {
            try (InputStream in = channel.get(remotePath)) {
                byte[] buffer = new byte[SshConstants.BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        });

        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(remoteFileName, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(body);
    }

    /**
     * 从远程服务器下载文件到本地机器使用 SFTP
     *
     * @param sessionId  会话 Id
     * @param remotePath 远程文件路径
     * @param localDir   本地目录路径, 文件将保存在此目录下
     * @throws BusinessException 如果会话无效、本地目录无效或 SFTP 操作失败
     */
    public void downloadFile(String sessionId, String remotePath, String localDir) {
        String remoteFileName = Paths.get(remotePath).getFileName().toString();
        File localDirectory = prepareLocalDirectory(localDir);
        File localFile = new File(localDirectory, remoteFileName);

        String operationDesc = String.format("下载文件 '%s' -> '%s'", remotePath, localFile.getAbsolutePath());

        executeSftpOperation(sessionId, operationDesc, channel -> {
            long fileSize = getRemoteFileSize(channel, remotePath);

            try (OutputStream out = new FileOutputStream(localFile)) {
                channel.get(remotePath, out, new WebSocketSftpProgressMonitor(sessionId, fileSize, messagingTemplate, FileOperationEnum.DOWNLOAD));
            }
        });
    }

    /**
     * 创建并配置 JSch Session 对象
     *
     * @param server 连接详情
     * @return 配置好的 Session 对象
     * @throws JSchException 如果会话创建或配置失败
     */
    private Session createSession(ServerRecordDto server) throws JSchException {
        log.debug("创建 JSch 实例并配置认证, 用户: {}", server.getUsername());
        JSch jsch = new JSch();
        // 设置认证方式
        setupAuth(jsch, server);

        log.debug("获取 JSch 会话实例, 服务器: {}@{} -p {}", server.getUsername(), server.getHost(), server.getPort());
        Session session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());

        // 如果是密码认证, 设置密码（用 byte[] 重载，避免 JSch 内部把 String 留在 String pool）
        if (server.getAuthType() == SshAuthTypeEnum.PASSWORD && server.getPassword() != null) {
            session.setPassword(server.getPassword().getBytes(StandardCharsets.UTF_8));
            log.debug("已设置会话密码 (密码本身不记录日志)");
        }

        // 应用其他会话配置
        applySessionConfig(session, server);

        session.setServerAliveCountMax(SshConstants.DEFAULT_SERVER_ALIVE_COUNT_MAX);
        session.setServerAliveInterval(SshConstants.DEFAULT_SERVER_ALIVE_INTERVAL);
        return session;
    }

    /**
     * 设置认证方式
     *
     * @param jsch   JSch 对象
     * @param server 服务器连接信息
     * @throws JSchException 认证设置失败时抛出
     */
    private void setupAuth(JSch jsch, ServerRecordDto server) throws JSchException {
        SshAuthTypeEnum authType = server.getAuthType();
        log.debug("配置认证类型: {}", authType);

        if (authType == SshAuthTypeEnum.KEY || authType == SshAuthTypeEnum.KEY_WITH_PASS) {
            String privateKeyPath = server.getPrivateKeyPath();
            if (!StringUtils.hasText(privateKeyPath)) {
                throw new BusinessException("私钥认证需要提供私钥文件路径");
            }
            File privateKeyFile = new File(privateKeyPath);
            if (!privateKeyFile.exists() || !privateKeyFile.isFile()) {
                throw new BusinessException("指定的私钥文件不存在或不是一个有效文件: " + privateKeyPath);
            }

            String passphrase = (authType == SshAuthTypeEnum.KEY_WITH_PASS) ? server.getPrivateKeyPassword() : null;
            try {
                // 用 byte[] 重载，避免 passphrase 以 String 形式被 JSch 内部缓存
                byte[] passphraseBytes = passphrase == null ? null : passphrase.getBytes(StandardCharsets.UTF_8);
                jsch.addIdentity(privateKeyPath, null, passphraseBytes);
                log.info("已添加私钥身份: {}", privateKeyPath);
            } catch (JSchException e) {
                log.error("添加私钥身份失败 '{}': {}", privateKeyPath, e.getMessage());
                if (e.getMessage().toLowerCase().contains("passphrase")) {
                    throw new JSchException("私钥密码错误或需要密码但未提供: " + privateKeyPath, e);
                }
                throw e;
            }
        } else if (authType == null || authType == SshAuthTypeEnum.PASSWORD) {
            // 密码认证由 session.setPassword() 处理
            log.debug("使用密码认证方式");
        } else {
            throw new BusinessException("不支持的认证类型: " + authType);
        }
    }

    /**
     * 应用 Session 配置
     *
     * @param session Session 对象
     * @param server  服务器连接信息
     */
    private void applySessionConfig(Session session, ServerRecordDto server) throws JSchException {
        log.debug("开始应用会话配置");
        // 设置加密算法等配置
        setIfNotNull(session, "kex", server.getKexAlgorithms());
        setIfNotNull(session, "cipher.s2c", server.getCipherAlgorithms());
        setIfNotNull(session, "cipher.c2s", server.getCipherAlgorithms());
        setIfNotNull(session, "mac.s2c", server.getMacAlgorithms());
        setIfNotNull(session, "mac.c2s", server.getMacAlgorithms());
        setIfNotNull(session, "server_host_key", server.getServerHostKeyAlgorithms());

        // 设置连接超时
        if (server.getConnectionTimeout() != null) {
            session.setTimeout(server.getConnectionTimeout());
        }

        // 设置布尔型配置项
        setBooleanConfig(session, "compression", server.getCompressionEnabled(), "zlib", "none");
        setBooleanConfig(session, "StrictHostKeyChecking", server.getStrictHostKeyChecking(), "yes", "no");
        setBooleanConfig(session, "X11Forwarding", server.getX11ForwardingEnabled(), "yes", "no");
        setBooleanConfig(session, "AllowTcpForwarding", server.getPortForwardingEnabled(), "yes", "no");
        log.debug("会话配置应用完毕");
    }

    /**
     * 设置 Session 配置项(非空时设置)
     *
     * @param session Session 对象
     * @param key     配置键
     * @param value   配置值
     */
    private void setIfNotNull(Session session, String key, String value) {
        if (StringUtils.hasText(value)) {
            session.setConfig(key, value);
            log.debug("设置 Session 配置项: {} = {}", key, value);
        }
    }

    /**
     * 设置布尔型 Session 配置项
     *
     * @param session    Session 对象
     * @param key        配置键
     * @param value      布尔值
     * @param trueValue  为 true 时的配置值
     * @param falseValue 为 false 时的配置值
     */
    private void setBooleanConfig(Session session, String key, Boolean value, String trueValue, String falseValue) {
        if (value != null) {
            session.setConfig(key, value ? trueValue : falseValue);
            log.debug("设置布尔型 Session 配置项: {} = {}", key, value ? trueValue : falseValue);
        }
    }

    /**
     * 获取给定 Id 的 Session 对象, 如果未找到或未连接则抛出异常。
     *
     * <p>"获取 + 失效就移除"必须原子化：之前两步分离时，两个线程可能同时观察到 disconnected
     * 然后各自尝试 sessions.remove，仅一次 disconnect/cleanup，另一次直接看到 null 也按已断开处理，
     * 但中间窗口内 channel 仍可能被新请求拿到去使用已死会话。这里用 sessionLock 同步整个判断 + 移除。
     */
    private Session getSession(String sessionId) {
        sessionLock.lock();
        try {
            Session session = sessions.get(sessionId);
            if (session == null) {
                throw new BusinessException(String.format("会话 [%s] 不存在", sessionId));
            }
            if (!session.isConnected()) {
                // 同步窗口内移除过期会话，避免两个线程都尝试清理或并发写入
                sessions.remove(sessionId);
                throw new BusinessException(String.format("会话 [%s] 连接未建立或已断开", sessionId));
            }
            return session;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * 获取给定 Id 的 Channel 对象, 如果未找到、未连接或类型不为 ChannelShell 则抛出异常
     */
    private ChannelShell getShellChannel(String sessionId, String channelId) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        Channel channel = channels.get(key);
        if (channel == null) {
            String errorMessage = String.format("会话 [%s] 的通道 [%s] 不存在", sessionId, channelId);
            throw new BusinessException(errorMessage);
        }
        if (!(channel instanceof ChannelShell)) {
            String errorMessage = String.format("通道 [%s] 为 %s 通道, 需要 Shell 通道", channelId, channel.getClass().getName());
            throw new BusinessException(errorMessage);
        }
        if (!channel.isConnected() || channel.isClosed()) {
            // 主动清理此断开通道的资源
            disconnectAndCleanupChannel(sessionId, channelId);
            String errorMessage = String.format("会话 [%s] 的通道 [%s] 已断开或关闭", sessionId, channelId);
            throw new BusinessException(errorMessage);
        }
        return (ChannelShell) channel;
    }

    /**
     * 处理指定通道命令执行队列中的下一个待执行命令
     *
     * @param sessionId 会话ID
     * @param channelId 通道ID
     * @param channel   对应的 ChannelShell 实例
     */
    private void processNextCommand(String sessionId, String channelId, ChannelShell channel) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        Queue<ShellCommandTask> queue = commandQueues.get(key);

        if (queue == null || queue.isEmpty()) {
            log.debug("尝试处理命令执行队列, 但会话 [{}] 通道 [{}] 命令执行队列已不存在: ", sessionId, channelId);
            return;
        }
        // 对队列的访问和修改加锁, 防止并发问题
        synchronized (queue) {
            ShellCommandTask currentTask = queue.peek();

            if (currentTask == null) {
                log.debug("会话 [{}] 通道 [{}] 命令执行队列为空, 无需处理", sessionId, channelId);
                return;
            }

            log.debug("会话 [{}] 通道 [{}] 检查队首任务 [{}] 状态: commandSent={}, completed={}", sessionId, channelId, currentTask.getTaskId(), currentTask.isCommandSent(), currentTask.isCompleted());

            // 如果任务已完成或已发送, 则不处理（等待结果或下一个任务）
            // 注意：这里有一个潜在问题, 如果一个任务发送失败, 它可能既不是 completed 也不是 commandSent=true
            // 我们需要在发送失败时显式地处理任务（移除或标记失败）
            if (currentTask.isCompleted()) {
                log.debug("会话 [{}] 通道 [{}] 队首任务 [{}] 已完成, 但仍在队列中, 将移除", sessionId, channelId, currentTask.getTaskId());
                // 移除已完成的任务
                queue.poll();
                // 检查下一个
                processNextCommand(sessionId, channelId, channel);
                return;
            }

            if (currentTask.isCommandSent()) {
                log.debug("通道 [{}] 队首任务 [{}] 已发送, 等待结果", channelId, currentTask.getTaskId());
                return;
            }

            // 再次检查通道连接状态, 因为获取锁可能耗时
            if (!channel.isConnected() || channel.isClosed()) {
                log.debug("通道 [{}] 在处理任务 [{}] 时已断开或关闭, 将清空剩余队列并标记失败", channelId, currentTask.getTaskId());
                String errorMessage = String.format("会话 [%s] 的通道 [%s] 已断开或关闭", sessionId, channelId);
                failAllPendingCommands(sessionId, channelId, queue, errorMessage);
                // 队列已清空
                return;
            }

            // 发送命令
            try {
                String commandToSend = currentTask.getFinalCommand();
                log.info("准备向会话 [{}] 通道 [{}] 发送命令: '{}'", sessionId, channelId, commandToSend);

                OutputStream outputStream = channel.getOutputStream();
                if (outputStream == null) {
                    log.error("会话 [{}] 通道 [{}] 的输出流为 null, 无法发送命令: '{}'", sessionId, channelId, commandToSend);
                    failAndRemoveTask(sessionId, channelId, queue, currentTask, "无法获取通道输出流");
                    // 尝试下一个
                    processNextCommand(sessionId, channelId, channel);
                    return;
                }
                outputStream.write(commandToSend.getBytes(StandardCharsets.UTF_8));
                // flush 非常重要, 确保数据被发送
                outputStream.flush();

                // 标记命令已发送
                currentTask.setCommandSent(true);
                log.info("已成功向 会话 [{}] 通道 [{}] 发送命令: '{}'", sessionId, channelId, commandToSend);
            } catch (IOException e) {
                log.error("向会话 [{}] 通道 [{}] 发送命令 '{}' 时发生 IO 异常: {}", sessionId, channelId, currentTask.getFinalCommand(), e.getMessage(), e);
                failAndRemoveTask(sessionId, channelId, queue, currentTask, "发送命令失败: " + e.getMessage());
                // 尝试处理队列中的下一个命令
                processNextCommand(sessionId, channelId, channel);
            } catch (Exception e) {
                log.error("向会话 [{}] 通道 [{}] 发送命令 '{}' 时发生意外异常: {}", sessionId, channelId, currentTask.getFinalCommand(), e.getMessage(), e);
                failAndRemoveTask(sessionId, channelId, queue, currentTask, "发送命令时发生意外错误: " + e.getMessage());
                // 尝试处理队列中的下一个命令
                processNextCommand(sessionId, channelId, channel);
            }
        }
    }

    /**
     * 辅助方法：标记任务失败并从队列移除
     */
    private void failAndRemoveTask(String sessionId, String channelId, Queue<ShellCommandTask> queue, ShellCommandTask task, String errorMessage) {
        // 移除任务
        queue.poll();
        try {
            task.getCallback().onFailure(sessionId, channelId, errorMessage, -1);
        } catch (Exception e) {
            log.error("任务 [{}] 调用失败回调时出错, 错误: {}", task.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：标记队列中所有待处理任务为失败
     */
    private void failAllPendingCommands(String sessionId, String channelId, Queue<ShellCommandTask> queue, String errorMessage) {
        ShellCommandTask taskToFail;
        while ((taskToFail = queue.poll()) != null) {
            log.debug("标记任务 [{}] 为失败 ({})", taskToFail.getTaskId(), errorMessage);
            try {
                taskToFail.getCallback().onFailure(sessionId, channelId, errorMessage, -1);
            } catch (Exception e) {
                log.error("任务 [{}] 调用失败回调时出错 (清空队列时), 错误: {}", taskToFail.getTaskId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 启动后台线程持续读取 Shell 通道的输出流
     * 检测命令完成标记并处理结果
     *
     * @param sessionId 会话 Id
     * @param channelId 通道 Id
     * @param channel   ChannelShell 实例
     */
    private void startOutputReader(String sessionId, String channelId, ChannelShell channel) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        Thread readerThread = new Thread(() -> {
            log.info("输出读取线程已启动: 会话 [{}] 通道 [{}]", sessionId, channelId);
            StringBuilder output = outputBuffers.get(key);
            if (output == null) {
                log.error("输出缓冲区丢失: 会话 [{}] 通道 [{}] 读取线程无法启动 ", sessionId, channelId);
                // 清理相关资源
                disconnectAndCleanupChannel(sessionId, channelId);
                return;
            }
            try (InputStream in = channel.getInputStream()) {
                byte[] buffer = new byte[SshConstants.BUFFER_SIZE];
                int bytesRead;
                // 持续读取命令输出
                while (channel.isConnected() && (bytesRead = in.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    log.info("会话 [{}] 通道 [{}] 收到数据块 ({} 字节): {}", sessionId, channelId, bytesRead, chunk);
                    // 追加到特定于通道的缓冲区
                    output.append(chunk);
                    // 通过 WebSocket 推送输出内容
                    String cleanedChunk = chunk.replaceAll(SshConstants.EXIT_CODE_AND_COMMAND_REGEX, "");
                    log.info("会话 [{}] 通道 [{}] 往 WebSocket 订阅频道推送终端输出内容: {}", sessionId, channelId, cleanedChunk);
                    messagingTemplate.convertAndSend(String.format("/topic/ssh/sessions/%s/shell/%s", sessionId, channelId), cleanedChunk);

                    // 检测到命令结束标志时处理结果
                    Matcher matcher = Pattern.compile(SshConstants.EXIT_CODE_RESULT_REGEX).matcher(output);
                    if (matcher.find()) {
                        processCommandResult(sessionId, channelId, channel, output.toString());
                        output.setLength(0);
                    }
                }
                // read 返回 -1, 表示流已结束 (通常是通道关闭)
                log.info("会话 [{}] 通道 [{}] 输入流已到达末尾 ", sessionId, channelId);
            } catch (IOException e) {
                if (channel.isConnected()) {
                    log.error("读取 Shell 通道时发生 IO 异常: 会话 [{}] 通道 [{}]: {}", sessionId, channelId, e.getMessage(), e);
                } else {
                    log.info("会话 [{}] 通道 [{}] 已断开, 读取线程正常结束 ", sessionId, channelId);
                }
            } catch (Exception e) {
                log.error("会话 [{}] 通道 [{}] 输出读取线程发生意外错误: {}", sessionId, channelId, e.getMessage(), e);
            } finally {
                log.info("会话 [{}] 通道 [{}] 输出读取线程结束, 开始清理通道资源", sessionId, channelId);
                // 确保无论如何都清理通道资源
                disconnectAndCleanupChannel(sessionId, channelId);
            }
        }, "SSH-OutputReader-[" + sessionId + "]-[" + channelId + "]");

        // 设置为守护线程, 允许 JVM 在主线程结束后退出
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * 处理已完成命令的收集到的输出
     * 提取退出码, 通知回调, 并触发下一个命令
     *
     * @param sessionId 会话 Id
     * @param channelId 通道 Id
     * @param channel   ChannelShell 实例
     * @param output    包含结束标记的命令输出
     */
    private void processCommandResult(String sessionId, String channelId, ChannelShell channel, String output) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        Queue<ShellCommandTask> queue = commandQueues.get(key);
        if (queue == null || queue.isEmpty()) {
            log.debug("处理命令结果时命令执行队列已为空: 会话 [{}] 通道 [{}]", sessionId, channelId);
            return;
        }

        synchronized (queue) {
            ShellCommandTask completedTask = queue.peek();
            if (completedTask == null) {
                log.debug("处理命令结果, 但队首任务为 null: 会话 [{}] 通道 [{}] 输出: {}", sessionId, channelId, output);
                return;
            }
            String taskId = completedTask.getTaskId();
            if (!completedTask.isCommandSent()) {
                log.error("处理命令结果, 但队首任务 TaskId: {} 尚未标记为已发送！这可能是一个逻辑错误 会话 [{}] 通道 [{}]", taskId, sessionId, channelId);
                return;
            }
            if (completedTask.isCompleted()) {
                log.debug("处理命令结果, 但队首任务 TaskId: {} 已标记为完成！可能重复检测到标记 会话 [{}] 通道 [{}] 忽略此次处理 ", taskId, sessionId, channelId);
                return;
            }

            // 清理输出内容中的特殊标记
            String cleanedOutput = output.replaceAll(SshConstants.EXIT_CODE_AND_COMMAND_REGEX, "");
            // 解析退出码
            int exitCode = parseExitCode(output);

            log.info("会话 [{}] 通道 [{}] 命令 '{}' 执行完成, 退出码: {}", sessionId, channelId, completedTask.getFinalCommand(), exitCode);
            log.debug("原始输出(带标记): {}", output);
            log.debug("清理后输出: {}", cleanedOutput);

            // 在调用回调和移除任务前标记为完成
            completedTask.setCompleted(true);
            // 从队列头部移除已完成的任务
            queue.poll();

            // 通知回调
            try {
                if (exitCode == 0) {
                    completedTask.getCallback().onSuccess(sessionId, channelId, cleanedOutput);
                } else {
                    completedTask.getCallback().onFailure(sessionId, channelId, cleanedOutput, exitCode);
                }
                log.info("会话 [{}] 通道 [{}] 任务 [{}] 往 WebSocket 订阅频道推送运行结果: {}", sessionId, channelId, taskId, exitCode);
                messagingTemplate.convertAndSend(String.format("/topic/ssh/sessions/%s/shell/%s/task/%s", sessionId, channelId, taskId), exitCode);
                log.info("已调用 会话 [{}] 通道 [{}] 命令 '{}' 的回调函数", sessionId, channelId, completedTask.getFinalCommand());
            } catch (Exception e) {
                log.error("调用 会话 [{}] 通道 [{}] 命令 '{}' 回调函数时出错: {}", sessionId, channelId, completedTask.getFinalCommand(), e.getMessage(), e);
            }
            // 触发队列中的下一个命令的处理
            processNextCommand(sessionId, channelId, channel);
        }
    }

    /**
     * 从输出内容中解析退出码
     *
     * @param output 命令输出内容
     * @return 退出码, 解析失败返回-1
     */
    private int parseExitCode(String output) {
        Matcher matcher = Pattern.compile(SshConstants.EXIT_CODE_RESULT_REGEX).matcher(output);
        if (matcher.find()) {
            try {
                String codeStr = matcher.group(1);
                log.debug("从标记中解析到退出码字符串: {}", codeStr);
                return Integer.parseInt(codeStr);
            } catch (NumberFormatException e) {
                log.error("无法将退出码标记中的 '{}' 解析为整数", matcher.group(1));
                return -1;
            }
        }
        log.debug("在输出中未找到退出码标记 ({})", SshConstants.EXIT_CODE);
        return -1;
    }

    /**
     * 清理与特定通道关联的资源（队列、缓冲区）
     * 当通道关闭或其读取线程终止时应调用此方法
     *
     * @param sessionId 会话 Id
     * @param channelId 需要清理资源的通道 Id
     */
    private void cleanupChannelResources(String sessionId, String channelId) {
        ChannelKey key = new ChannelKey(sessionId, channelId);
        log.debug("开始清理通道资源: 会话 [{}] 通道 [{}]", sessionId, channelId);

        // 移除命令执行队列, 并尝试通知待处理的任务
        Queue<ShellCommandTask> queue = commandQueues.remove(key);
        if (queue != null) {
            log.debug("已移除通道[{}] 的命令执行队列, 队列中剩余 {} 个任务将被标记为失败 ", channelId, queue.size());
            String errorMessage = String.format("会话 [%s] 的通道 [%s] 意外关闭", sessionId, channelId);
            failAllPendingCommands(sessionId, channelId, queue, errorMessage);
        } else {
            log.debug("通道 [{}] 的命令执行队列已不存在, 无需清理", channelId);
        }

        // 移除输出缓冲区
        if (outputBuffers.remove(key) != null) {
            log.debug("已移除通道[{}] 的输出缓冲区", channelId);
        } else {
            log.debug("通道 [{}] 的输出缓冲区已不存在, 无需清理", channelId);
        }
        log.debug("通道资源清理完毕: 会话 [{}] 通道 [{}]", sessionId, channelId);
    }


    /**
     * 在专用的 SFTP 通道上下文中执行给定的 SFTP 操作
     * 处理通道的打开、关闭和错误记录
     *
     * @param sessionId     会话 Id
     * @param operationDesc 操作的描述性文本, 用于日志记录
     * @param operation     SftpOperation 函数式接口的实现
     * @throws BusinessException 如果会话无效或 SFTP 操作失败
     */
    private void executeSftpOperation(String sessionId, String operationDesc, SftpOperation operation) {
        Session session = getSession(sessionId);
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel(SshConstants.ChannelType.SFTP);
            String channelId = String.valueOf(channel.getId());
            log.debug("为 会话 [{}] 打开 SFTP 通道, 通道 Id: {}", sessionId, channelId);
            int channelConnectTimeout = SshConstants.DEFAULT_CHANNEL_CONNECT_TIMEOUT;
            log.debug("会话 [{}] SFTP 通道 [{}] 设置连接超时时间: {} ms", sessionId, channelId, channelConnectTimeout);
            channel.connect(channelConnectTimeout);
            log.debug("会话 [{}] SFTP 通道 [{}] 连接成功, 执行操作: {}", sessionId, channelId, operationDesc);
            // 执行具体的文件传输操作
            operation.execute(channel);
            log.debug("会话 [{}] SFTP 通道 [{}] 操作成功: {}", sessionId, channelId, operationDesc);
        } catch (JSchException e) {
            String errorMessage = String.format("会话 [%s] 打开或连接 SFTP 通道失败: %s", sessionId, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } catch (SftpException e) {
            String errorMessage = String.format("会话 [%s] SFTP 操作 '%s' 失败: %s", sessionId, operationDesc, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } catch (IOException e) {
            // 主要来自文件流操作
            String errorMessage = String.format("会话 [%s] SFTP 操作 '%s' 期间发生 IO 错误: %s", sessionId, operationDesc, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } catch (Exception e) {
            // 捕获 SftpOperation.execute(channel) 中可能抛出的其他异常
            String errorMessage = String.format("会话 [%s] SFTP 操作 '%s' 期间发生意外错误: %s", sessionId, operationDesc, e.getMessage());
            throw new BusinessException(errorMessage, e);
        } finally {
            if (channel != null) {
                if (channel.isConnected()) {
                    channel.disconnect();
                }
                log.debug("会话 [{}] SFTP 通道 [{}] 已断开", sessionId, channel.getId());
            }
        }
    }

    /**
     * SFTP操作函数式接口
     */
    @FunctionalInterface
    private interface SftpOperation {
        /**
         * 在提供的 SFTP 通道上执行操作
         *
         * @param channel 已连接的 ChannelSftp 实例
         * @throws Exception 如果操作失败
         */
        void execute(ChannelSftp channel) throws Exception;
    }

    /**
     * 通道键对象, 用于标识唯一的通道
     */
    @EqualsAndHashCode
    private static class ChannelKey {
        private final String sessionId;
        private final String channelId;

        private ChannelKey(String sessionId, String channelId) {
            this.sessionId = sessionId;
            this.channelId = channelId;
        }
    }

    /**
     * 校验本地文件
     *
     * @param localPath 本地文件路径
     * @return 本地文件
     */
    private File validateLocalFile(String localPath) {
        File localFile = new File(localPath);
        if (!localFile.exists() || !localFile.isFile()) {
            throw new BusinessException("本地文件不存在或不是有效文件: " + localPath);
        }
        if (!localFile.canRead()) {
            throw new BusinessException("本地文件不可读: " + localPath);
        }
        return localFile;
    }

    /**
     * 准备远程目录
     *
     * @param channel   SFTP 通道
     * @param remoteDir 远程目录路径
     * @return 远程目标目录路径
     * @throws SftpException 其他 SFTP 异常
     */
    private String prepareRemoteDirectory(ChannelSftp channel, String remoteDir) throws SftpException {
        // 先校验整条路径不含 .. 段，否则按段 mkdir 时会被 SFTP 服务端解析跳出目标目录
        PathSafetyUtils.assertNoTraversalSegments(remoteDir);

        // 确保 remoteDir 不以 / 结尾
        String targetDir = remoteDir.endsWith("/") ? remoteDir.substring(0, remoteDir.length() - 1) : remoteDir;

        // 检查远程目录是否存在，不存在则创建
        String[] directories = targetDir.split("/");
        String path = "";
        for (String dir : directories) {
            if (dir.isEmpty()) {
                continue;
            }
            path += "/" + dir;

            try {
                SftpATTRS attrs = channel.stat(path);
                if (!attrs.isDir()) {
                    throw new SftpException(ChannelSftp.SSH_FX_FAILURE, "'" + path + "' 已存在但不是一个目录");
                }
                log.debug("远程目录 '{}' 已存在", targetDir);
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    log.debug("远程目录 '{}' 不存在, 尝试创建", path);
                    channel.mkdir(path);
                    log.debug("远程目录 '{}' 创建成功", path);
                } else {
                    throw e;
                }
            }
        }
        return targetDir;
    }

    /**
     * 准备本地目录
     *
     * @param localDir 本地目录
     */
    private File prepareLocalDirectory(String localDir) {
        File localDirectory = new File(localDir);
        if (!localDirectory.exists() || !localDirectory.isDirectory()) {
            log.debug("本地目录 '{}' 不存在, 尝试创建", localDir);
            if (!localDirectory.mkdirs()) {
                throw new BusinessException("本地目录不存在且无法创建: " + localDir);
            }
            log.debug("本地目录 '{}' 创建成功", localDir);
        }
        if (!localDirectory.canWrite()) {
            throw new BusinessException(String.format("本地目录 '%s' 不可写", localDir));
        }
        return localDirectory;
    }

    /**
     * 获取远程文件大小
     *
     * @param channel    SFTP 通道
     * @param remotePath 远程文件路径
     * @return 文件大小
     */
    private long getRemoteFileSize(ChannelSftp channel, String remotePath) {
        try {
            SftpATTRS attrs = channel.stat(remotePath);
            if (attrs.isDir()) {
                throw new SftpException(ChannelSftp.SSH_FX_FAILURE, "'" + remotePath + "' 是一个目录, 无法下载");
            }
            long fileSize = attrs.getSize();
            log.debug("获取到远程文件 '{}' 大小: {}", remotePath, fileSize);
            return fileSize;
        } catch (SftpException e) {
            String errorMessage = String.format("获取远程文件 '%s' 的状态信息失败: %s", remotePath, e.getMessage());
            throw new BusinessException(errorMessage, e);
        }
    }

    /**
     * 执行命令并返回结果
     *
     * @param server  服务器信息
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws BusinessException 如果执行失败
     */
    public String executeCommand(ServerRecordDto server, String command) {
        log.info("请求在服务器 [{}] 上执行命令: '{}'", server.getHost(), command);

        // 建立SSH连接
        String sessionId = connect(server);
        try {
            // 执行命令
            SshExecResult result = executeExecCommand(sessionId, command);

            // 检查执行结果
            if (result.getExitCode() != 0) {
                throw new BusinessException("命令执行失败: " + result.getResult());
            }

            return result.getResult();
        } finally {
            // 确保断开连接
            disconnect(sessionId);
        }
    }

}