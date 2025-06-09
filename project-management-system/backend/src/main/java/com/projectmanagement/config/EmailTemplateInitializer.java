package com.projectmanagement.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.projectmanagement.entity.EmailTemplate;
import com.projectmanagement.mapper.EmailTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 邮件模板初始化器
 * 在应用启动时确保模板正确
 */
@Slf4j
@Component
public class EmailTemplateInitializer implements ApplicationRunner {

    @Autowired
    private EmailTemplateMapper emailTemplateMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 开始初始化邮件模板 ===");

        // 确保TODO_REMINDER模板存在且正确
        ensureTodoReminderTemplate();

        // 确保DEADLINE_REMINDER模板存在且正确
        ensureDeadlineReminderTemplate();

        // 确保TASK_ASSIGNMENT模板存在且正确
        ensureTaskAssignmentTemplate();

        // 确保TASK_STATUS_CHANGE模板存在且正确
        ensureTaskStatusChangeTemplate();

        log.info("=== 邮件模板初始化完成 ===");
    }

    private void ensureTodoReminderTemplate() {
        String templateCode = "TODO_REMINDER";

        QueryWrapper<EmailTemplate> query = new QueryWrapper<>();
        query.eq("template_code", templateCode);
        EmailTemplate existing = emailTemplateMapper.selectOne(query);

        if (existing != null) {
            // 检查模板内容是否包含正确的变量
            String content = existing.getContentTemplate();
            if (content.contains("${todoCount}") && content.contains("${taskListHtml}")) {
                log.info("✅ TODO_REMINDER模板已存在且正确");
                return;
            }

            log.info("🔄 更新TODO_REMINDER模板内容");
            // existing.setContentTemplate(getCorrectTodoReminderContent());
            existing.setVariablesDescription(
                    "{\"todoCount\":\"任务数量\",\"taskListHtml\":\"任务列表HTML\",\"currentDate\":\"当前日期\"}");
            existing.setUpdateTime(LocalDateTime.now());
            emailTemplateMapper.updateById(existing);
        } else {
            log.info("➕ 创建TODO_REMINDER模板");
            EmailTemplate template = new EmailTemplate();
            template.setTemplateCode(templateCode);
            template.setTemplateName("待办任务提醒模板");
            template.setTemplateType("HTML");
            template.setSubjectTemplate("[项目管理系统] 待办任务提醒 - ${todoCount}个任务");
            // template.setContentTemplate(getCorrectTodoReminderContent());
            template.setVariablesDescription(
                    "{\"todoCount\":\"任务数量\",\"taskListHtml\":\"任务列表HTML\",\"currentDate\":\"当前日期\"}");
            template.setEnabled(true);
            template.setDescription("系统自动创建的待办任务提醒模板");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            emailTemplateMapper.insert(template);
        }

        log.info("✅ TODO_REMINDER模板配置完成");
    }

    private void ensureDeadlineReminderTemplate() {
        String templateCode = "DEADLINE_REMINDER";

        QueryWrapper<EmailTemplate> query = new QueryWrapper<>();
        query.eq("template_code", templateCode);
        EmailTemplate existing = emailTemplateMapper.selectOne(query);

        if (existing != null) {
            // 检查模板内容是否包含正确的变量
            String content = existing.getContentTemplate();
            if (content.contains("${taskCount}") && content.contains("${taskListHtml}")) {
                log.info("✅ DEADLINE_REMINDER模板已存在且正确");
                return;
            }

            // log.info("🔄 更新DEADLINE_REMINDER模板内容");
            // existing.setContentTemplate(getCorrectDeadlineReminderContent());
            existing.setVariablesDescription(
                    "{\"userName\":\"用户名\",\"taskCount\":\"任务数量\",\"taskListHtml\":\"任务列表HTML\",\"currentDate\":\"当前日期\"}");
            existing.setUpdateTime(LocalDateTime.now());
            emailTemplateMapper.updateById(existing);
        } else {
            log.info("➕ 创建DEADLINE_REMINDER模板");
            EmailTemplate template = new EmailTemplate();
            template.setTemplateCode(templateCode);
            template.setTemplateName("截止日期提醒模板");
            template.setTemplateType("HTML");
            template.setSubjectTemplate("⏰ 任务截止提醒 - ${taskCount}个任务即将到期");
            // template.setContentTemplate(getCorrectDeadlineReminderContent());
            template.setVariablesDescription(
                    "{\"userName\":\"用户名\",\"taskCount\":\"任务数量\",\"taskListHtml\":\"任务列表HTML\",\"currentDate\":\"当前日期\"}");
            template.setEnabled(true);
            template.setDescription("系统自动创建的截止日期提醒模板");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            emailTemplateMapper.insert(template);
        }

        log.info("✅ DEADLINE_REMINDER模板配置完成");
    }

    private void ensureTaskAssignmentTemplate() {
        String templateCode = "TASK_ASSIGNMENT";

        QueryWrapper<EmailTemplate> query = new QueryWrapper<>();
        query.eq("template_code", templateCode);
        EmailTemplate existing = emailTemplateMapper.selectOne(query);

        if (existing != null) {
            // 检查模板类型是否为HTML
            if ("HTML".equals(existing.getTemplateType())
                    && existing.getContentTemplate().contains("<!DOCTYPE html>")) {
                log.info("✅ TASK_ASSIGNMENT模板已存在且正确");
                return;
            }

            log.info("🔄 更新TASK_ASSIGNMENT模板内容");
            existing.setTemplateType("HTML");
            existing.setSubjectTemplate("📝 新任务分配通知 - ${taskTitle}");
            existing.setContentTemplate(getTaskAssignmentContent());
            existing.setVariablesDescription(
                    "{\"assigneeName\":\"分配给的用户名\",\"taskTitle\":\"任务标题\",\"description\":\"任务描述\",\"priority\":\"优先级\",\"dueDate\":\"截止日期\",\"assignedBy\":\"分配人\",\"projectName\":\"项目名称\",\"currentDate\":\"当前日期\"}");
            existing.setUpdateTime(LocalDateTime.now());
            emailTemplateMapper.updateById(existing);
        } else {
            log.info("➕ 创建TASK_ASSIGNMENT模板");
            EmailTemplate template = new EmailTemplate();
            template.setTemplateCode(templateCode);
            template.setTemplateName("任务分配通知模板");
            template.setTemplateType("HTML");
            template.setSubjectTemplate("📝 新任务分配通知 - ${taskTitle}");
            template.setContentTemplate(getTaskAssignmentContent());
            template.setVariablesDescription(
                    "{\"assigneeName\":\"分配给的用户名\",\"taskTitle\":\"任务标题\",\"description\":\"任务描述\",\"priority\":\"优先级\",\"dueDate\":\"截止日期\",\"assignedBy\":\"分配人\",\"projectName\":\"项目名称\",\"currentDate\":\"当前日期\"}");
            template.setEnabled(true);
            template.setDescription("系统自动创建的任务分配通知模板");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            emailTemplateMapper.insert(template);
        }

        log.info("✅ TASK_ASSIGNMENT模板配置完成");
    }

    private void ensureTaskStatusChangeTemplate() {
        String templateCode = "TASK_STATUS_CHANGE";

        QueryWrapper<EmailTemplate> query = new QueryWrapper<>();
        query.eq("template_code", templateCode);
        EmailTemplate existing = emailTemplateMapper.selectOne(query);

        if (existing != null) {
            // 检查模板类型是否为HTML
            if ("HTML".equals(existing.getTemplateType())
                    && existing.getContentTemplate().contains("<!DOCTYPE html>")) {
                log.info("✅ TASK_STATUS_CHANGE模板已存在且正确");
                return;
            }

            log.info("🔄 更新TASK_STATUS_CHANGE模板内容");
            existing.setTemplateType("HTML");
            existing.setSubjectTemplate("🔄 任务状态更新通知 - ${taskTitle}");
            existing.setContentTemplate(getTaskStatusChangeContent());
            existing.setVariablesDescription(
                    "{\"assigneeName\":\"用户名\",\"taskTitle\":\"任务标题\",\"projectName\":\"项目名称\",\"oldStatus\":\"原状态\",\"newStatus\":\"新状态\",\"completionStatus\":\"完成状态\",\"dueDate\":\"截止日期\",\"currentDate\":\"当前日期\"}");
            existing.setUpdateTime(LocalDateTime.now());
            emailTemplateMapper.updateById(existing);
        } else {
            log.info("➕ 创建TASK_STATUS_CHANGE模板");
            EmailTemplate template = new EmailTemplate();
            template.setTemplateCode(templateCode);
            template.setTemplateName("任务状态变更通知模板");
            template.setTemplateType("HTML");
            template.setSubjectTemplate("🔄 任务状态更新通知 - ${taskTitle}");
            template.setContentTemplate(getTaskStatusChangeContent());
            template.setVariablesDescription(
                    "{\"assigneeName\":\"用户名\",\"taskTitle\":\"任务标题\",\"projectName\":\"项目名称\",\"oldStatus\":\"原状态\",\"newStatus\":\"新状态\",\"completionStatus\":\"完成状态\",\"dueDate\":\"截止日期\",\"currentDate\":\"当前日期\"}");
            template.setEnabled(true);
            template.setDescription("系统自动创建的任务状态变更通知模板");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            emailTemplateMapper.insert(template);
        }

        log.info("✅ TASK_STATUS_CHANGE模板配置完成");
    }

    private String getCorrectTodoReminderContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, Microsoft YaHei, sans-serif; background-color: #f5f7fa;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f5f7fa;'>" +
                "<tr>" +
                "<td align='center' style='padding: 20px;'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width: 600px; background-color: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>"
                +

                "<!-- 头部 -->" +
                "<tr>" +
                "<td style='padding: 30px; text-align: center; border-radius: 8px 8px 0 0; border: 2px solid #007ACC;'>"
                +
                "<h1 style='margin: 0; font-size: 24px; font-weight: bold; color: #000000 !important;'>📋 待办任务提醒</h1>" +
                "<p style='margin: 10px 0 0 0; font-size: 16px; color: #000000 !important;'>您有待办任务需要处理</p>" +
                "</td>" +
                "</tr>" +

                "<!-- 内容区域 -->" +
                "<tr>" +
                "<td style='padding: 30px; background-color: #ffffff;'>" +

                "<!-- 问候语 -->" +
                "<p style='font-size: 16px; color: #000000 !important; margin: 0 0 20px 0;'>您好！</p>" +

                "<!-- 统计概览 -->" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='border-radius: 8px; margin: 20px 0; border: 2px solid #4299e1;'>"
                +
                "<tr>" +
                "<td style='padding: 20px; text-align: center; background-color: #ffffff;'>" +
                "<h3 style='margin: 0 0 15px 0; font-size: 18px; color: #000000 !important;'>📊 任务概览</h3>" +
                "<div style='font-size: 24px; font-weight: bold; margin-bottom: 5px; color: #000000 !important;'>${todoCount}</div>"
                +
                "<div style='font-size: 14px; color: #000000 !important;'>个待办任务</div>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<!-- 任务详情标题 -->" +
                "<p style='font-size: 16px; color: #000000 !important; margin: 25px 0 20px 0;'>以下是您的待办任务提醒：</p>" +

                "<!-- 任务列表 -->" +
                "<div style='margin: 20px 0;'>" +
                "${taskListHtml}" +
                "</div>" +

                "</td>" +
                "</tr>" +

                "<!-- 底部 -->" +
                "<tr>" +
                "<td style='background-color: #ffffff; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; border-radius: 0 0 8px 8px;'>"
                +

                "<!-- 温馨提示 -->" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='border-radius: 6px; margin-bottom: 20px; border: 2px solid #81e6d9;'>"
                +
                "<tr>" +
                "<td style='padding: 15px; text-align: center; background-color: #ffffff;'>" +
                "<p style='margin: 0; color: #000000 !important; font-size: 14px; font-weight: bold;'>💡 温馨提示：请及时处理以上待办，确保项目进度</p>"
                +
                "<p style='margin: 5px 0 0 0; color: #000000 !important; font-size: 14px;'>系统地址：<a href='http://10.10.119.182:8016/login' style='color: #0066cc !important;'>http://10.10.119.182:8016/login</a></p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<p style='color: #000000 !important; font-size: 12px; margin: 0;'>此邮件由项目管理系统自动发送，请勿回复 | 发送时间：${currentDate}</p>"
                +
                "</td>" +
                "</tr>" +

                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String getCorrectDeadlineReminderContent() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin: 0; padding: 0; font-family: Arial, Microsoft YaHei, sans-serif; background-color: #f5f7fa;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f5f7fa;'><tr><td align='center' style='padding: 20px;'><table width='600' cellpadding='0' cellspacing='0' style='max-width: 600px; background-color: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'><tr><td style='background-color: #764ba2; color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0;'><h1 style='margin: 0; font-size: 24px; font-weight: bold;'>待办临期提醒</h1><p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;'>您有任务即将到期，请及时关注</p></td></tr><tr><td style='padding: 30px;'><p style='font-size: 16px; color: #333; margin: 0 0 20px 0;'>您好 <strong>${userName}</strong>！</p><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #3182ce; border-radius: 8px; margin: 20px 0;'><tr><td style='padding: 20px; text-align: center; color: white;'><h3 style='margin: 0 0 15px 0; font-size: 18px;'>📊 任务概览</h3><div style='font-size: 24px; font-weight: bold; margin-bottom: 5px;'>{{ Math.floor(taskCount) }}</div><div style='font-size: 14px; opacity: 0.9;'>个待处理任务</div></td></tr></table><h3 style='color: #333; margin: 25px 0 20px 0; font-size: 18px;'>📋 任务详情</h3><div style='margin: 20px 0;'>${taskListHtml}</div></td></tr><tr><td style='background-color: #f8f9fa; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; border-radius: 0 0 8px 8px;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #e6fffa; border: 1px solid #81e6d9; border-radius: 6px; margin-bottom: 20px;'><tr><td style='padding: 15px; text-align: center;'><p style='margin: 0; color: #234e52; font-size: 14px; font-weight: bold;'>💡 温馨提示：请及时处理，更新进度: </p></td><td style='padding: 15px; text-align: center;'><p style='margin: 0; color: #234e52; font-size: 14px; font-weight: bold;'>http://10.10.119.182:8016/login</p></td></tr></table><p style='color: #666; font-size: 12px; margin: 0;'>越南信息化专项 | ${currentDate}</p></td></tr></table></td></tr></table></body></html>;";
    }

    private String getTaskAssignmentContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, Microsoft YaHei, sans-serif; background-color: #f5f7fa;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f5f7fa;'>" +
                "<tr>" +
                "<td align='center' style='padding: 20px;'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width: 600px; background-color: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>"
                +
                "<tr>" +
                "<td style='background-color: #28a745; color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0;'>"
                +
                "<h1 style='margin: 0; font-size: 24px; font-weight: bold;'>📝 新任务分配通知</h1>" +
                "<p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;'>您有一个新的待办任务需要处理</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 30px;'>" +
                "<p style='font-size: 16px; color: #000000; margin: 0 0 20px 0;'>Dear <strong>${assigneeName}</strong>，</p>"
                +
                "<p style='font-size: 16px; color: #000000; margin: 0 0 25px 0;'>您有一个新的待办任务，详情如下：</p>" +
                "<h3 style='color: #000000; margin: 25px 0 20px 0; font-size: 18px;'>📋 任务详情</h3>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f8f9fa; border: 2px solid #28a745; border-radius: 8px; margin: 15px 0;'>"
                +
                "<tr>" +
                "<td style='padding: 20px;'>" +
                "<table width='100%' cellpadding='8' cellspacing='0'>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>所属项目：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>${projectName}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>任务标题：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px; font-weight: bold;'>${taskTitle}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>任务描述：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>${description}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>优先级：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'><span style='background-color: #ffc107; color: #212529; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;'>${priority}</span></td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>截止日期：</td>"
                +
                "<td style='color: #dc3545; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px; font-weight: bold;'>${dueDate}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; padding: 12px 8px;'>分配人：</td>"
                +
                "<td style='color: #000000; font-size: 14px; padding: 12px 8px;'>${assignedBy}</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='background-color: #f8f9fa; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; border-radius: 0 0 8px 8px;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #e8f5e8; border: 2px solid #28a745; border-radius: 6px; margin-bottom: 20px;'>"
                +
                "<tr>" +
                "<td style='padding: 15px; text-align: center;'>" +
                "<p style='margin: 0; color: #000000; font-size: 14px; font-weight: bold;'>💡 温馨提示：请及时处理，若已完成，请及时维护待办进度</p>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding-bottom: 15px; text-align: center;'>" +
                "<p style='margin: 0; color: #000000; font-size: 14px; font-weight: bold;'>http://10.10.119.182:8016/login</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #fff3cd; border: 2px solid #ffc107; border-radius: 6px; margin-bottom: 20px;'>"
                +
                "<tr>" +
                "<td style='padding: 15px; text-align: center;'>" +
                "<p style='margin: 0; color: #000000; font-size: 14px; font-weight: bold;'>📞 如有疑问，请联系：barlin.zhang</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<p style='color: #000000; font-size: 12px; margin: 0;'>越南职能信息化专项 | ${currentDate}</p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String getTaskStatusChangeContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, Microsoft YaHei, sans-serif; background-color: #f5f7fa;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f5f7fa;'>" +
                "<tr>" +
                "<td align='center' style='padding: 20px;'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width: 600px; background-color: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>"
                +
                "<tr>" +
                "<td style='background-color: #17a2b8; color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0;'>"
                +
                "<h1 style='margin: 0; font-size: 24px; font-weight: bold;'>🔄 任务状态更新通知</h1>" +
                "<p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;'>任务状态已发生变更，请及时关注</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 30px;'>" +
                "<p style='font-size: 16px; color: #000000; margin: 0 0 20px 0;'>Dear <strong>${assigneeName}</strong>，</p>"
                +
                "<p style='font-size: 16px; color: #000000; margin: 0 0 25px 0;'>您的任务状态已更新，详情如下：</p>" +
                "<h3 style='color: #000000; margin: 25px 0 20px 0; font-size: 18px;'>📊 更新详情</h3>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f8f9fa; border: 2px solid #17a2b8; border-radius: 8px; margin: 15px 0;'>"
                +
                "<tr>" +
                "<td style='padding: 20px;'>" +
                "<table width='100%' cellpadding='8' cellspacing='0'>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>更新人：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px; font-weight: bold;'>${assigneeName}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>所属项目：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>${projectName}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>任务标题：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px; font-weight: bold;'>${taskTitle}</td>"
                +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>状态变化：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>" +
                "<span style='background-color: #6c757d; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;'>${oldStatus}</span>"
                +
                "<span style='margin: 0 8px; color: #17a2b8; font-weight: bold;'>→</span>" +
                "<span style='background-color: #17a2b8; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;'>${newStatus}</span>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>完成状态：</td>"
                +
                "<td style='color: #000000; font-size: 14px; border-bottom: 1px solid #dee2e6; padding: 12px 8px;'>" +
                "<span style='background-color: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;'>${completionStatus}</span>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='width: 25%; font-weight: bold; color: #000000; font-size: 14px; padding: 12px 8px;'>截止日期：</td>"
                +
                "<td style='color: #dc3545; font-size: 14px; padding: 12px 8px; font-weight: bold;'>${dueDate}</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='background-color: #f8f9fa; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; border-radius: 0 0 8px 8px;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #d1ecf1; border: 2px solid #17a2b8; border-radius: 6px; margin-bottom: 20px;'>"
                +
                "<tr>" +
                "<td style='padding: 15px; text-align: center;'>" +
                "<p style='margin: 0; color: #000000; font-size: 14px; font-weight: bold;'>💡 温馨提示：请及时关注任务进展并维护进度</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding-bottom: 15px; text-align: center;'>" +
                "<p style='margin: 0; color: #000000; font-size: 14px; font-weight: bold;'>http://10.10.119.182:8016/login</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<p style='color: #000000; font-size: 12px; margin: 0;'>FROM：越南职能信息化专项 | ${currentDate}</p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }
}