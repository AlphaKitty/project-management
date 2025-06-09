package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.dto.EmailRuleDTO;
import com.projectmanagement.dto.UserEmailPreferenceDTO;
import com.projectmanagement.entity.EmailSendRule;
import com.projectmanagement.entity.EmailTemplate;
import com.projectmanagement.entity.UserEmailPreference;
import com.projectmanagement.mapper.EmailSendRuleMapper;
import com.projectmanagement.mapper.EmailTemplateMapper;
import com.projectmanagement.mapper.UserEmailPreferenceMapper;
import com.projectmanagement.service.EmailRuleService;
import com.projectmanagement.service.EmailSendService;
import com.projectmanagement.service.impl.EmailRuleProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 邮件规则管理Service实现类
 */
@Service
public class EmailRuleServiceImpl implements EmailRuleService {

    @Autowired
    private EmailSendRuleMapper emailSendRuleMapper;

    @Autowired
    private UserEmailPreferenceMapper userEmailPreferenceMapper;

    @Autowired
    private EmailTemplateMapper emailTemplateMapper;

    @Autowired
    private EmailRuleProcessor emailRuleProcessor;

    @Autowired
    private EmailSendService emailSendService;

    @Override
    public Page<EmailSendRule> getEmailRules(int current, int size, String ruleType, Boolean enabled) {
        Page<EmailSendRule> page = new Page<>(current, size);
        QueryWrapper<EmailSendRule> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(ruleType)) {
            queryWrapper.eq("rule_type", ruleType);
        }
        if (enabled != null) {
            queryWrapper.eq("enabled", enabled);
        }

        queryWrapper.orderByDesc("priority").orderByDesc("create_time");
        return emailSendRuleMapper.selectPage(page, queryWrapper);
    }

    @Override
    public EmailSendRule getEmailRuleById(Long id) {
        return emailSendRuleMapper.selectById(id);
    }

    @Override
    public boolean createEmailRule(EmailRuleDTO emailRuleDTO, Long creatorId) {
        EmailSendRule emailSendRule = new EmailSendRule();
        BeanUtils.copyProperties(emailRuleDTO, emailSendRule);
        emailSendRule.setCreatorId(creatorId);
        return emailSendRuleMapper.insert(emailSendRule) > 0;
    }

    @Override
    public boolean updateEmailRule(Long id, EmailRuleDTO emailRuleDTO) {
        EmailSendRule emailSendRule = new EmailSendRule();
        BeanUtils.copyProperties(emailRuleDTO, emailSendRule);
        emailSendRule.setId(id);
        return emailSendRuleMapper.updateById(emailSendRule) > 0;
    }

    @Override
    public boolean deleteEmailRule(Long id) {
        return emailSendRuleMapper.deleteById(id) > 0;
    }

    @Override
    public boolean toggleEmailRule(Long id, Boolean enabled) {
        EmailSendRule emailSendRule = new EmailSendRule();
        emailSendRule.setId(id);
        emailSendRule.setEnabled(enabled);
        return emailSendRuleMapper.updateById(emailSendRule) > 0;
    }

    @Override
    public UserEmailPreference getUserEmailPreference(Long userId) {
        QueryWrapper<UserEmailPreference> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserEmailPreference preference = userEmailPreferenceMapper.selectOne(queryWrapper);

        // 如果用户没有偏好设置，创建默认设置
        if (preference == null) {
            preference = createDefaultPreference(userId);
        }

        return preference;
    }

    @Override
    public boolean updateUserEmailPreference(Long userId, UserEmailPreferenceDTO preferenceDTO) {
        QueryWrapper<UserEmailPreference> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserEmailPreference existingPreference = userEmailPreferenceMapper.selectOne(queryWrapper);

        UserEmailPreference preference = new UserEmailPreference();
        BeanUtils.copyProperties(preferenceDTO, preference);
        preference.setUserId(userId);

        if (existingPreference != null) {
            preference.setId(existingPreference.getId());
            return userEmailPreferenceMapper.updateById(preference) > 0;
        } else {
            return userEmailPreferenceMapper.insert(preference) > 0;
        }
    }

    @Override
    public String[] getRuleTypes() {
        return new String[] { "DEADLINE", "STATUS_CHANGE", "SUMMARY", "INACTIVE" };
    }

    @Override
    public String[] getEmailTemplates() {
        QueryWrapper<EmailTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", true);
        queryWrapper.select("template_code", "template_name");

        List<EmailTemplate> templates = emailTemplateMapper.selectList(queryWrapper);
        return templates.stream()
                .map(template -> template.getTemplateCode() + ":" + template.getTemplateName())
                .toArray(String[]::new);
    }

    /**
     * 创建默认的用户邮件偏好设置
     */
    private UserEmailPreference createDefaultPreference(Long userId) {
        UserEmailPreference preference = new UserEmailPreference();
        preference.setUserId(userId);
        preference.setEnableEmail(true);
        preference.setDeadlineReminder(true);
        preference.setStatusChange(true);
        preference.setTaskAssignment(true);
        preference.setDailySummary(false);
        preference.setWeeklySummary(false);
        preference.setMonthlySummary(false);
        preference.setDailySummaryTime("08:30");
        preference.setWeeklyDay(1);
        preference.setMonthlyDay(1);
        preference.setUrgentOnly(false);
        preference.setQuietStart("22:00");
        preference.setQuietEnd("08:00");
        preference.setMaxEmailsPerDay(10);

        userEmailPreferenceMapper.insert(preference);
        return preference;
    }

    @Override
    public Map<String, Object> parseTriggerCondition(String triggerCondition) {
        return emailRuleProcessor.parseTriggerCondition(triggerCondition);
    }

    @Override
    public void testDeadlineRule(EmailSendRule rule, Map<String, Object> conditions) {
        // 直接调用邮件发送服务的截止日期提醒方法
        emailSendService.handleDeadlineReminderNotification(rule, conditions);
    }

    @Override
    public void testOverdueRule(EmailSendRule rule, Map<String, Object> conditions) {
        // 直接调用邮件发送服务的逾期任务提醒方法
        emailSendService.handleOverdueTaskNotification();
    }
}