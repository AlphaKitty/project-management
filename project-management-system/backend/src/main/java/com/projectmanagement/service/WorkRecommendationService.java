package com.projectmanagement.service;

import com.projectmanagement.dto.WorkRecommendationDTO;

/**
 * 智能工作推荐服务
 */
public interface WorkRecommendationService {

    /**
     * 获取用户的工作推荐
     * 
     * @param userId 用户ID
     * @return 推荐汇总
     */
    WorkRecommendationDTO.RecommendationSummary getUserRecommendations(Long userId);

    /**
     * 执行推荐操作
     * 
     * @param recommendationId 推荐ID
     * @param userId           用户ID
     * @param actionData       操作数据
     * @return 执行结果
     */
    boolean executeRecommendation(String recommendationId, Long userId, Object actionData);

    /**
     * 标记推荐为已处理
     * 
     * @param recommendationId 推荐ID
     * @param userId           用户ID
     * @return 处理结果
     */
    boolean markAsHandled(String recommendationId, Long userId);

    /**
     * 忽略推荐
     * 
     * @param recommendationId 推荐ID
     * @param userId           用户ID
     * @return 处理结果
     */
    boolean ignoreRecommendation(String recommendationId, Long userId);
}