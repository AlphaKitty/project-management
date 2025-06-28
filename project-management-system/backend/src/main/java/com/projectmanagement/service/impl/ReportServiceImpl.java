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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private final ReportMapper reportMapper;
    private final TodoMapper todoMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

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
        Boolean fuzzyMode = reportDTO.getFuzzyMode() != null ? reportDTO.getFuzzyMode() : true;

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
        Map<Long, com.projectmanagement.entity.Project> projectMap = new java.util.LinkedHashMap<>();

        // 首先添加有待办任务的项目
        todos.stream()
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
                .forEach(p -> projectMap.put(p.getId(), p));

        // 然后添加指定的项目（如果不在上面的列表中）
        if (projectIds != null && !projectIds.isEmpty()) {
            for (Long projectId : projectIds) {
                if (!projectMap.containsKey(projectId)) {
                    com.projectmanagement.entity.Project project = projectMapper.selectById(projectId);
                    if (project != null) {
                        projectMap.put(projectId, project);
                    }
                }
            }
        }

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

        // 模糊模式下的过滤逻辑
        if (fuzzyMode) {
            // 对于待办数大于3个的项目，只显示时间范围内的任务
            Map<Long, List<Todo>> currentTodosByProject = currentTodos.stream()
                    .collect(Collectors.groupingBy(Todo::getProjectId));
            Map<Long, List<Todo>> nextTodosByProject = nextTodos.stream()
                    .collect(Collectors.groupingBy(Todo::getProjectId));

            currentTodos = currentTodosByProject.entrySet().stream()
                    .flatMap(entry -> {
                        Long projectId = entry.getKey();
                        List<Todo> projectTodos = entry.getValue();

                        if (projectTodos.size() > 3) {
                            // 项目待办数大于3个，只显示时间范围内的
                            LocalDate weekAgo = LocalDate.now().minusDays(7);
                            LocalDate weekLater = LocalDate.now().plusDays(7);

                            return projectTodos.stream().filter(todo -> {
                                if (todo.getDueDate() != null) {
                                    return !todo.getDueDate().isBefore(weekAgo) &&
                                            !todo.getDueDate().isAfter(weekLater);
                                }
                                return true; // 没有截止日期的任务仍然显示
                            });
                        }
                        return projectTodos.stream(); // 待办数<=3个的项目显示所有任务
                    })
                    .collect(Collectors.toList());

            nextTodos = nextTodosByProject.entrySet().stream()
                    .flatMap(entry -> {
                        Long projectId = entry.getKey();
                        List<Todo> projectTodos = entry.getValue();

                        if (projectTodos.size() > 3) {
                            // 项目待办数大于3个，只显示时间范围内的
                            LocalDate weekAgo = LocalDate.now().minusDays(7);
                            LocalDate weekLater = LocalDate.now().plusDays(7);

                            return projectTodos.stream().filter(todo -> {
                                if (todo.getDueDate() != null) {
                                    return !todo.getDueDate().isBefore(weekAgo) &&
                                            !todo.getDueDate().isAfter(weekLater);
                                }
                                return true; // 没有截止日期的任务仍然显示
                            });
                        }
                        return projectTodos.stream(); // 待办数<=3个的项目显示所有任务
                    })
                    .collect(Collectors.toList());
        }

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
        boolean hasCurrentWork = false;

        // 在模糊模式下，生成整体总结
        if (fuzzyMode) {
            // 统计项目数量和状态
            int totalProjects = projectMap.size();
            int inProgressProjects = 0;
            int completedProjects = 0;

            for (com.projectmanagement.entity.Project project : projectMap.values()) {
                if ("PROGRESS".equals(project.getStatus())) {
                    inProgressProjects++;
                } else if ("COMPLETED".equals(project.getStatus())) {
                    completedProjects++;
                }
            }

            // 统计待办任务数量
            int totalCurrentTodos = currentTodos.size();
            int completedTodosCount = (int) currentTodos.stream()
                    .filter(todo -> "DONE".equals(todo.getStatus()))
                    .count();
            int inProgressTodosCount = (int) currentTodos.stream()
                    .filter(todo -> "PROGRESS".equals(todo.getStatus()))
                    .count();

            // 统计下期待办任务数量
            int nextTodosCount = nextTodos.size();

            String period = type.equals("WEEKLY") ? "本周"
                    : type.equals("BIWEEKLY") ? "本双周"
                            : type.equals("MONTHLY") ? "本月" : "本期";

            String nextPeriod = type.equals("WEEKLY") ? "下周"
                    : type.equals("BIWEEKLY") ? "下双周"
                            : type.equals("MONTHLY") ? "下月" : "下期";

            content.append("【总结】共").append(totalProjects).append("个项目，其中")
                    .append(inProgressProjects).append("个进行中，")
                    .append(completedProjects).append("个已完成；")
                    .append(period).append("识别").append(totalCurrentTodos)
                    .append("项待办，其中已完成").append(completedTodosCount)
                    .append("项，进行中").append(inProgressTodosCount).append("项；")
                    .append(nextPeriod).append("预计").append(nextTodosCount)
                    .append("项待办，暂无风险。\n\n");
        }

        for (com.projectmanagement.entity.Project project : projectMap.values()) {
            List<Todo> projectTodos = currentTodos.stream()
                    .filter(todo -> project.getId().equals(todo.getProjectId()))
                    .sorted(this::compareTodos)
                    .collect(Collectors.toList());

            content.append("\n#### ").append(project.getName()).append("\n");
            hasCurrentWork = true;

            // 在模糊模式下添加节点状态信息
            if (fuzzyMode) {
                String nodeStatus = generateNodeStatus(project, projectTodos, nextTodos);
                content.append(nodeStatus).append("\n\n");
            }

            if (!projectTodos.isEmpty()) {
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

                    // 模糊模式下不显示按时/逾期状态
                    if (!fuzzyMode && todo.getDueDate() != null) {
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

            // 为没有待办任务的项目生成项目整体状态
            if (projectTodos.isEmpty()) {
                String projectStatus = generateProjectOverallStatus(project, "current");
                content.append("- ").append(projectStatus).append("\n");
            }
        }

        if (!hasCurrentWork) {
            content.append("- 暂无本期工作\n");
        }
        content.append("\n");

        // 下期计划
        String nextPeriod = type.equals("WEEKLY") ? "下周"
                : type.equals("BIWEEKLY") ? "下两周" : type.equals("MONTHLY") ? "下月" : "下阶段";
        content.append("### ").append(nextPeriod).append("计划\n");
        boolean hasNextPlan = false;

        for (com.projectmanagement.entity.Project project : projectMap.values()) {
            List<Todo> projectTodos = nextTodos.stream()
                    .filter(todo -> project.getId().equals(todo.getProjectId()))
                    .sorted(this::compareTodos)
                    .collect(Collectors.toList());

            content.append("\n#### ").append(project.getName()).append("\n");
            hasNextPlan = true;

            if (!projectTodos.isEmpty()) {
                for (Todo todo : projectTodos) {
                    content.append("- ")
                            .append(getPriorityLabel(todo.getPriority()))
                            .append(todo.getTitle());
                    if (todo.getAssignee() != null) {
                        content.append("（").append(todo.getAssignee().getNickname()).append("）");
                    }
                    if (todo.getDueDate() != null) {
                        content.append(" - 截止：").append(todo.getDueDate());

                        // 模糊模式下不显示逾期天数
                        if (!fuzzyMode) {
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
                    }
                    content.append("\n");
                }
            }

            // 为没有待办任务的项目生成项目整体计划
            if (projectTodos.isEmpty()) {
                String projectPlan = generateProjectOverallStatus(project, "plan");
                content.append("- ").append(projectPlan).append("\n");
            }
        }

        if (!hasNextPlan) {
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

    /**
     * 生成节点状态信息（模糊模式下使用）
     */
    private String generateNodeStatus(com.projectmanagement.entity.Project project, List<Todo> currentTodos,
            List<Todo> nextTodos) {
        if (project == null) {
            return "【节点状态】项目信息不完整";
        }

        StringBuilder status = new StringBuilder();
        status.append("【节点状态】");

        // 1. 解析里程碑信息
        String currentMilestone = "未知里程碑";
        double milestoneProgress = 0.0;

        if (project.getMilestones() != null && !project.getMilestones().trim().isEmpty()) {
            try {
                // 解析里程碑JSON
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>> typeToken = new com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>>() {
                };
                List<java.util.Map<String, Object>> milestones = gson.fromJson(project.getMilestones(),
                        typeToken.getType());

                if (milestones != null && !milestones.isEmpty()) {
                    // 查找当前里程碑（第一个未完成的或最后一个）
                    java.util.Map<String, Object> currentMilestoneObj = null;
                    java.util.Map<String, Object> prevMilestoneObj = null;

                    for (int i = 0; i < milestones.size(); i++) {
                        java.util.Map<String, Object> milestone = milestones.get(i);
                        String status_str = (String) milestone.get("status");

                        if (!"COMPLETED".equals(status_str)) {
                            currentMilestoneObj = milestone;
                            if (i > 0) {
                                prevMilestoneObj = milestones.get(i - 1);
                            }
                            break;
                        }
                    }

                    // 如果所有里程碑都完成了，使用最后一个
                    if (currentMilestoneObj == null && !milestones.isEmpty()) {
                        currentMilestoneObj = milestones.get(milestones.size() - 1);
                        if (milestones.size() > 1) {
                            prevMilestoneObj = milestones.get(milestones.size() - 2);
                        }
                    }

                    if (currentMilestoneObj != null) {
                        currentMilestone = (String) currentMilestoneObj.get("name");

                        // 计算里程碑进度（基于时间）
                        String dueDateStr = (String) currentMilestoneObj.get("dueDate");
                        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                            try {
                                LocalDate currentDueDate = LocalDate.parse(dueDateStr);
                                LocalDate prevDueDate = null;

                                if (prevMilestoneObj != null) {
                                    String prevDueDateStr = (String) prevMilestoneObj.get("dueDate");
                                    if (prevDueDateStr != null && !prevDueDateStr.trim().isEmpty()) {
                                        prevDueDate = LocalDate.parse(prevDueDateStr);
                                    }
                                }

                                // 如果没有前一个里程碑，使用项目开始时间
                                if (prevDueDate == null) {
                                    prevDueDate = project.getStartDate() != null ? project.getStartDate()
                                            : currentDueDate.minusDays(30);
                                }

                                LocalDate today = LocalDate.now();
                                long totalDays = ChronoUnit.DAYS.between(prevDueDate, currentDueDate);
                                long elapsedDays = ChronoUnit.DAYS.between(prevDueDate, today);

                                if (totalDays > 0) {
                                    milestoneProgress = Math.max(0,
                                            Math.min(100, (double) elapsedDays / totalDays * 100));
                                }
                            } catch (Exception e) {
                                // 日期解析失败，使用默认进度
                                milestoneProgress = project.getProgress() != null ? project.getProgress() : 0;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 里程碑JSON解析失败，使用项目名称作为里程碑
                currentMilestone = project.getName();
                milestoneProgress = project.getProgress() != null ? project.getProgress() : 0;
            }
        } else {
            // 没有里程碑信息，使用项目名称
            currentMilestone = project.getName();
            milestoneProgress = project.getProgress() != null ? project.getProgress() : 0;
        }

        status.append("当前里程碑：").append(currentMilestone)
                .append("，当前里程碑进度：").append(String.format("%.0f", milestoneProgress)).append("%");

        // 2. 统计待办任务数量
        List<Todo> projectCurrentTodos = currentTodos.stream()
                .filter(todo -> project.getId().equals(todo.getProjectId()))
                .collect(Collectors.toList());

        List<Todo> projectNextTodos = nextTodos.stream()
                .filter(todo -> project.getId().equals(todo.getProjectId()))
                .collect(Collectors.toList());

        // 合并当前和下期的待办任务来统计总数
        List<Todo> allProjectTodos = new java.util.ArrayList<>();
        allProjectTodos.addAll(projectCurrentTodos);
        // 添加下期计划中不在当前工作中的任务
        for (Todo nextTodo : projectNextTodos) {
            boolean exists = projectCurrentTodos.stream()
                    .anyMatch(currentTodo -> currentTodo.getId().equals(nextTodo.getId()));
            if (!exists) {
                allProjectTodos.add(nextTodo);
            }
        }

        int totalTodos = allProjectTodos.size();
        int completedCount = (int) allProjectTodos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()))
                .count();
        int inProgressCount = (int) allProjectTodos.stream()
                .filter(todo -> "PROGRESS".equals(todo.getStatus()))
                .count();
        int todoCount = totalTodos - completedCount - inProgressCount;

        status.append("，共识别").append(totalTodos).append("个待办")
                .append("，进行中").append(inProgressCount).append("个")
                .append("，已完成").append(completedCount).append("个");

        return status.toString();
    }

    /**
     * 为没有待办任务的项目生成整体状态信息
     */
    private String generateProjectOverallStatus(com.projectmanagement.entity.Project project, String type) {
        if (project == null) {
            return "项目状态未知";
        }

        // 计算项目剩余天数
        long daysLeft = 0;
        if (project.getEndDate() != null) {
            daysLeft = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), project.getEndDate());
        }

        // 获取项目当前状态
        String statusText = "";
        switch (project.getStatus()) {
            case "PENDING":
                statusText = "待启动";
                break;
            case "PROGRESS":
                statusText = "正常进行";
                break;
            case "COMPLETED":
                statusText = "已完成";
                break;
            case "CANCELLED":
                statusText = "已取消";
                break;
            default:
                statusText = "进行中";
                break;
        }

        // 根据类型生成不同的状态描述
        if ("current".equals(type)) {
            // 本期工作状态
            if ("COMPLETED".equals(project.getStatus())) {
                return statusText + "：" + project.getName() + "（项目已完成）";
            } else if ("CANCELLED".equals(project.getStatus())) {
                return statusText + "：" + project.getName() + "（项目已取消）";
            } else {
                String timeInfo = "";
                if (daysLeft > 0) {
                    timeInfo = "（剩余" + daysLeft + "天）";
                } else if (daysLeft == 0) {
                    timeInfo = "（今日截止）";
                } else if (daysLeft < 0) {
                    timeInfo = "（已逾期" + Math.abs(daysLeft) + "天）";
                }
                return statusText + "：" + project.getName() + timeInfo;
            }
        } else if ("plan".equals(type)) {
            // 下期计划状态
            if ("COMPLETED".equals(project.getStatus())) {
                return "项目维护：" + project.getName() + "（持续优化和维护）";
            } else if ("CANCELLED".equals(project.getStatus())) {
                return "项目归档：" + project.getName() + "（完成资料整理）";
            } else {
                // 根据进度估算生成计划描述
                int progress = project.getProgress() != null ? project.getProgress() : 0;
                String progressInfo = "（预估进度" + progress + "%）";

                if (progress < 30) {
                    return "持续推进：" + project.getName() + progressInfo;
                } else if (progress < 70) {
                    return "加速推进：" + project.getName() + progressInfo;
                } else {
                    return "收尾推进：" + project.getName() + progressInfo;
                }
            }
        }

        return statusText + "：" + project.getName();
    }
}