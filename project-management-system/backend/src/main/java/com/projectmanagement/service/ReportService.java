package com.projectmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.projectmanagement.dto.ReportDTO;
import com.projectmanagement.entity.Report;

import java.util.List;

/**
 * 报告服务接口
 */
public interface ReportService extends IService<Report> {

    /**
     * 查询报告列表
     */
    List<Report> getReports();

    /**
     * 根据ID查询报告
     */
    Report getReportById(Long id);

    /**
     * 根据项目ID查询报告列表
     */
    List<Report> getReportsByProjectId(Long projectId);

    /**
     * 根据类型查询报告列表
     */
    List<Report> getReportsByType(String type);

    /**
     * 根据创建人查询报告列表
     */
    List<Report> getReportsByCreatorId(Long creatorId);

    /**
     * 生成报告
     */
    Report generateReport(ReportDTO reportDTO);

    /**
     * 更新报告
     */
    Report updateReport(Long id, ReportDTO reportDTO);

    /**
     * 删除报告
     */
    boolean deleteReport(Long id);
} 