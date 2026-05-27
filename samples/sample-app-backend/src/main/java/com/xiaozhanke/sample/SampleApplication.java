package com.xiaozhanke.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * deploy-tool 部署测试用最简后端应用。
 *
 * <p>启动方式:{@code java -jar sample-app-backend-1.0.0.jar --server.port=8080 --spring.profiles.active=test}
 *
 * <p>探活接口:
 * <ul>
 *   <li>{@code GET /hello} — 返回端口/profile/启动时间(供 deploy-tool 端到端验证用)</li>
 *   <li>{@code GET /actuator/health} — Spring Boot Actuator 标准探活</li>
 * </ul>
 */
@SpringBootApplication
@RestController
public class SampleApplication {

    private static final LocalDateTime BOOT_TIME = LocalDateTime.now();

    @Value("${server.port:8080}")
    private int port;

    @Value("${spring.profiles.active:default}")
    private String profile;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "app", "sample-app-backend",
                "version", "1.0.0",
                "port", port,
                "profile", profile,
                "bootTime", BOOT_TIME.toString()
        );
    }
}
