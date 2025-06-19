package com.projectmanagement.controller;

import com.projectmanagement.common.ApiResponse;
import com.projectmanagement.dto.EmailTemplateDTO;
import com.projectmanagement.entity.EmailTemplate;
import com.projectmanagement.annotation.OperationLog;
import com.projectmanagement.enums.BusinessModule;
import com.projectmanagement.enums.OperationType;
import com.projectmanagement.service.EmailTemplateService;
// import io.swagger.annotations.Api;
// import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 邮件模板管理控制器
 */
@RestController
@RequestMapping("/email-templates")
@RequiredArgsConstructor
// @Api(tags = "邮件模板管理", description = "邮件模板的增删改查和发送功能")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    /**
     * 获取邮件模板列表
     */
    @GetMapping
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.EMAIL_TEMPLATE, description = "查询邮件模板列表")
    // @ApiOperation(value = "获取邮件模板列表")
    public ApiResponse<List<EmailTemplate>> getTemplateList() {
        List<EmailTemplate> templates = emailTemplateService.getTemplateList();
        return ApiResponse.success(templates);
    }

    /**
     * 根据ID获取邮件模板
     */
    @GetMapping("/{id}")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.EMAIL_TEMPLATE, description = "查询邮件模板详情")
    // @ApiOperation(value = "根据ID获取邮件模板详情")
    public ApiResponse<EmailTemplate> getTemplateById(@PathVariable Long id) {
        EmailTemplate template = emailTemplateService.getById(id);
        if (template == null) {
            return ApiResponse.error("邮件模板不存在");
        }
        return ApiResponse.success(template);
    }

    /**
     * 根据模板代码获取邮件模板
     */
    @GetMapping("/code/{templateCode}")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.EMAIL_TEMPLATE, description = "根据代码查询邮件模板")
    // @ApiOperation(value = "根据模板代码获取邮件模板")
    public ApiResponse<EmailTemplate> getTemplateByCode(@PathVariable String templateCode) {
        EmailTemplate template = emailTemplateService.getTemplateByCode(templateCode);
        if (template == null) {
            return ApiResponse.error("邮件模板不存在");
        }
        return ApiResponse.success(template);
    }

    /**
     * 创建邮件模板
     */
    @PostMapping
    @OperationLog(type = OperationType.CREATE, module = BusinessModule.EMAIL_TEMPLATE, description = "创建邮件模板")
    // @ApiOperation(value = "创建邮件模板")
    public ApiResponse<EmailTemplate> createTemplate(@RequestBody EmailTemplateDTO templateDTO) {
        try {
            EmailTemplate template = emailTemplateService.createTemplate(templateDTO);
            return ApiResponse.success(template);
        } catch (Exception e) {
            return ApiResponse.error("创建邮件模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新邮件模板
     */
    @PutMapping("/{id}")
    @OperationLog(type = OperationType.UPDATE, module = BusinessModule.EMAIL_TEMPLATE, description = "更新邮件模板")
    // @ApiOperation(value = "更新邮件模板")
    public ApiResponse<EmailTemplate> updateTemplate(@PathVariable Long id, @RequestBody EmailTemplateDTO templateDTO) {
        try {
            EmailTemplate template = emailTemplateService.updateTemplate(id, templateDTO);
            return ApiResponse.success(template);
        } catch (Exception e) {
            return ApiResponse.error("更新邮件模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除邮件模板
     */
    @DeleteMapping("/{id}")
    @OperationLog(type = OperationType.DELETE, module = BusinessModule.EMAIL_TEMPLATE, description = "删除邮件模板")
    // @ApiOperation(value = "删除邮件模板")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        try {
            boolean success = emailTemplateService.deleteTemplate(id);
            if (success) {
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error("删除邮件模板失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("删除邮件模板失败: " + e.getMessage());
        }
    }

    /**
     * 预览邮件模板
     */
    @PostMapping("/{templateCode}/preview")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.EMAIL_TEMPLATE, description = "预览邮件模板")
    // @ApiOperation(value = "预览邮件模板渲染结果")
    public ApiResponse<Map<String, String>> previewTemplate(
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> variables) {
        try {
            Map<String, String> rendered = emailTemplateService.renderTemplate(templateCode, variables);
            return ApiResponse.success(rendered);
        } catch (Exception e) {
            return ApiResponse.error("模板预览失败: " + e.getMessage());
        }
    }

    /**
     * 发送模板邮件
     */
    @PostMapping("/{templateCode}/send")
    @OperationLog(type = OperationType.CREATE, module = BusinessModule.EMAIL_TEMPLATE, description = "发送模板邮件")
    // @ApiOperation(value = "发送模板邮件")
    public ApiResponse<Void> sendTemplateEmail(
            @PathVariable String templateCode,
            @RequestParam String to,
            @RequestBody Map<String, Object> variables) {
        try {
            boolean success = emailTemplateService.sendTemplateEmail(templateCode, to, variables);
            if (success) {
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error("邮件发送失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("邮件发送失败: " + e.getMessage());
        }
    }
}