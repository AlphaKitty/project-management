package com.projectmanagement.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkRecommendationDTO {

    /**
     * 单个推荐项
     */
    @Data
    public static class RecommendationItem {
        private String id;
        private String type; // URGENT, STAGNANT, PROGRESS, COLLABORATION, RISK, SUGGESTION
        private String title;
        private String description;
        private Long projectId;
        private Long todoId;
        private String priority; // HIGH, MEDIUM, LOW
        private String actionType; // CREATE_TODO, UPDATE_PROJECT, REVIEW, DISCUSSION, VIEW_PROJECT
        private Object actionData;
        private LocalDateTime createTime;
    }

    /**
     * 推荐汇总
     */
    @Data
    public static class RecommendationSummary {
        private List<RecommendationItem> urgent;
        private List<RecommendationItem> stagnant;
        private List<RecommendationItem> progress;
        private List<RecommendationItem> collaboration;
        private List<RecommendationItem> risk;
        private List<RecommendationItem> suggestions;
        private Integer totalCount;
    }
}