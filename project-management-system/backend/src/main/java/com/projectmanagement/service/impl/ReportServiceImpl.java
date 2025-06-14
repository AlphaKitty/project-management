package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.ReportDTO;
import com.projectmanagement.entity.Report;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.mapper.ProjectMapper;
import com.projectmanagement.mapper.ReportMapper;
import com.projectmanagement.mapper.TodoMapper;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报告服务实现类
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private TodoMapper todoMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Report> getReports() {
        return reportMapper.selectReportsWithDetails();
    }

    @Override
    public Report getReportById(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public List<Report> getReportsByProjectId(Long projectId) {
        return reportMapper.selectReportsByProjectId(projectId);
    }

    @Override
    public List<Report> getReportsByType(String type) {
        return reportMapper.selectReportsByType(type);
    }

    @Override
    public List<Report> getReportsByCreatorId(Long creatorId) {
        return reportMapper.selectReportsByCreatorId(creatorId);
    }

    @Override
    public Report generateReport(ReportDTO reportDTO) {
        Report report = new Report();

        // 如果有多个项目ID，使用第一个作为主项目（用于兼容）
        if (reportDTO.getProjectIds() != null && !reportDTO.getProjectIds().isEmpty()) {
            report.setProjectId(reportDTO.getProjectIds().get(0));
        } else if (reportDTO.getProjectId() != null) {
            report.setProjectId(reportDTO.getProjectId());
        }

        report.setType(reportDTO.getType());
        report.setTitle(reportDTO.getTitle());
        report.setReportDate(reportDTO.getReportDate());
        report.setCreatorId(reportDTO.getCreatorId());

        // 如果没有提供内容，生成默认内容
        if (StringUtils.hasText(reportDTO.getContent())) {
            report.setContent(reportDTO.getContent());
        } else {
            report.setContent(generateDefaultContent(reportDTO));
        }

        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());

        reportMapper.insert(report);
        return report;
    }

    @Override
    public Report updateReport(Long id, ReportDTO reportDTO) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("报告不存在");
        }

        if (reportDTO.getProjectId() != null) {
            report.setProjectId(reportDTO.getProjectId());
        }
        if (StringUtils.hasText(reportDTO.getType())) {
            report.setType(reportDTO.getType());
        }
        if (StringUtils.hasText(reportDTO.getTitle())) {
            report.setTitle(reportDTO.getTitle());
        }
        if (StringUtils.hasText(reportDTO.getContent())) {
            report.setContent(reportDTO.getContent());
        }
        if (reportDTO.getReportDate() != null) {
            report.setReportDate(reportDTO.getReportDate());
        }

        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return report;
    }

    @Override
    public boolean deleteReport(Long id) {
        return reportMapper.deleteById(id) > 0;
    }

    /**
     * 生成默认报告内容
     */
    private String generateDefaultContent(ReportDTO reportDTO) {
        String type = reportDTO.getType();
        LocalDate reportDate = reportDTO.getReportDate();
        List<Long> projectIds = reportDTO.getProjectIds();

        // 兼容旧的单项目ID
        if ((projectIds == null || projectIds.isEmpty()) && reportDTO.getProjectId() != null) {
            projectIds = Arrays.asList(reportDTO.getProjectId());
        }

        // 根据报告类型计算时间范围
        LocalDate startDate;
        LocalDate endDate = reportDate;

        switch (type) {
            case "WEEKLY":
                startDate = reportDate.minusDays(6);
                break;
            case "BIWEEKLY":
                startDate = reportDate.minusDays(13);
                break;
            case "MONTHLY":
                startDate = reportDate.withDayOfMonth(1);
                break;
            default:
                startDate = reportDate.minusDays(29);
                break;
        }

        // 查询项目相关的待办任务
        QueryWrapper<Todo> queryWrapper = new QueryWrapper<>();
        if (projectIds != null && !projectIds.isEmpty()) {
            queryWrapper.in("project_id", projectIds);
        }
        List<Todo> todos = todoMapper.selectList(queryWrapper);

        // 加载关联数据（项目和负责人信息）
        for (Todo todo : todos) {
            if (todo.getProjectId() != null) {
                todo.setProject(projectMapper.selectById(todo.getProjectId()));
            }
            if (todo.getAssigneeId() != null) {
                todo.setAssignee(userMapper.selectById(todo.getAssigneeId()));
            }
        }

        // 查询所有相关项目并按创建时间升序排序
        Map<Long, com.projectmanagement.entity.Project> projectMap = todos.stream()
                .map(Todo::getProject)
                .filter(p -> p != null)
                .distinct()
                .sorted((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null)
                        return 0;
                    if (a.getCreateTime() == null)
                        return 1;
                    if (b.getCreateTime() == null)
                        return -1;
                    return a.getCreateTime().compareTo(b.getCreateTime());
                })
                .collect(Collectors.toMap(
                        com.projectmanagement.entity.Project::getId,
                        p -> p,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));

        // 分类待办
        List<Todo> completedTodos = todos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()) &&
                        todo.getCompletedTime() != null &&
                        !todo.getCompletedTime().toLocalDate().isBefore(startDate) &&
                        !todo.getCompletedTime().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());
        List<Todo> inProgressTodos = todos.stream()
                .filter(todo -> "PROGRESS".equals(todo.getStatus()))
                .collect(Collectors.toList());
        List<Todo> todoTodos = todos.stream()
                .filter(todo -> "TODO".equals(todo.getStatus()))
                .collect(Collectors.toList());

        // 合并本期工作（已完成+进行中）
        List<Todo> currentTodos = new java.util.ArrayList<>();
        currentTodos.addAll(completedTodos);
        currentTodos.addAll(inProgressTodos);

        // 下期计划：所有未完成（非DONE）
        List<Todo> nextTodos = todos.stream()
                .filter(todo -> !"DONE".equals(todo.getStatus()))
                .collect(Collectors.toList());

        StringBuilder content = new StringBuilder();
        switch (type) {
            case "WEEKLY":
                content.append("## 周报（").append(startDate).append(" - ").append(endDate).append("）\n\n");
                break;
            case "BIWEEKLY":
                content.append("## 双周报（").append(startDate).append(" - ").append(endDate).append("）\n\n");
                break;
            case "MONTHLY":
                content.append("## 月报（").append(reportDate.getYear()).append("年").append(reportDate.getMonthValue())
                        .append("月）\n\n");
                break;
            default:
                content.append("## 阶段报告（").append(startDate).append(" - ").append(endDate).append("）\n\n");
                break;
        }

        // 本期工作
        content.append("### 本期工作\n");
        if (!currentTodos.isEmpty()) {
            for (com.projectmanagement.entity.Project project : projectMap.values()) {
                List<Todo> projectTodos = currentTodos.stream()
                        .filter(todo -> project.getId().equals(todo.getProjectId()))
                        .sorted(this::compareTodos)
                        .collect(Collectors.toList());
                if (!projectTodos.isEmpty()) {
                    content.append("\n#### ").append(project.getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        content.append("- ")
                                .append(getPriorityLabel(todo.getPriority()))
                                .append(getStatusLabel(todo.getStatus()))
                                .append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        if ("DONE".equals(todo.getStatus()) && todo.getCompletedTime() != null) {
                            content.append(" - 完成时间：").append(todo.getCompletedTime().toLocalDate());
                        } else if (todo.getDueDate() != null) {
                            content.append(" - 截止：").append(todo.getDueDate());
                        }
                        if (todo.getDueDate() != null) {
                            java.time.LocalDate due = todo.getDueDate();
                            if ("DONE".equals(todo.getStatus()) && todo.getCompletedTime() != null) {
                                java.time.LocalDate completed = todo.getCompletedTime().toLocalDate();
                                long diff = java.time.temporal.ChronoUnit.DAYS.between(completed, due);
                                if (diff > 0) {
                                    content.append("，提前").append(diff).append("天完成");
                                } else if (diff == 0) {
                                    content.append("，按时完成");
                                } else {
                                    content.append("，逾期").append(Math.abs(diff)).append("天完成");
                                }
                            } else {
                                long diff = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), due);
                                if (diff > 0) {
                                    content.append("，剩余").append(diff).append("天");
                                } else if (diff == 0) {
                                    content.append("，今天截止");
                                } else {
                                    content.append("，已逾期").append(Math.abs(diff)).append("天");
                                }
                            }
                        }
                        content.append("\n");
                    }
                }
            }
        } else {
            content.append("- 暂无本期工作\n");
        }
        content.append("\n");

        // 下期计划
        String nextPeriod = type.equals("WEEKLY") ? "下周"
                : type.equals("BIWEEKLY") ? "下两周" : type.equals("MONTHLY") ? "下月" : "下阶段";
        content.append("### ").append(nextPeriod).append("计划\n");
        if (!nextTodos.isEmpty()) {
            for (com.projectmanagement.entity.Project project : projectMap.values()) {
                List<Todo> projectTodos = nextTodos.stream()
                        .filter(todo -> project.getId().equals(todo.getProjectId()))
                        .sorted(this::compareTodos)
                        .collect(Collectors.toList());
                if (!projectTodos.isEmpty()) {
                    content.append("\n#### ").append(project.getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        content.append("- ")
                                .append(getPriorityLabel(todo.getPriority()))
                                .append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        if (todo.getDueDate() != null) {
                            content.append(" - 截止：").append(todo.getDueDate());
                            java.time.LocalDate due = todo.getDueDate();
                            long diff = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), due);
                            if (diff > 0) {
                                content.append("，剩余").append(diff).append("天");
                            } else if (diff == 0) {
                                content.append("，今天截止");
                            } else {
                                content.append("，已逾期").append(Math.abs(diff)).append("天");
                            }
                        }
                        content.append("\n");
                    }
                }
            }
        } else {
            content.append("- 待制定\n");
        }

        return content.toString();
    }

    // 优先级标签
    private String getPriorityLabel(String priority) {
        switch (priority) {
            case "HIGH":
                return "【高】";
            case "MEDIUM":
                return "【中】";
            case "LOW":
                return "【低】";
            default:
                return "";
        }
    }

    // 状态标签
    private String getStatusLabel(String status) {
        switch (status) {
            case "PROGRESS":
                return "【进行中】";
            case "DONE":
                return "【已完成】";
            case "TODO":
                return "【待办】";
            default:
                return "";
        }
    }

    /**
     * 比较两个待办任务（先按优先级从高到低，再按截止时间从早到晚）
     */
    private int compareTodos(Todo a, Todo b) {
        // 先按优先级排序
        int priorityCompare = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // 优先级相同时，按截止日期排序
        if (a.getDueDate() != null && b.getDueDate() != null) {
            return a.getDueDate().compareTo(b.getDueDate());
        } else if (a.getDueDate() != null) {
            return -1; // 有截止日期的排在前面
        } else if (b.getDueDate() != null) {
            return 1;
        }

        return 0;
    }

    private int getPriorityOrder(String priority) {
        switch (priority) {
            case "HIGH":
                return 3;
            case "MEDIUM":
                return 2;
            case "LOW":
                return 1;
            default:
                return 0;
        }
    }
}