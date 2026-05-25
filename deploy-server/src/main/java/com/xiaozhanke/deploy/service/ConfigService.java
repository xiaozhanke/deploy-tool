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
        String templateName = "nginx.conf.ftl";
        log.info("开始生成 Nginx 配置, 模板=[{}]", templateName);
        try {
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(params, writer);
            String content = writer.toString();
            // 仅记录度量信息（行数 / 字符数 / 模板名）以避免把内网域名、upstream 地址、SSL 配置等基础设施细节
            // 灌进日志体系；完整配置仍以接口返回值形式回给调用方落盘
            log.info("生成 Nginx 配置完成, 模板=[{}], 行数={}, 字符数={}",
                    templateName,
                    countLines(content),
                    content.length());
            return content;
        } catch (IOException e) {
            String errorMessage = String.format("无法加载配置文件模板 [%s]，请检查文件是否存在且可读。", templateName);
            throw new BusinessException(errorMessage, e);
        } catch (TemplateException e) {
            String errorMessage = String.format("使用模板 [%s] 生成配置文件时失败，请检查模板语法和参数。", templateName);
            throw new BusinessException(errorMessage, e);
        }
    }

    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
