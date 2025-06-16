package com.projectmanagement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目DTO
 */
@Data
public class ProjectDTO {

    private Long id;
    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private Long creatorId;
    private Long assigneeId;
    private List<Long> memberIds;
    private String milestones;
    private String thisWeekWork;
    private String nextWeekPlan;
}