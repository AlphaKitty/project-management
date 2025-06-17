package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todos")
public class Todo extends BaseEntity {

    private String title;
    private String description;
    private Long projectId;
    private Long assigneeId;
    private String priority;
    private String status;
    private LocalDate dueDate; // 截止日期只到天
    private LocalDateTime completedTime; // 完成时间需要精确时间
    private Boolean emailEnabled; // 是否启用邮件通知，默认为true
    private Long creatorId;

    @TableField(exist = false)
    private Project project;

    @TableField(exist = false)
    private User assignee;

    @TableField(exist = false)
    private User creator;
}