package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.dto.WorkRecommendationDTO;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.WorkRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 智能工作推荐控制器
 */
@Slf4j
@RestController
@RequestMapping("/work-recommendations")
@RequiredArgsConstructor
public class WorkRecommendationController {

    private final WorkRecommendationService workRecommendationService;

    /**
     * 获取工作推荐
     */
    @GetMapping
    public Result<WorkRecommendationDTO.RecommendationSummary> getRecommendations(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        try {
            WorkRecommendationDTO.RecommendationSummary recommendations = workRecommendationService
                    .getUserRecommendations(currentUser.getId());
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取工作推荐失败", e);
            return Result.error("获取工作推荐失败");
        }
    }

    /**
     * 执行推荐操作
     */
    @PostMapping("/{recommendationId}/execute")
    public Result<String> executeRecommendation(
            @PathVariable String recommendationId,
            @RequestBody(required = false) Object actionData,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = workRecommendationService.executeRecommendation(
                    recommendationId, currentUser.getId(), actionData);
            return success ? Result.success("操作成功") : Result.error("操作失败");
        } catch (Exception e) {
            log.error("执行推荐操作失败", e);
            return Result.error("执行推荐操作失败");
        }
    }

    /**
     * 标记推荐为已处理
     */
    @PutMapping("/{recommendationId}/handled")
    public Result<String> markAsHandled(@PathVariable String recommendationId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = workRecommendationService.markAsHandled(recommendationId, currentUser.getId());
            return success ? Result.success("标记成功") : Result.error("标记失败");
        } catch (Exception e) {
            log.error("标记推荐失败", e);
            return Result.error("标记推荐失败");
        }
    }

    /**
     * 忽略推荐
     */
    @PutMapping("/{recommendationId}/ignore")
    public Result<String> ignoreRecommendation(@PathVariable String recommendationId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = workRecommendationService.ignoreRecommendation(recommendationId, currentUser.getId());
            return success ? Result.success("忽略成功") : Result.error("忽略失败");
        } catch (Exception e) {
            log.error("忽略推荐失败", e);
            return Result.error("忽略推荐失败");
        }
    }
}