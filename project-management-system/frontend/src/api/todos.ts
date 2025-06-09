import request from "./request";
import type { ApiResponse } from "./request";
import type { Todo, TodoDTO } from "@/types";

// 待办任务API
export const todoApi = {
  // 获取待办任务列表
  getTodos(): Promise<ApiResponse<Todo[]>> {
    return request.get("/todos");
  },

  // 获取今日待办任务
  getTodayTodos(): Promise<ApiResponse<Todo[]>> {
    return request.get("/todos/today");
  },

  // 获取本周待办任务
  getWeekTodos(): Promise<ApiResponse<Todo[]>> {
    return request.get("/todos/week");
  },

  // 获取用户的待办任务
  getUserTodos(userId: number): Promise<ApiResponse<Todo[]>> {
    return request.get(`/todos/user/${userId}`);
  },

  // 获取待办任务详情
  getTodo(id: number): Promise<ApiResponse<Todo>> {
    return request.get(`/todos/${id}`);
  },

  // 创建待办任务
  createTodo(data: TodoDTO): Promise<ApiResponse<Todo>> {
    return request.post("/todos", data);
  },

  // 更新待办任务
  updateTodo(id: number, data: TodoDTO): Promise<ApiResponse<Todo>> {
    return request.put(`/todos/${id}`, data);
  },

  // 删除待办任务
  deleteTodo(id: number): Promise<ApiResponse<string>> {
    return request.delete(`/todos/${id}`);
  },

  // 更新任务状态
  updateStatus(id: number, status: string): Promise<ApiResponse<string>> {
    return request.put(`/todos/${id}/status`, null, {
      params: { status },
    });
  },

  // 发送待办任务邮件
  sendEmail(email: string, userId?: number): Promise<ApiResponse<string>> {
    const params: any = { email };
    if (userId) {
      params.userId = userId;
    }
    return request.post("/todos/send-email", null, { params });
  },
};
