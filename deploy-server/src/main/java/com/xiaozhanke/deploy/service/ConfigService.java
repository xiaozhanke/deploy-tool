package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.exception.BusinessException;
import com.xiaozhanke.deploy.model.request.NginxConfigParams;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;

/**
 * 配置文件服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class ConfigService {

    private final FreeMarkerConfigurer freeMarkerConfigurer;

    public ConfigService(FreeMarkerConfigurer freeMarkerConfigurer) {
        this.freeMarkerConfigurer = freeMarkerConfigurer;
    }

    /**
     * 生成 Nginx 配置文件
     *
     * @param params 配置参数
     * @return 配置文件内容
     */
    public String addNginxConfig(NginxConfigParams params) {
        log.info("开始生成配置文件");
        String templateName = "nginx.conf.ftl";
        try {
            log.info("获取模板文件: {}", templateName);
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(params, writer);
            log.info("生成配置文件完成:\n{}", writer);
            return writer.toString();
        } catch (IOException e) {
            String errorMessage = String.format("无法加载配置文件模板 [%s]，请检查文件是否存在且可读。", templateName);
            throw new BusinessException(errorMessage, e);
        } catch (TemplateException e) {
            String errorMessage = String.format("使用模板 [%s] 生成配置文件时失败，请检查模板语法和参数。", templateName);
            throw new BusinessException(errorMessage, e);
        }
    }
}
