package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.dto.ReportDTO;
import com.projectmanagement.entity.Report;
import com.projectmanagement.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报告管理控制器
 */
@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取报告列表
     */
    @GetMapping
    public Result<List<Report>> getReports(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long creatorId) {

        List<Report> reports;

        if (projectId != null) {
            reports = reportService.getReportsByProjectId(projectId);
        } else if (type != null) {
            reports = reportService.getReportsByType(type);
        } else if (creatorId != null) {
            reports = reportService.getReportsByCreatorId(creatorId);
        } else {
            reports = reportService.getReports();
        }

        return Result.success(reports);
    }

    /**
     * 根据ID获取报告详情
     */
    @GetMapping("/{id}")
    public Result<Report> getReport(@PathVariable Long id) {
        Report report = reportService.getReportById(id);
        if (report == null) {
            return Result.error("报告不存在");
        }
        return Result.success(report);
    }

    /**
     * 生成报告
     */
    @PostMapping("/generate")
    public Result<Report> generateReport(@RequestBody ReportDTO reportDTO) {
        try {
            Report report = reportService.generateReport(reportDTO);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 更新报告
     */
    @PutMapping("/{id}")
    public Result<Report> updateReport(@PathVariable Long id, @RequestBody ReportDTO reportDTO) {
        try {
            Report report = reportService.updateReport(id, reportDTO);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error("更新报告失败: " + e.getMessage());
        }
    }

    /**
     * 删除报告
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteReport(@PathVariable Long id) {
        try {
            boolean success = reportService.deleteReport(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 按类型统计报告数量
     */
    @GetMapping("/stats")
    public Result<Object> getReportStats() {
        try {
            List<Report> weeklyReports = reportService.getReportsByType("WEEKLY");
            List<Report> biweeklyReports = reportService.getReportsByType("BIWEEKLY");
            List<Report> monthlyReports = reportService.getReportsByType("MONTHLY");
            List<Report> stageReports = reportService.getReportsByType("STAGE");

            return Result.success(new Object() {
                public final int weekly = weeklyReports.size();
                public final int biweekly = biweeklyReports.size();
                public final int monthly = monthlyReports.size();
                public final int stage = stageReports.size();
                public final int total = weekly + biweekly + monthly + stage;
            });
        } catch (Exception e) {
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
}