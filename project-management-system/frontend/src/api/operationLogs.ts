import request from "./request";

// 操作日志接口
export interface OperationLog {
  id: number;
  userId: number;
  username: string;
  operationType: string;
  module: string;
  description: string;
  requestUrl: string;
  requestMethod: string;
  requestParams?: string;
  response?: string;
  success: boolean;
  errorMessage?: string;
  duration: number;
  ipAddress: string;
  userAgent: string;
  operationTime: string;
  createTime: string;
}

// 分页查询参数
export interface OperationLogQuery {
  current: number;
  size: number;
  username?: string;
  module?: string;
  operationType?: string;
  success?: boolean;
  startTime?: string;
  endTime?: string;
}

// MyBatis-Plus分页响应
export interface IPage<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}

// 统计数据
export interface OperationStats {
  totalCount: number;
  successCount: number;
  failCount: number;
  operationTypeStats: Array<{
    operationType: string;
    count: number;
    percentage: number;
  }>;
  moduleStats: Array<{
    module: string;
    count: number;
    percentage: number;
  }>;
}

/**
 * 分页查询操作日志（管理员）
 */
export const getOperationLogs = (params: OperationLogQuery) => {
  return request.get<IPage<OperationLog>>("/operation-logs", { params });
};

/**
 * 获取个人操作日志
 */
export const getMyOperationLogs = (limit = 50) => {
  return request.get<OperationLog[]>("/operation-logs/my-logs", {
    params: { limit },
  });
};

/**
 * 获取操作统计数据（管理员）
 */
export const getOperationStats = (params?: {
  startTime?: string;
  endTime?: string;
}) => {
  return request.get<OperationStats>("/operation-logs/stats", { params });
};

/**
 * 清理过期日志（管理员）
 */
export const cleanExpiredLogs = (keepDays = 30) => {
  return request.delete<string>("/operation-logs/clean", {
    params: { keepDays },
  });
};
