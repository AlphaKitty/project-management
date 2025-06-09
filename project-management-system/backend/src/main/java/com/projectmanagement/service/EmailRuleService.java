package com.projectmanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.dto.EmailRuleDTO;
import com.projectmanagement.dto.UserEmailPreferenceDTO;
import com.projectmanagement.entity.EmailSendRule;
import com.projectmanagement.entity.UserEmailPreference;

/**
 * 邮件规则管理Service接口
 */
public interface EmailRuleService {

    /**
     * 分页查询邮件规则
     */
    Page<EmailSendRule> getEmailRules(int current, int size, String ruleType, Boolean enabled);

    /**
     * 根据ID获取邮件规则
     */
    EmailSendRule getEmailRuleById(Long id);

    /**
     * 创建邮件规则
     */
    boolean createEmailRule(EmailRuleDTO emailRuleDTO, Long creatorId);

    /**
     * 更新邮件规则
     */
    boolean updateEmailRule(Long id, EmailRuleDTO emailRuleDTO);

    /**
     * 删除邮件规则
     */
    boolean deleteEmailRule(Long id);

    /**
     * 启用/禁用邮件规则
     */
    boolean toggleEmailRule(Long id, Boolean enabled);

    /**
     * 获取用户邮件偏好设置
     */
    UserEmailPreference getUserEmailPreference(Long userId);

    /**
     * 更新用户邮件偏好设置
     */
    boolean updateUserEmailPreference(Long userId, UserEmailPreferenceDTO preferenceDTO);

    /**
     * 获取规则类型列表
     */
    String[] getRuleTypes();

    /**
     * 获取邮件模板列表
     */
    String[] getEmailTemplates();

    /**
     * 解析触发条件JSON
     */
    java.util.Map<String, Object> parseTriggerCondition(String triggerCondition);

    /**
     * 测试截止日期规则
     */
    void testDeadlineRule(com.projectmanagement.entity.EmailSendRule rule, java.util.Map<String, Object> conditions);

    /**
     * 测试逾期任务规则
     */
    void testOverdueRule(com.projectmanagement.entity.EmailSendRule rule, java.util.Map<String, Object> conditions);
}