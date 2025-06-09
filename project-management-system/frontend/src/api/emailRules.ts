import request from "./request";
import type { ApiResponse } from "./request";

export interface EmailRule {
  id?: number;
  ruleName: string;
  ruleType: string;
  triggerCondition: string;
  emailTemplateCode: string;
  enabled: boolean;
  priority: number;
  maxFrequency: string;
  businessHoursOnly: boolean;
  excludeWeekends: boolean;
  targetRoles: string;
  description: string;
  creatorId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface UserEmailPreference {
  id?: number;
  userId: number;
  enableEmail: boolean;
  deadlineReminder: boolean;
  statusChange: boolean;
  taskAssignment: boolean;
  overdueTask: boolean;
  dailySummary: boolean;
  weeklySummary: boolean;
  monthlySummary: boolean;
  dailySummaryTime: string;
  weeklyDay: number;
  monthlyDay: number;
  urgentOnly: boolean;
  quietStart: string;
  quietEnd: string;
  maxEmailsPerDay: number;
  createTime?: string;
  updateTime?: string;
}

export interface EmailRuleQuery {
  current?: number;
  size?: number;
  ruleType?: string;
  enabled?: boolean;
}

export interface PageResult<T> {
  records: T[];
  current: number;
  size: number;
  total: number;
  pages: number;
}

// 邮件规则管理API
export const emailRuleApi = {
  // 分页查询邮件规则
  getEmailRules: (
    params: EmailRuleQuery
  ): Promise<ApiResponse<PageResult<EmailRule>>> => {
    return request.get("/email-rules", { params });
  },

  // 根据ID获取邮件规则
  getEmailRuleById: (id: number): Promise<ApiResponse<EmailRule>> => {
    return request.get(`/email-rules/${id}`);
  },

  // 创建邮件规则
  createEmailRule: (data: EmailRule): Promise<ApiResponse<string>> => {
    return request.post("/email-rules", data);
  },

  // 更新邮件规则
  updateEmailRule: (
    id: number,
    data: EmailRule
  ): Promise<ApiResponse<string>> => {
    return request.put(`/email-rules/${id}`, data);
  },

  // 删除邮件规则
  deleteEmailRule: (id: number): Promise<ApiResponse<string>> => {
    return request.delete(`/email-rules/${id}`);
  },

  // 启用/禁用邮件规则
  toggleEmailRule: (
    id: number,
    enabled: boolean
  ): Promise<ApiResponse<string>> => {
    return request.put(`/email-rules/${id}/toggle`, null, {
      params: { enabled },
    });
  },

  // 获取当前用户的邮件偏好设置
  getUserEmailPreference: (): Promise<ApiResponse<UserEmailPreference>> => {
    return request.get("/email-rules/preferences");
  },

  // 更新当前用户的邮件偏好设置
  updateUserEmailPreference: (
    data: UserEmailPreference
  ): Promise<ApiResponse<string>> => {
    return request.put("/email-rules/preferences", data);
  },

  // 获取规则类型列表
  getRuleTypes: (): Promise<ApiResponse<string[]>> => {
    return request.get("/email-rules/rule-types");
  },

  // 获取邮件模板列表
  getEmailTemplates: (): Promise<ApiResponse<string[]>> => {
    return request.get("/email-rules/email-templates");
  },

  // 测试执行邮件规则
  testEmailRule: (id: number): Promise<ApiResponse<string>> => {
    return request.post(`/email-rules/test-rule/${id}`);
  },
};
