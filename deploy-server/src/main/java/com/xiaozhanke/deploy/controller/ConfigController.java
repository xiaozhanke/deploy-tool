package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.NginxConfigParams;
import com.xiaozhanke.deploy.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置文件接口
 *
 * @author xiaozhanke
 */
@Tag(name = "config", description = "配置文件接口")
@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * 生成 Nginx 配置文件内容
     *
     * @param params 配置文件参数
     * @return 根据模板生成的 conf 文件内容
     */
    @Operation(summary = "生成 Nginx 配置文件内容", description = "根据模板生成 Nginx 配置文件内容")
    @PostMapping("/nginx")
    public String addNginxConfig(@Validated @RequestBody NginxConfigParams params) {
        return configService.addNginxConfig(params);
    }
}
