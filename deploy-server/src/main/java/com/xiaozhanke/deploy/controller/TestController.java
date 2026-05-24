package com.xiaozhanke.deploy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 *
 * @author xiaozhanke
 */
@Tag(name = "test", description = "测试接口")
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    /**
     * 测试 API 可用性
     *
     * @return pong
     */
    @Operation(summary = "ping", description = "测试 API 可用性")
    @GetMapping("/ping")
    public String ping() {
        log.info("收到 ping 请求，返回 pong");
        return "pong";
    }
}
