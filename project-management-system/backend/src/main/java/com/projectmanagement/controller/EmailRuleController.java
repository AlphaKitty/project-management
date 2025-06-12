package com.projectmanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.common.Result;
import com.projectmanagement.common.ResultCode;
import com.projectmanagement.dto.EmailRuleDTO;
import com.projectmanagement.dto.UserEmailPreferenceDTO;
import com.projectmanagement.entity.EmailSendRule;
import com.projectmanagement.entity.User;
import com.projectmanagement.entity.UserEmailPreference;
import com.projectmanagement.service.EmailRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 邮件规则管理Controller
 */
@RestController
@RequestMapping("/email-rules")
public class EmailRuleController {

    @Autowired
    private EmailRuleService emailRuleService;

    /**
     * 分页查询邮件规则
     */
    @GetMapping
    public Result<Page<EmailSendRule>> getEmailRules(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Boolean enabled) {
        Page<EmailSendRule> page = emailRuleService.getEmailRules(current, size, ruleType, enabled);
        return Result.success(page);
    }

    /**
     * 根据ID获取邮件规则
     */
    @GetMapping("/{id}")
    public Result<EmailSendRule> getEmailRuleById(@PathVariable Long id) {
        EmailSendRule rule = emailRuleService.getEmailRuleById(id);
        if (rule == null) {
            return Result.error("邮件规则不存在");
        }
        return Result.success(rule);
    }

    /**
     * 创建邮件规则
     */
    @PostMapping
    public Result<String> createEmailRule(@RequestBody EmailRuleDTO emailRuleDTO, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        boolean success = emailRuleService.createEmailRule(emailRuleDTO, currentUser.getId());
        if (success) {
            return Result.success("邮件规则创建成功");
        } else {
            return Result.error("邮件规则创建失败");
        }
    }

    /**
     * 更新邮件规则
     */
    @PutMapping("/{id}")
    public Result<String> updateEmailRule(@PathVariable Long id, @RequestBody EmailRuleDTO emailRuleDTO) {
        boolean success = emailRuleService.updateEmailRule(id, emailRuleDTO);
        if (success) {
            return Result.success("邮件规则更新成功");
        } else {
            return Result.error("邮件规则更新失败");
        }
    }

    /**
     * 删除邮件规则
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteEmailRule(@PathVariable Long id) {
        boolean success = emailRuleService.deleteEmailRule(id);
        if (success) {
            return Result.success("邮件规则删除成功");
        } else {
            return Result.error("邮件规则删除失败");
        }
    }

    /**
     * 启用/禁用邮件规则
     */
    @PutMapping("/{id}/toggle")
    public Result<String> toggleEmailRule(@PathVariable Long id, @RequestParam Boolean enabled) {
        boolean success = emailRuleService.toggleEmailRule(id, enabled);
        if (success) {
            String message = enabled ? "邮件规则已启用" : "邮件规则已禁用";
            return Result.success(message);
        } else {
            return Result.error("操作失败");
        }
    }

    /**
     * 获取当前用户的邮件偏好设置
     */
    @GetMapping("/preferences")
    public Result<UserEmailPreference> getUserEmailPreference(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        UserEmailPreference preference = emailRuleService.getUserEmailPreference(currentUser.getId());
        return Result.success(preference);
    }

    /**
     * 更新当前用户的邮件偏好设置
     */
    @PutMapping("/preferences")
    public Result<String> updateUserEmailPreference(@RequestBody UserEmailPreferenceDTO preferenceDTO,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        boolean success = emailRuleService.updateUserEmailPreference(currentUser.getId(), preferenceDTO);
        if (success) {
            return Result.success("邮件偏好设置更新成功");
        } else {
            return Result.error("邮件偏好设置更新失败");
        }
    }

    /**
     * 获取规则类型列表
     */
    @GetMapping("/rule-types")
    public Result<String[]> getRuleTypes() {
        String[] types = emailRuleService.getRuleTypes();
        return Result.success(types);
    }

    /**
     * 获取邮件模板列表
     */
    @GetMapping("/email-templates")
    public Result<String[]> getEmailTemplates() {
        String[] templates = emailRuleService.getEmailTemplates();
        return Result.success(templates);
    }

    /**
     * 测试执行邮件规则
     */
    @PostMapping("/test-rule/{id}")
    public Result<String> testEmailRule(@PathVariable Long id) {
        try {
            EmailSendRule rule = emailRuleService.getEmailRuleById(id);
            if (rule == null) {
                return Result.error("邮件规则不存在");
            }

            if (!rule.getEnabled()) {
                return Result.error("邮件规则未启用");
            }

            // 解析触发条件
            Map<String, Object> conditions = emailRuleService.parseTriggerCondition(rule.getTriggerCondition());

            // 根据规则类型执行相应的测试
            switch (rule.getRuleType()) {
                case "DEADLINE":
                    emailRuleService.testDeadlineRule(rule, conditions);
                    return Result.success("截止日期提醒规则测试成功，请检查邮件队列");

                case "STATUS_CHANGE":
                    return Result.success("状态变更规则需要通过实际的任务状态变更来触发");

                case "OVERDUE":
                    emailRuleService.testOverdueRule(rule, conditions);
                    return Result.success("逾期任务提醒规则测试成功，请检查邮件队列");

                default:
                    return Result.error("不支持的规则类型: " + rule.getRuleType());
            }

        } catch (Exception e) {
            // log.error("测试邮件规则失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }
}