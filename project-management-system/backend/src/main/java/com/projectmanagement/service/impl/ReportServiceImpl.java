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
                // 周报：从报告日期往前推7天
                startDate = reportDate.minusDays(6);
                break;
            case "BIWEEKLY":
                // 双周报：从报告日期往前推14天
                startDate = reportDate.minusDays(13);
                break;
            case "MONTHLY":
                // 月报：从报告日期往前推到月初
                startDate = reportDate.withDayOfMonth(1);
                break;
            default:
                // 阶段报告：默认30天
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

        // 根据状态和时间分类待办任务
        List<Todo> completedTodos = todos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()) &&
                        todo.getCompletedTime() != null &&
                        !todo.getCompletedTime().toLocalDate().isBefore(startDate) &&
                        !todo.getCompletedTime().toLocalDate().isAfter(endDate))
                .sorted((a, b) -> a.getCompletedTime().compareTo(b.getCompletedTime())) // 按完成时间从早到晚排序
                .collect(Collectors.toList());

        List<Todo> inProgressTodos = todos.stream()
                .filter(todo -> "PROGRESS".equals(todo.getStatus()))
                .collect(Collectors.toList());

        List<Todo> overdueTodos = inProgressTodos.stream()
                .filter(todo -> todo.getDueDate() != null &&
                        todo.getDueDate().isBefore(LocalDate.now()))
                .sorted(this::compareTodos) // 按优先级和截止时间排序
                .collect(Collectors.toList());

        List<Todo> normalProgressTodos = inProgressTodos.stream()
                .filter(todo -> todo.getDueDate() == null ||
                        !todo.getDueDate().isBefore(LocalDate.now()))
                .sorted(this::compareTodos) // 按优先级和截止时间排序
                .collect(Collectors.toList());

        List<Todo> todoTodos = todos.stream()
                .filter(todo -> "TODO".equals(todo.getStatus()))
                .sorted(this::compareTodos) // 按优先级和截止时间排序
                .collect(Collectors.toList());

        // 生成报告内容
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

        // 已完成工作（按项目分组）
        content.append("### 已完成工作\n");
        if (!completedTodos.isEmpty()) {
            Map<Long, List<Todo>> todosByProject = completedTodos.stream()
                    .filter(todo -> todo.getProjectId() != null)
                    .collect(Collectors.groupingBy(Todo::getProjectId));

            for (Map.Entry<Long, List<Todo>> entry : todosByProject.entrySet()) {
                List<Todo> projectTodos = entry.getValue();
                if (!projectTodos.isEmpty() && projectTodos.get(0).getProject() != null) {
                    content.append("\n#### ").append(projectTodos.get(0).getProject().getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        content.append("- ").append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        if (todo.getCompletedTime() != null) {
                            content.append(" - 完成时间：").append(todo.getCompletedTime().toLocalDate());
                            // 添加完成状态描述
                            if (todo.getDueDate() != null) {
                                long daysDiff = ChronoUnit.DAYS.between(todo.getCompletedTime().toLocalDate(),
                                        todo.getDueDate());
                                if (daysDiff > 0) {
                                    content.append("，提前").append(daysDiff).append("天完成");
                                } else if (daysDiff == 0) {
                                    content.append("，按时完成");
                                } else {
                                    content.append("，逾期").append(Math.abs(daysDiff)).append("天完成");
                                }
                            }
                        }
                        content.append("\n");
                    }
                }
            }

            // 处理没有项目的任务
            List<Todo> noProjectTodos = completedTodos.stream()
                    .filter(todo -> todo.getProjectId() == null)
                    .collect(Collectors.toList());
            if (!noProjectTodos.isEmpty()) {
                content.append("\n#### 其他任务\n");
                for (Todo todo : noProjectTodos) {
                    content.append("- ").append(todo.getTitle());
                    if (todo.getAssignee() != null) {
                        content.append("（").append(todo.getAssignee().getNickname()).append("）");
                    }
                    if (todo.getCompletedTime() != null) {
                        content.append(" - 完成时间：").append(todo.getCompletedTime().toLocalDate());
                        // 添加完成状态描述
                        if (todo.getDueDate() != null) {
                            long daysDiff = ChronoUnit.DAYS.between(todo.getCompletedTime().toLocalDate(),
                                    todo.getDueDate());
                            if (daysDiff > 0) {
                                content.append("，提前").append(daysDiff).append("天完成");
                            } else if (daysDiff == 0) {
                                content.append("，按时完成");
                            } else {
                                content.append("，逾期").append(Math.abs(daysDiff)).append("天完成");
                            }
                        }
                    }
                    content.append("\n");
                }
            }
        } else {
            content.append("- 暂无已完成任务\n");
        }
        content.append("\n");

        // 进行中的工作（按项目分组）
        content.append("### 进行中的工作\n");
        if (!normalProgressTodos.isEmpty()) {
            Map<Long, List<Todo>> todosByProject = normalProgressTodos.stream()
                    .filter(todo -> todo.getProjectId() != null)
                    .collect(Collectors.groupingBy(Todo::getProjectId));

            for (Map.Entry<Long, List<Todo>> entry : todosByProject.entrySet()) {
                List<Todo> projectTodos = entry.getValue();
                if (!projectTodos.isEmpty() && projectTodos.get(0).getProject() != null) {
                    content.append("\n#### ").append(projectTodos.get(0).getProject().getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        content.append("- ");
                        if ("HIGH".equals(todo.getPriority())) {
                            content.append("【高】");
                        }
                        content.append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        if (todo.getDueDate() != null) {
                            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), todo.getDueDate());
                            if (daysUntilDue >= 0) {
                                content.append(" - 剩余").append(daysUntilDue).append("天");
                            }
                        }
                        content.append("\n");
                    }
                }
            }
        } else {
            content.append("- 暂无进行中的任务\n");
        }
        content.append("\n");

        // 风险项（已逾期的任务，按项目分组）
        if (!overdueTodos.isEmpty()) {
            content.append("### 风险项\n");
            Map<Long, List<Todo>> todosByProject = overdueTodos.stream()
                    .filter(todo -> todo.getProjectId() != null)
                    .collect(Collectors.groupingBy(Todo::getProjectId));

            for (Map.Entry<Long, List<Todo>> entry : todosByProject.entrySet()) {
                List<Todo> projectTodos = entry.getValue();
                if (!projectTodos.isEmpty() && projectTodos.get(0).getProject() != null) {
                    content.append("\n#### ").append(projectTodos.get(0).getProject().getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        long overdueDays = ChronoUnit.DAYS.between(todo.getDueDate(), LocalDate.now());
                        String priority = todo.getPriority();
                        String riskLevel = "HIGH".equals(priority) ? "高风险" : "MEDIUM".equals(priority) ? "中风险" : "低风险";
                        content.append("- 【").append(riskLevel).append("】")
                                .append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        content.append(" - 已逾期").append(overdueDays).append("天\n");
                    }
                }
            }
            content.append("\n");
        }

        // 下期计划（按项目分组）
        String nextPeriod = type.equals("WEEKLY") ? "下周"
                : type.equals("BIWEEKLY") ? "下两周" : type.equals("MONTHLY") ? "下月" : "下阶段";
        content.append("### ").append(nextPeriod).append("计划\n");
        if (!todoTodos.isEmpty()) {
            Map<Long, List<Todo>> todosByProject = todoTodos.stream()
                    .filter(todo -> todo.getProjectId() != null)
                    .collect(Collectors.groupingBy(Todo::getProjectId));

            for (Map.Entry<Long, List<Todo>> entry : todosByProject.entrySet()) {
                List<Todo> projectTodos = entry.getValue();
                if (!projectTodos.isEmpty() && projectTodos.get(0).getProject() != null) {
                    content.append("\n#### ").append(projectTodos.get(0).getProject().getName()).append("\n");
                    for (Todo todo : projectTodos) {
                        content.append("- ");
                        if ("HIGH".equals(todo.getPriority())) {
                            content.append("【高优先级】");
                        }
                        content.append(todo.getTitle());
                        if (todo.getAssignee() != null) {
                            content.append("（").append(todo.getAssignee().getNickname()).append("）");
                        }
                        if (todo.getDueDate() != null) {
                            content.append(" - 截止：").append(todo.getDueDate());
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