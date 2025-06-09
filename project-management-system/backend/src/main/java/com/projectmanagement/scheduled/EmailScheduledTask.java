package com.projectmanagement.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.projectmanagement.entity.EmailSendRule;
import com.projectmanagement.mapper.EmailSendRuleMapper;
import com.projectmanagement.service.EmailSendService;
import com.projectmanagement.service.impl.EmailRuleProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 邮件定时任务
 */
@Slf4j
@Component
public class EmailScheduledTask {

    @Autowired
    private EmailSendService emailSendService;

    @Autowired
    private EmailSendRuleMapper emailSendRuleMapper;

    @Autowired
    private EmailRuleProcessor ruleProcessor;

    /**
     * 每30秒处理一次邮件发送队列
     */
    @Scheduled(fixedRate = 30000)
    public void processEmailQueue() {
        try {
            emailSendService.processEmailQueue();
        } catch (Exception e) {
            log.error("处理邮件队列失败", e);
        }
    }

    /**
     * 每小时检查一次是否需要发送定时邮件
     * 支持从规则配置中读取 send_time
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时整点执行
    // @Scheduled(cron = "0 * * * * ?") // 每分钟执行，用于测试
    // @Scheduled(cron = "0 */5 * * * ?") // 每5分钟执行一次
    public void processScheduledEmails() {
        try {
            LocalTime currentTime = LocalTime.now();
            String currentTimeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            log.info("检查定时邮件任务，当前时间: {}", currentTimeStr);

            // 查找启用的邮件规则
            QueryWrapper<EmailSendRule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("enabled", true)
                    .in("rule_type", "DEADLINE", "OVERDUE");

            List<EmailSendRule> rules = emailSendRuleMapper.selectList(queryWrapper);
            log.info("找到 {} 条启用的邮件规则", rules.size());

            for (EmailSendRule rule : rules) {
                try {
                    // 解析触发条件
                    String triggerCondition = rule.getTriggerCondition();
                    if (triggerCondition != null) {
                        Map<String, Object> conditions = ruleProcessor.parseTriggerCondition(triggerCondition);

                        // 检查是否到了发送时间
                        if (ruleProcessor.isTimeToSend(conditions)) {
                            log.info("触发定时邮件规则: {} (发送时间: {})", rule.getRuleName(),
                                    ruleProcessor.getSendTime(conditions));

                            // 根据规则类型执行相应的邮件发送
                            if ("DEADLINE".equals(rule.getRuleType())) {
                                if (ruleProcessor.isOverdueRule(conditions)) {
                                    // 逾期任务提醒
                                    emailSendService.handleOverdueTaskNotification();
                                } else {
                                    // 截止日期提醒 - 传递规则配置
                                    emailSendService.handleDeadlineReminderNotification(rule, conditions);
                                }
                            } else if ("OVERDUE".equals(rule.getRuleType())) {
                                // 逾期任务提醒
                                emailSendService.handleOverdueTaskNotification();
                            }
                        } else {
                            log.debug("规则 {} 未到发送时间，配置时间: {}, 当前时间: {}",
                                    rule.getRuleName(), ruleProcessor.getSendTime(conditions), currentTimeStr);
                        }
                    }
                } catch (Exception e) {
                    log.error("处理邮件规则失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("处理定时邮件失败", e);
        }
    }

    /**
     * 兜底方案：每天早上8点处理截止日期和逾期任务提醒
     * 如果规则配置中没有 send_time，则使用此默认时间
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processDefaultScheduledEmails() {
        try {
            log.info("执行默认定时邮件任务 (早上8点)");

            // 检查是否有规则配置了自定义时间
            QueryWrapper<EmailSendRule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("enabled", true)
                    .eq("rule_type", "DEADLINE");

            List<EmailSendRule> rules = emailSendRuleMapper.selectList(queryWrapper);
            boolean hasCustomTime = false;

            for (EmailSendRule rule : rules) {
                try {
                    String triggerCondition = rule.getTriggerCondition();
                    if (triggerCondition != null) {
                        Map<String, Object> conditions = ruleProcessor.parseTriggerCondition(triggerCondition);
                        if (ruleProcessor.getSendTime(conditions) != null) {
                            hasCustomTime = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析规则配置失败: ruleId={}", rule.getId());
                }
            }

            // 如果没有自定义时间配置，则执行默认的邮件发送
            if (!hasCustomTime) {
                log.info("没有找到自定义发送时间配置，执行默认邮件发送");
                emailSendService.handleDeadlineReminderNotification();
                emailSendService.handleOverdueTaskNotification();
            } else {
                log.info("已配置自定义发送时间，跳过默认邮件发送");
            }

        } catch (Exception e) {
            log.error("处理默认定时邮件失败", e);
        }
    }
}