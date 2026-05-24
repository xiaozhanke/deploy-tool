package com.xiaozhanke.deploy.constant;

/**
 * SSH 常量
 *
 * @author xiaozhanke
 */
public final class SshConstants {

    private SshConstants() {
    }

    // --- 命令执行相关 ---

    /**
     * 用于在 Shell 命令输出中标记退出码的特殊字符串。
     * 应选择一个极少在正常输出中出现的字符串。
     */
    public static final String EXIT_CODE = "__EXIT_CODE__";

    /**
     * 附加到用户命令后的 Shell 命令片段，用于打印退出码标记和实际退出码 ($?)。
     * 使用分号确保即使前一个命令失败，这个 echo 命令也能执行。
     */
    public static final String EXIT_CODE_COMMAND = "; echo " + EXIT_CODE + ":$?";

    /**
     * 退出码正则表达式
     * 匹配: ; echo __EXIT_CODE__:$?
     */
    public static final String EXIT_CODE_REGEX = ";\\s*echo\\s*" + EXIT_CODE + ":\\$\\?";

    /**
     * 退出码正则表达式
     * 匹配: ; echo __EXIT_CODE__:$? 和 __EXIT_CODE__:0
     */
    public static final String EXIT_CODE_AND_COMMAND_REGEX = ";\\s*echo\\s*" + EXIT_CODE + ":\\$\\?|" + EXIT_CODE + ":(-?\\d+)";

    /**
     * 退出码正则表达式
     * 匹配: __EXIT_CODE__:0
     */
    public static final String EXIT_CODE_RESULT_REGEX = EXIT_CODE + ":(-?\\d+)";

    /**
     * 缓冲区大小（字节），用于读取 Shell 输出流或 SFTP 文件传输。
     * 4096 (4KB) 是一个常见且合理的大小。
     */
    public static final int BUFFER_SIZE = 4096;

    // --- 超时设置 (单位：毫秒) ---

    /**
     * 默认的 SSH 会话（Session）连接超时时间（毫秒）。
     * 用于 {@link com.jcraft.jsch.Session#connect(int)}。
     * 30 秒是一个常用的值。
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    /**
     * 默认的 SSH 通道（Channel）连接超时时间（毫秒）。
     * 通常通道连接比会话建立快，可以设置短一些。
     * 用于 {@link com.jcraft.jsch.Channel#connect(int)}。
     * 10 秒是一个常用的值。
     */
    public static final int DEFAULT_CHANNEL_CONNECT_TIMEOUT = 10000;

    /**
     * 默认的服务器活动探测间隔时间（毫秒）。
     * Session 会在此间隔向服务器发送一个空包以保持连接活跃，防止因网络设备空闲超时而断开。
     * 用于 {@link com.jcraft.jsch.Session#setServerAliveInterval(int)}。
     * 60 秒是一个常用的值。
     */
    public static final int DEFAULT_SERVER_ALIVE_INTERVAL = 60000;

    /**
     * 默认的服务器活动探测最大尝试次数。
     * 如果连续发送探测包达到此次数后仍未收到服务器响应，则认为连接已断开。
     * 用于 {@link com.jcraft.jsch.Session#setServerAliveCountMax(int)}。
     * 3 次是一个常用的值。
     */
    public static final int DEFAULT_SERVER_ALIVE_COUNT_MAX = 3;

    // --- SSH 通道类型 ---

    /**
     * 定义 JSch 支持的常用 SSH 通道类型名称。
     * 用于 {@link com.jcraft.jsch.Session#openChannel(String)}。
     */
    public static final class ChannelType {
        private ChannelType() {
        }

        /**
         * 交互式 Shell 通道。用于执行需要终端交互的命令。
         */
        public static final String SHELL = "shell";

        /**
         * 执行单个命令的通道 (非交互式)。
         */
        public static final String EXEC = "exec";

        /**
         * 文件传输协议通道 (Secure File Transfer Protocol)。
         */
        public static final String SFTP = "sftp";
    }
}
