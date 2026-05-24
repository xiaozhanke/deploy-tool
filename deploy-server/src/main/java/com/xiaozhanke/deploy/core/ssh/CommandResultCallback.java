package com.xiaozhanke.deploy.core.ssh;

/**
 * 命令执行结果回调接口
 *
 * @author xiaozhanke
 */
public interface CommandResultCallback {

    void onSuccess(String sessionId, String channelId, String output);

    void onFailure(String sessionId, String channelId, String error, int exitCode);
}
