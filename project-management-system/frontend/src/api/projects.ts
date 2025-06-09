import request from "./request";
import type { ApiResponse } from "./request";
import type { Project, ProjectDTO } from "@/types";

// 项目API
export const projectApi = {
  // 获取项目列表
  getProjects(): Promise<ApiResponse<Project[]>> {
    return request.get("/projects");
  },

  // 获取项目详情
  getProject(id: number): Promise<ApiResponse<Project>> {
    return request.get(`/projects/${id}`);
  },

  // 创建项目
  createProject(data: ProjectDTO): Promise<ApiResponse<Project>> {
    return request.post("/projects", data);
  },

  // 更新项目
  updateProject(id: number, data: ProjectDTO): Promise<ApiResponse<Project>> {
    return request.put(`/projects/${id}`, data);
  },

  // 删除项目
  deleteProject(id: number): Promise<ApiResponse<string>> {
    return request.delete(`/projects/${id}`);
  },

  // 获取用户参与的项目
  getUserProjects(userId: number): Promise<ApiResponse<Project[]>> {
    return request.get(`/projects/user/${userId}`);
  },

  // 更新项目进度
  updateProgress(id: number, progress: number): Promise<ApiResponse<string>> {
    return request.put(`/projects/${id}/progress`, null, {
      params: { progress },
    });
  },

  // 添加项目成员
  addMember(projectId: number, userId: number): Promise<ApiResponse<string>> {
    return request.post(`/projects/${projectId}/members`, null, {
      params: { userId },
    });
  },

  // 移除项目成员
  removeMember(
    projectId: number,
    userId: number
  ): Promise<ApiResponse<string>> {
    return request.delete(`/projects/${projectId}/members/${userId}`);
  },
};
