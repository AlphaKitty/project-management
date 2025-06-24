import request from "./request";
import type { ApiResponse } from "./request";
import type { WorkRecommendationSummary } from "@/types";

// 智能工作推荐API
export const workRecommendationApi = {
  // 获取工作推荐
  getRecommendations(): Promise<ApiResponse<WorkRecommendationSummary>> {
    return request.get("/work-recommendations");
  },

  // 标记推荐为已处理
  markAsHandled(recommendationId: string): Promise<ApiResponse<string>> {
    return request.put(`/work-recommendations/${recommendationId}/handled`);
  },

  // 忽略推荐
  ignoreRecommendation(recommendationId: string): Promise<ApiResponse<string>> {
    return request.put(`/work-recommendations/${recommendationId}/ignore`);
  },

  // 执行推荐操作
  executeRecommendation(
    recommendationId: string,
    actionData?: any
  ): Promise<ApiResponse<string>> {
    return request.post(
      `/work-recommendations/${recommendationId}/execute`,
      actionData
    );
  },
};
