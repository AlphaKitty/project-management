package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.EmailTemplateDTO;
import com.projectmanagement.entity.EmailTemplate;
import com.projectmanagement.mapper.EmailTemplateMapper;
import com.projectmanagement.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮件模板服务实现类
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl extends ServiceImpl<EmailTemplateMapper, EmailTemplate>
        implements EmailTemplateService {

    private final EmailTemplateMapper emailTemplateMapper;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    /**
     * 应用启动时检查和更新邮件模板
     */
    @PostConstruct
    public void initializeEmailTemplates() {
        System.out.println("=== 邮件模板服务已启动，以数据库中的模板配置为准 ===");
    }

    @Override
    public List<EmailTemplate> getTemplateList() {
        return emailTemplateMapper.selectList(null);
    }

    @Override
    public EmailTemplate getTemplateByCode(String templateCode) {
        return emailTemplateMapper.selectByTemplateCode(templateCode);
    }

    @Override
    @Transactional
    public EmailTemplate createTemplate(EmailTemplateDTO templateDTO) {
        EmailTemplate template = new EmailTemplate();
        template.setTemplateCode(templateDTO.getTemplateCode());
        template.setTemplateName(templateDTO.getTemplateName());
        template.setSubjectTemplate(templateDTO.getSubjectTemplate());
        template.setContentTemplate(templateDTO.getContentTemplate());
        template.setTemplateType(templateDTO.getTemplateType() != null ? templateDTO.getTemplateType() : "HTML");
        template.setDescription(templateDTO.getDescription());
        template.setVariablesDescription(templateDTO.getSupportedVariables());
        template.setEnabled(templateDTO.getEnabled() != null ? templateDTO.getEnabled() : true);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreatorId(templateDTO.getCreatorId());

        emailTemplateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public EmailTemplate updateTemplate(Long templateId, EmailTemplateDTO templateDTO) {
        EmailTemplate template = emailTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("邮件模板不存在");
        }

        template.setTemplateName(templateDTO.getTemplateName());
        template.setSubjectTemplate(templateDTO.getSubjectTemplate());
        template.setContentTemplate(templateDTO.getContentTemplate());
        template.setTemplateType(templateDTO.getTemplateType());
        template.setDescription(templateDTO.getDescription());
        template.setVariablesDescription(templateDTO.getSupportedVariables());
        template.setEnabled(templateDTO.getEnabled());
        template.setUpdateTime(LocalDateTime.now());

        emailTemplateMapper.updateById(template);
        return template;
    }

    @Override
    @Transactional
    public boolean deleteTemplate(Long templateId) {
        return emailTemplateMapper.deleteById(templateId) > 0;
    }

    @Override
    public Map<String, String> renderTemplate(String templateCode, Map<String, Object> variables) {
        EmailTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new RuntimeException("邮件模板不存在: " + templateCode + "，请在数据库中配置相应的邮件模板");
        }

        if (!template.getEnabled()) {
            throw new RuntimeException("邮件模板已禁用: " + templateCode);
        }

        Map<String, String> result = new HashMap<>();

        try {
            // 创建Thymeleaf上下文
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }

            // 渲染主题
            String subject = renderString(template.getSubjectTemplate(), context);
            result.put("subject", subject);

            // 渲染内容
            String content = renderString(template.getContentTemplate(), context);
            result.put("content", content);

            result.put("templateType", template.getTemplateType());

        } catch (Exception e) {
            System.err.println("模板渲染失败: " + e.getMessage());
            throw new RuntimeException("模板渲染失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean sendTemplateEmail(String templateCode, String to, Map<String, Object> variables) {
        try {
            // 渲染模板
            Map<String, String> rendered = renderTemplate(templateCode, variables);

            String subject = rendered.get("subject");
            String content = rendered.get("content");
            String templateType = rendered.get("templateType");

            // 创建邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFromAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            // 根据模板类型设置内容
            boolean isHtml = "HTML".equalsIgnoreCase(templateType);
            helper.setText(content, isHtml);

            // 发送邮件
            mailSender.send(message);

            System.out.println("📧 模板邮件发送成功: " + templateCode + " -> " + to);
            return true;

        } catch (Exception e) {
            System.err.println("模板邮件发送失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 渲染字符串模板
     */
    private String renderString(String template, Context context) {
        if (template == null || template.trim().isEmpty()) {
            return "";
        }

        try {
            return templateEngine.process(template, context);
        } catch (Exception e) {
            // 如果Thymeleaf解析失败，尝试简单的变量替换
            System.out.println("⚠️  Thymeleaf解析失败，使用简单变量替换: " + e.getMessage());
            return simpleVariableReplace(template, getAllVariables(context));
        }
    }

    /**
     * 获取Context中的所有变量
     */
    private Map<String, Object> getAllVariables(Context context) {
        Map<String, Object> variables = new HashMap<>();
        if (context != null) {
            for (String variableName : context.getVariableNames()) {
                variables.put(variableName, context.getVariable(variableName));
            }
        }
        return variables;
    }

    /**
     * 简单的变量替换（备用方案）
     */
    private String simpleVariableReplace(String template, Map<String, Object> variables) {
        String result = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : "";

                // 替换 ${variable} 格式
                result = result.replace("${" + key + "}", value);
                // 替换 [[${variable}]] 格式
                result = result.replace("[[${" + key + "}]]", value);
                // 替换 {variable} 格式
                result = result.replace("{" + key + "}", value);
            }
        }
        return result;
    }
}