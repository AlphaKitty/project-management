package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 项目报告实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reports")
public class Report extends BaseEntity {

    private Long projectId;
    private String type;
    private String title;
    private String content;
    private LocalDate reportDate;
    private Long creatorId;

    @TableField(exist = false)
    private Project project;

    @TableField(exist = false)
    private User creator;
} 