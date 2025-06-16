package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("projects")
public class Project extends BaseEntity {

    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private Long creatorId;
    private Long assigneeId;

    /**
     * 里程碑节点，JSON格式存储
     * 格式: [{"name": "需求分析", "status": "COMPLETED", "dueDate": "2024-01-15",
     * "description": "完成需求分析"}]
     */
    private String milestones;

    /**
     * 本周工作内容
     */
    private String thisWeekWork;

    /**
     * 下周计划内容
     */
    private String nextWeekPlan;

    @TableField(exist = false)
    private List<User> members;

    @TableField(exist = false)
    private User creator;

    @TableField(exist = false)
    private User assignee;
}