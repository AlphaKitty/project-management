package com.projectmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.projectmanagement.dto.EmailTemplateDTO;
import com.projectmanagement.entity.EmailTemplate;

import java.util.List;
import java.util.Map;

/**
 * 邮件模板服务接口
 */
public interface EmailTemplateService extends IService<EmailTemplate> {

    /**
     * 获取邮件模板列表
     */
    List<EmailTemplate> getTemplateList();

    /**
     * 根据模板代码获取模板
     */
    EmailTemplate getTemplateByCode(String templateCode);

    /**
     * 创建邮件模板
     */
    EmailTemplate createTemplate(EmailTemplateDTO templateDTO);

    /**
     * 更新邮件模板
     */
    EmailTemplate updateTemplate(Long templateId, EmailTemplateDTO templateDTO);

    /**
     * 删除邮件模板
     */
    boolean deleteTemplate(Long templateId);

    /**
     * 渲染邮件模板
     * @param templateCode 模板代码
     * @param variables 模板变量
     * @return 渲染后的邮件内容（主题和内容）
     */
    Map<String, String> renderTemplate(String templateCode, Map<String, Object> variables);

    /**
     * 发送模板邮件
     * @param templateCode 模板代码
     * @param to 收件人
     * @param variables 模板变量
     * @return 发送结果
     */
    boolean sendTemplateEmail(String templateCode, String to, Map<String, Object> variables);
}