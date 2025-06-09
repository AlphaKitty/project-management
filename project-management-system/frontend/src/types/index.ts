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
  createTime: string;
  updateTime: string;
  // 关联数据
  creator?: User;
  assignee?: User;
  members?: User[];
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
