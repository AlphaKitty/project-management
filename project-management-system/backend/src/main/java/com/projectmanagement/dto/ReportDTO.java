package com.projectmanagement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 报告DTO
 */
@Data
public class ReportDTO {

    private Long projectId;

    private List<Long> projectIds; // 支持多个项目ID

    private String type;

    private String title;

    private String content;

    private LocalDate reportDate;

    private Long creatorId;

    private Boolean fuzzyMode = true; // 模糊模式开关，默认开启
}