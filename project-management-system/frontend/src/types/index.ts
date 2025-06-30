// 用户信息
export interface User {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone?: string;
  avatar?: string;
  status: number;
  createTime: string;
  updateTime: string;
}

// 里程碑信息
export interface Milestone {
  name: string;
  status: "PENDING" | "PROGRESS" | "COMPLETED";
  dueDate?: string;
  description?: string;
  completedDate?: string;
}

// 项目信息
export interface Project {
  id: number;
  name: string;
  description?: string;
  status: "PENDING" | "PROGRESS" | "COMPLETED" | "CANCELLED";
  startDate?: string;
  endDate?: string;
  progress: number;
  creatorId: number;
  assigneeId?: number;
  milestones?: string; // JSON字符串格式的里程碑数据
  thisWeekWork?: string; // 本周工作内容
  nextWeekPlan?: string; // 下周计划内容
  createTime: string;
  updateTime: string;
  // 关联数据
  creator?: User;
  assignee?: User;
  members?: User[];
  // 解析后的里程碑数据
  milestonesData?: Milestone[];
}

// 项目DTO（用于创建/更新）
export interface ProjectDTO {
  id?: number;
  name: string;
  description?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  progress?: number;
  creatorId?: number;
  assigneeId?: number;
  memberIds?: number[];
  milestones?: string;
  thisWeekWork?: string;
  nextWeekPlan?: string;
}

// 待办任务
export interface Todo {
  id: number;
  title: string;
  description?: string;
  projectId?: number;
  assigneeId?: number;
  priority: "LOW" | "MEDIUM" | "HIGH";
  status: "TODO" | "PROGRESS" | "DONE";
  dueDate?: string; // 格式：YYYY-MM-DD
  completedTime?: string;
  creatorId: number;
  createTime: string;
  updateTime: string;
  // 关联数据
  project?: Project;
  assignee?: User;
  creator?: User;
}

// 任务DTO（用于创建/更新）
export interface TodoDTO {
  id?: number;
  title: string;
  description?: string;
  projectId?: number;
  assigneeId?: number;
  priority?: string;
  status?: string;
  dueDate?: string; // 格式：YYYY-MM-DD
  emailEnabled?: boolean; // 是否启用邮件通知
  creatorId: number;
}

// 项目报告
export interface Report {
  id: number;
  projectId?: number;
  type: "WEEKLY" | "BIWEEKLY" | "MONTHLY" | "STAGE";
  title: string;
  content?: string;
  reportDate: string; // 格式：YYYY-MM-DD
  creatorId: number;
  createTime: string;
  updateTime: string;
  // 关联数据
  project?: Project;
  creator?: User;
}

// 报告DTO（用于创建）
export interface ReportDTO {
  id?: number;
  projectId?: number;
  projectIds?: number[];
  type: "WEEKLY" | "BIWEEKLY" | "MONTHLY" | "STAGE";
  title: string;
  content?: string;
  reportDate: string;
  creatorId: number;
  fuzzyMode?: boolean; // 模糊模式开关，默认true
}

// 状态标签映射
export const StatusLabels = {
  // 项目状态
  PENDING: "待启动",
  PROGRESS: "进行中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",

  // 任务优先级
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",

  // 任务状态
  TODO: "待办",
  DONE: "已完成",

  // 报告类型
  WEEKLY: "周报",
  MONTHLY: "月报",
  STAGE: "阶段报告",
};

// 状态颜色映射
export const StatusColors = {
  // 项目状态
  PENDING: "orange",
  PROGRESS: "blue",
  COMPLETED: "green",
  CANCELLED: "red",

  // 任务优先级
  LOW: "green",
  MEDIUM: "orange",
  HIGH: "red",

  // 任务状态
  TODO: "orange",
  DONE: "green",
};

// 智能工作推荐相关类型
export interface WorkRecommendation {
  id: string;
  type:
    | "URGENT"
    // | "STAGNANT" // 项目停滞功能已移除
    | "PROGRESS"
    | "COLLABORATION"
    | "RISK"
    | "SUGGESTION";
  title: string;
  description: string;
  projectId?: number;
  todoId?: number;
  priority: "HIGH" | "MEDIUM" | "LOW";
  actionType:
    | "CREATE_TODO"
    | "UPDATE_PROJECT"
    | "REVIEW"
    | "DISCUSSION"
    | "VIEW_PROJECT"
    | "VIEW_TODO";
  actionData?: any;
  createTime: string;
}

export interface WorkRecommendationSummary {
  urgent: WorkRecommendation[];
  stagnant: WorkRecommendation[];
  progress: WorkRecommendation[];
  collaboration: WorkRecommendation[];
  risk: WorkRecommendation[];
  suggestions: WorkRecommendation[];
  totalCount: number;
}

// 推荐类型标签映射
export const RecommendationTypeLabels = {
  URGENT: "紧急推进",
  // STAGNANT: "项目停滞", // 项目停滞功能已移除
  PROGRESS: "项目推进",
  COLLABORATION: "协作待办",
  RISK: "风险预警",
  SUGGESTION: "智能建议",
};

// 推荐类型图标映射
export const RecommendationTypeIcons = {
  URGENT: "🔥",
  // STAGNANT: "🛑", // 项目停滞功能已移除
  PROGRESS: "⚡",
  COLLABORATION: "🤝",
  RISK: "⚠️",
  SUGGESTION: "💡",
};
