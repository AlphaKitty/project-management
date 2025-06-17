package com.projectmanagement.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 待办任务DTO
 */
@Data
public class TodoDTO {

    private Long id;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "请选择所属项目")
    private Long projectId;

    @NotNull(message = "请选择负责人")
    private Long assigneeId;

    @NotBlank(message = "请选择优先级")
    private String priority;

    @NotBlank(message = "请选择任务状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Boolean emailEnabled; // 是否启用邮件通知，默认为true

    @NotNull(message = "创建者ID不能为空")
    private Long creatorId;
}