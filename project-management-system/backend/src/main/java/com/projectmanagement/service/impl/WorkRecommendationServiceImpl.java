package com.projectmanagement.service.impl;

import com.projectmanagement.dto.WorkRecommendationDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.service.WorkRecommendationService;
import com.projectmanagement.service.ProjectService;
import com.projectmanagement.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 智能工作推荐服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkRecommendationServiceImpl implements WorkRecommendationService {

    private final ProjectService projectService;
    private final TodoService todoService;

    @Override
    public WorkRecommendationDTO.RecommendationSummary getUserRecommendations(Long userId) {
        log.info("获取用户 {} 的工作推荐", userId);

        WorkRecommendationDTO.RecommendationSummary summary = new WorkRecommendationDTO.RecommendationSummary();

        // 初始化各类推荐列表
        List<WorkRecommendationDTO.RecommendationItem> urgent = new ArrayList<>();
        List<WorkRecommendationDTO.RecommendationItem> stagnant = new ArrayList<>();
        List<WorkRecommendationDTO.RecommendationItem> progress = new ArrayList<>();
        List<WorkRecommendationDTO.RecommendationItem> collaboration = new ArrayList<>();
        List<WorkRecommendationDTO.RecommendationItem> risk = new ArrayList<>();
        List<WorkRecommendationDTO.RecommendationItem> suggestions = new ArrayList<>();

        try {
            // 获取用户相关的项目和待办
            List<Project> userProjects = projectService.getProjectListByUser(userId);
            List<Todo> userTodos = todoService.getUserTodos(userId);

            log.info("用户 {} 的数据统计: 项目 {} 个, 待办 {} 个", userId, userProjects.size(), userTodos.size());

            // 1. 分析紧急推进项
            urgent.addAll(analyzeUrgentTasks(userTodos));
            log.info("紧急推进分析完成，发现 {} 条", urgent.size());
            
            // 2. 分析风险预警
            risk.addAll(analyzeRiskWarnings(userProjects, userTodos));
            log.info("风险预警分析完成，发现 {} 条", risk.size());

            // 3. 分析项目停滞
            stagnant.addAll(analyzeStagnantProjects(userProjects, userTodos));
            log.info("项目停滞分析完成，发现 {} 条", stagnant.size());

            // 4. 分析项目推进
            progress.addAll(analyzeProgressIssues(userProjects));
            log.info("项目推进分析完成，发现 {} 条", progress.size());

            // 5. 分析协作待办
            collaboration.addAll(analyzeCollaborationTasks(userTodos));
            log.info("协作待办分析完成，发现 {} 条", collaboration.size());

            // 6. 生成智能建议
            suggestions.addAll(generateSuggestions(userProjects, userTodos));
            log.info("智能建议生成完成，发现 {} 条", suggestions.size());

        } catch (Exception e) {
            log.error("生成工作推荐时出错", e);
        }

        // 封装结果
        summary.setUrgent(urgent);
        summary.setStagnant(stagnant);
        summary.setProgress(progress);
        summary.setCollaboration(collaboration);
        summary.setRisk(risk);
        summary.setSuggestions(suggestions);
        summary.setTotalCount(urgent.size() + stagnant.size() + progress.size() +
                collaboration.size() + risk.size() + suggestions.size());

        log.info("为用户 {} 生成了 {} 条工作推荐", userId, summary.getTotalCount());
        return summary;
    }

    /**
     * 分析紧急推进任务
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeUrgentTasks(List<Todo> todos) {
        List<WorkRecommendationDTO.RecommendationItem> urgent = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        log.info("开始分析紧急任务，总待办数: {}", todos.size());

        for (Todo todo : todos) {
            if (todo.getDueDate() != null && !"DONE".equals(todo.getStatus())) {
                LocalDateTime dueDate = todo.getDueDate().atStartOfDay();
                long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDate);

                log.debug("检查待办: {}, 到期时间: {}, 距离到期: {}小时, 优先级: {}",
                        todo.getTitle(), todo.getDueDate(), hoursUntilDue, todo.getPriority());

                // 扩展条件：
                // 1. 已逾期的任务（任何优先级）
                // 2. 48小时内到期的高优先级任务
                // 3. 24小时内到期的中优先级任务
                boolean isUrgent = false;
                String description = "";

                if (hoursUntilDue <= 0) {
                    isUrgent = true;
                    description = String.format("任务已逾期 %d 小时，需要立即处理", Math.abs(hoursUntilDue));
                } else if (hoursUntilDue <= 24
                        && ("HIGH".equals(todo.getPriority()) || "MEDIUM".equals(todo.getPriority()))) {
                    isUrgent = true;
                    description = "任务今日到期，请及时处理";
                } else if (hoursUntilDue <= 48 && "HIGH".equals(todo.getPriority())) {
                    isUrgent = true;
                    description = "高优先级任务即将到期，建议优先处理";
                }

                if (isUrgent) {
                    WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setType("URGENT");
                    item.setTitle(todo.getTitle());
                    item.setDescription(description);
                    item.setTodoId(todo.getId());
                    item.setProjectId(todo.getProjectId());
                    item.setPriority("HIGH");
                    item.setActionType("VIEW_TODO"); // 紧急任务应该查看具体待办
                    item.setCreateTime(LocalDateTime.now());
                    urgent.add(item);

                    log.info("发现紧急任务: {}", todo.getTitle());
                }
            }
        }

        return urgent;
    }

    /**
     * 分析项目停滞
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeStagnantProjects(List<Project> projects,
            List<Todo> todos) {
        List<WorkRecommendationDTO.RecommendationItem> stagnant = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Project project : projects) {
            List<Todo> projectTodos = getProjectTodos(todos, project.getId());

            if (isProjectStagnant(project, projectTodos)) {
                long daysSinceLastActivity = calculateDaysSinceLastActivity(project, projectTodos, now);

                if (daysSinceLastActivity >= 3) {
                    stagnant.add(createStagnantRecommendation(project, daysSinceLastActivity));
                }
            }
        }

        return stagnant;
    }

    /**
     * 获取指定项目的待办任务
     */
    private List<Todo> getProjectTodos(List<Todo> allTodos, Long projectId) {
        return allTodos.stream()
                .filter(todo -> projectId.equals(todo.getProjectId()))
                .collect(Collectors.toList());
    }

    /**
     * 判断项目是否停滞
     */
    private boolean isProjectStagnant(Project project, List<Todo> projectTodos) {
        boolean isActive = "PROGRESS".equals(project.getStatus());
        boolean isIncomplete = project.getProgress() < 100;
        boolean hasNoActiveTodos = projectTodos.stream()
                .noneMatch(todo -> "TODO".equals(todo.getStatus()) || "PROGRESS".equals(todo.getStatus()));

        return isActive && isIncomplete && hasNoActiveTodos;
    }

    /**
     * 计算距离最后活动的天数
     */
    private long calculateDaysSinceLastActivity(Project project, List<Todo> projectTodos, LocalDateTime now) {
        // 获取最后完成的待办时间
        LocalDateTime lastCompletedTime = projectTodos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()) && todo.getCompletedTime() != null)
                .map(Todo::getCompletedTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 获取最后创建的待办时间
        LocalDateTime lastCreatedTime = projectTodos.stream()
                .map(Todo::getCreateTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 获取最后更新的待办时间
        LocalDateTime lastUpdatedTime = projectTodos.stream()
                .map(Todo::getUpdateTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 取最后活动时间的最大值（创建、完成、更新中的最新时间）
        LocalDateTime lastActivityTime = project.getCreateTime(); // 默认为项目创建时间
        
        if (lastCompletedTime != null && lastCompletedTime.isAfter(lastActivityTime)) {
            lastActivityTime = lastCompletedTime;
        }
        if (lastCreatedTime != null && lastCreatedTime.isAfter(lastActivityTime)) {
            lastActivityTime = lastCreatedTime;
        }
        if (lastUpdatedTime != null && lastUpdatedTime.isAfter(lastActivityTime)) {
            lastActivityTime = lastUpdatedTime;
        }

        return ChronoUnit.DAYS.between(lastActivityTime, now);
    }

    /**
     * 创建停滞项目推荐项
     */
    private WorkRecommendationDTO.RecommendationItem createStagnantRecommendation(Project project,
            long daysSinceLastActivity) {
        WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
        item.setId(UUID.randomUUID().toString());
        item.setType("STAGNANT");
        item.setTitle(project.getName() + " - 项目停滞");
        item.setDescription(String.format("项目已 %d 天无新待办，进度 %d%%，建议创建下一步计划",
                daysSinceLastActivity, project.getProgress()));
        item.setProjectId(project.getId());
        item.setPriority("MEDIUM");
        item.setActionType("VIEW_PROJECT");
        item.setCreateTime(LocalDateTime.now());
        return item;
    }

    /**
     * 分析项目推进问题
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeProgressIssues(List<Project> projects) {
        List<WorkRecommendationDTO.RecommendationItem> progress = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        log.info("开始分析项目推进问题，项目数: {}", projects.size());

        for (Project project : projects) {
            if ("PROGRESS".equals(project.getStatus()) && project.getProgress() < 100) {
                boolean hasProgressIssue = false;
                String description = "";

                // 情况1：有明确时间计划的项目进度分析
                if (project.getStartDate() != null && project.getEndDate() != null) {
                    LocalDateTime startDate = project.getStartDate().atStartOfDay();
                    LocalDateTime endDate = project.getEndDate().atStartOfDay();

                    if (now.isBefore(endDate)) {
                        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
                        long passedDays = ChronoUnit.DAYS.between(startDate, now);

                        if (totalDays > 0) {
                            double timeProgress = (double) passedDays / totalDays * 100;
                            double actualProgress = project.getProgress();

                            // 降低阈值从15%到10%
                            if (timeProgress - actualProgress > 10) {
                                hasProgressIssue = true;
                                description = String.format("时间进度 %.1f%%，实际进度 %.1f%%，建议加快推进",
                                        timeProgress, actualProgress);
                            }
                        }
                    }
                }

                // 情况2：长时间无进度更新的项目
                if (!hasProgressIssue) {
                    long daysSinceUpdate = ChronoUnit.DAYS.between(project.getUpdateTime(), now);
                    if (daysSinceUpdate >= 7 && project.getProgress() < 50) {
                        hasProgressIssue = true;
                        description = String.format("项目已 %d 天无进度更新，当前进度 %d%%，建议检查推进情况",
                                daysSinceUpdate, project.getProgress());
                    }
                }

                // 情况3：低进度长期项目
                if (!hasProgressIssue) {
                    long daysSinceCreated = ChronoUnit.DAYS.between(project.getCreateTime(), now);
                    if (daysSinceCreated >= 14 && project.getProgress() < 30) {
                        hasProgressIssue = true;
                        description = String.format("项目创建已 %d 天，进度仅 %d%%，建议重新评估和推进",
                                daysSinceCreated, project.getProgress());
                    }
                }

                if (hasProgressIssue) {
                    WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setType("PROGRESS");
                    item.setTitle(project.getName() + " - 进度需要关注");
                    item.setDescription(description);
                    item.setProjectId(project.getId());
                    item.setPriority("HIGH");
                    item.setActionType("VIEW_PROJECT");
                    item.setCreateTime(LocalDateTime.now());
                    progress.add(item);

                    log.info("发现进度问题项目: {}", project.getName());
                }
            }
        }

        return progress;
    }

    /**
     * 分析协作待办
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeCollaborationTasks(List<Todo> todos) {
        List<WorkRecommendationDTO.RecommendationItem> collaboration = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        log.info("开始分析协作待办，总待办数: {}", todos.size());

        for (Todo todo : todos) {
            // 扩展协作检测条件：
            // 1. 进行中任务超过1天无更新（降低阈值）
            // 2. 待办状态任务超过3天未开始
            // 3. 高优先级任务超过1天未更新

            long daysSinceUpdate = ChronoUnit.DAYS.between(todo.getUpdateTime(), now);
            boolean needsCollaboration = false;
            String description = "";

            if ("PROGRESS".equals(todo.getStatus()) && daysSinceUpdate >= 1) {
                needsCollaboration = true;
                description = String.format("任务已 %d 天无更新，可能需要协作或沟通", daysSinceUpdate);
            } else if ("TODO".equals(todo.getStatus()) && daysSinceUpdate >= 3) {
                needsCollaboration = true;
                description = String.format("任务创建已 %d 天未开始，建议跟进执行情况", daysSinceUpdate);
            } else if ("HIGH".equals(todo.getPriority()) && !"DONE".equals(todo.getStatus()) && daysSinceUpdate >= 1) {
                needsCollaboration = true;
                description = String.format("高优先级任务已 %d 天无更新，建议重点关注", daysSinceUpdate);
            }

            if (needsCollaboration) {
                WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
                item.setId(UUID.randomUUID().toString());
                item.setType("COLLABORATION");
                item.setTitle(todo.getTitle() + " - 需要协作推进");
                item.setDescription(description);
                item.setTodoId(todo.getId());
                item.setProjectId(todo.getProjectId());
                item.setPriority("MEDIUM");
                item.setActionType("VIEW_TODO"); // 协作待办应该查看具体任务
                item.setCreateTime(LocalDateTime.now());
                collaboration.add(item);

                log.info("发现协作待办: {}, 状态: {}, 天数: {}", todo.getTitle(), todo.getStatus(), daysSinceUpdate);
            }
        }

        return collaboration;
    }

    /**
     * 分析风险预警
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeRiskWarnings(List<Project> projects,
            List<Todo> todos) {
        List<WorkRecommendationDTO.RecommendationItem> risk = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Project project : projects) {
            if ("PROGRESS".equals(project.getStatus())) {
                // 重点关注当前进行中里程碑的风险预警
                risk.addAll(analyzeCurrentMilestoneRisks(project, now));
            }
        }

        return risk;
    }

    /**
     * 分析当前进行中里程碑的风险预警
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeCurrentMilestoneRisks(Project project, LocalDateTime now) {
        List<WorkRecommendationDTO.RecommendationItem> milestoneRisks = new ArrayList<>();
        
        if (project.getMilestones() == null || project.getMilestones().trim().isEmpty()) {
            return milestoneRisks;
        }

        try {
            // 解析里程碑JSON数据
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>> typeToken = 
                new com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>>() {};
            List<java.util.Map<String, Object>> milestones = gson.fromJson(project.getMilestones(), typeToken.getType());

            // 优先分析进行中的里程碑
            List<java.util.Map<String, Object>> progressMilestones = milestones.stream()
                .filter(milestone -> "PROGRESS".equals(milestone.get("status")))
                .collect(java.util.stream.Collectors.toList());

            // 如果没有进行中的里程碑，则分析待开始的里程碑
            if (progressMilestones.isEmpty()) {
                progressMilestones = milestones.stream()
                    .filter(milestone -> "PENDING".equals(milestone.get("status")))
                    .collect(java.util.stream.Collectors.toList());
            }

            for (java.util.Map<String, Object> milestone : progressMilestones) {
                String name = (String) milestone.get("name");
                String status = (String) milestone.get("status");
                String dueDateStr = (String) milestone.get("dueDate");

                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    try {
                        // 解析里程碑截止日期
                        java.time.LocalDate dueDate = java.time.LocalDate.parse(dueDateStr);
                        LocalDateTime dueDatetime = dueDate.atTime(23, 59, 59);
                        long daysUntilMilestone = ChronoUnit.DAYS.between(now, dueDatetime);

                        WorkRecommendationDTO.RecommendationItem item = null;
                        String statusText = "PROGRESS".equals(status) ? "进行中" : "待开始";

                        if (daysUntilMilestone < 0) {
                            // 当前里程碑已逾期 - 最高优先级
                            item = new WorkRecommendationDTO.RecommendationItem();
                            item.setId(UUID.randomUUID().toString());
                            item.setType("RISK");
                            item.setTitle(String.format("[逾期%d天] %s - 当前里程碑逾期", 
                                    Math.abs(daysUntilMilestone), project.getName()));
                            item.setDescription(String.format("当前%s里程碑「%s」已逾期 %d 天，严重影响项目进度！",
                                    statusText, name, Math.abs(daysUntilMilestone)));
                            item.setPriority("HIGH");
                        } else if (daysUntilMilestone == 0) {
                            // 当前里程碑今日到期 - 最高优先级
                            item = new WorkRecommendationDTO.RecommendationItem();
                            item.setId(UUID.randomUUID().toString());
                            item.setType("RISK");
                            item.setTitle(String.format("[今日到期] %s - 当前里程碑今日到期", project.getName()));
                            item.setDescription(String.format("当前%s里程碑「%s」今日到期，请立即处理！",
                                    statusText, name));
                            item.setPriority("HIGH");
                        } else if (daysUntilMilestone <= 3) {
                            // 当前里程碑即将到期（3天内）- 高优先级
                            item = new WorkRecommendationDTO.RecommendationItem();
                            item.setId(UUID.randomUUID().toString());
                            item.setType("RISK");
                            item.setTitle(String.format("[剩余%d天] %s - 当前里程碑紧急", 
                                    daysUntilMilestone, project.getName()));
                            item.setDescription(String.format("当前%s里程碑「%s」还有 %d 天到期，请加急推进！",
                                    statusText, name, daysUntilMilestone));
                            item.setPriority("HIGH");
                        } else if (daysUntilMilestone <= 7) {
                            // 当前里程碑一周内到期 - 中等优先级
                            item = new WorkRecommendationDTO.RecommendationItem();
                            item.setId(UUID.randomUUID().toString());
                            item.setType("RISK");
                            item.setTitle(String.format("[剩余%d天] %s - 当前里程碑提醒", 
                                    daysUntilMilestone, project.getName()));
                            item.setDescription(String.format("当前%s里程碑「%s」还有 %d 天到期，建议重点关注",
                                    statusText, name, daysUntilMilestone));
                            item.setPriority("MEDIUM");
                        } else if (daysUntilMilestone <= 14) {
                            // 当前里程碑两周内到期 - 提前提醒
                            item = new WorkRecommendationDTO.RecommendationItem();
                            item.setId(UUID.randomUUID().toString());
                            item.setType("RISK");
                            item.setTitle(String.format("[剩余%d天] %s - 里程碑计划提醒", 
                                    daysUntilMilestone, project.getName()));
                            item.setDescription(String.format("当前%s里程碑「%s」还有 %d 天到期，建议制定详细计划",
                                    statusText, name, daysUntilMilestone));
                            item.setPriority("MEDIUM");
                        }

                        if (item != null) {
                            item.setProjectId(project.getId());
                            item.setActionType("VIEW_PROJECT");
                            item.setCreateTime(LocalDateTime.now());
                            milestoneRisks.add(item);

                            log.info("发现当前里程碑风险: 项目={}, 里程碑={}, 状态={}, 剩余天数={}", 
                                    project.getName(), name, status, daysUntilMilestone);
                        }

                    } catch (Exception e) {
                        log.warn("解析里程碑日期失败: {}, 日期字符串: {}", name, dueDateStr, e);
                    }
                }
            }

            // 如果没有找到任何当前里程碑风险，检查是否有已逾期的其他里程碑
            if (milestoneRisks.isEmpty()) {
                milestoneRisks.addAll(analyzeOverdueMilestones(project, milestones, now));
            }

        } catch (Exception e) {
            log.warn("解析项目 {} 的里程碑数据失败: {}", project.getName(), e.getMessage());
        }

        return milestoneRisks;
    }

    /**
     * 分析逾期的里程碑（当没有当前进行中的里程碑时）
     */
    private List<WorkRecommendationDTO.RecommendationItem> analyzeOverdueMilestones(Project project, 
            List<java.util.Map<String, Object>> milestones, LocalDateTime now) {
        List<WorkRecommendationDTO.RecommendationItem> overdueMilestones = new ArrayList<>();

        for (java.util.Map<String, Object> milestone : milestones) {
            String name = (String) milestone.get("name");
            String status = (String) milestone.get("status");
            String dueDateStr = (String) milestone.get("dueDate");

            // 只分析未完成且有截止日期的里程碑
            if (!"COMPLETED".equals(status) && dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                try {
                    java.time.LocalDate dueDate = java.time.LocalDate.parse(dueDateStr);
                    LocalDateTime dueDatetime = dueDate.atTime(23, 59, 59);
                    long daysUntilMilestone = ChronoUnit.DAYS.between(now, dueDatetime);

                    if (daysUntilMilestone < 0) {
                        // 发现逾期里程碑
                        WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
                        item.setId(UUID.randomUUID().toString());
                        item.setType("RISK");
                        item.setTitle(String.format("[逾期%d天] %s - 里程碑逾期", 
                                Math.abs(daysUntilMilestone), project.getName()));
                        item.setDescription(String.format("里程碑「%s」已逾期 %d 天，需要重新规划",
                                name, Math.abs(daysUntilMilestone)));
                        item.setProjectId(project.getId());
                        item.setPriority("HIGH");
                        item.setActionType("VIEW_PROJECT");
                        item.setCreateTime(LocalDateTime.now());
                        overdueMilestones.add(item);
                    }
                } catch (Exception e) {
                    log.warn("解析逾期里程碑日期失败: {}, 日期字符串: {}", name, dueDateStr, e);
                }
            }
        }

        return overdueMilestones;
    }

    /**
     * 生成智能建议
     */
    private List<WorkRecommendationDTO.RecommendationItem> generateSuggestions(List<Project> projects,
            List<Todo> todos) {
        List<WorkRecommendationDTO.RecommendationItem> suggestions = new ArrayList<>();

        log.info("开始生成智能建议，项目数: {}, 待办数: {}", projects.size(), todos.size());

        // 建议1：优化任务优先级
        long highPriorityCount = todos.stream()
                .filter(todo -> "HIGH".equals(todo.getPriority()) && !"DONE".equals(todo.getStatus()))
                .count();

        if (highPriorityCount > 3) { // 降低阈值
            WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
            item.setId(UUID.randomUUID().toString());
            item.setType("SUGGESTION");
            item.setTitle("建议重新评估任务优先级");
            item.setDescription(String.format("当前有 %d 个高优先级任务，建议重新评估优先级分配", highPriorityCount));
            item.setPriority("LOW");
            item.setActionType("VIEW_PROJECT");
            item.setCreateTime(LocalDateTime.now());
            suggestions.add(item);
        }

        // 建议2：项目进度跟进
        long activeProjectsCount = projects.stream()
                .filter(project -> "PROGRESS".equals(project.getStatus()))
                .count();

        if (activeProjectsCount > 0) {
            WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
            item.setId(UUID.randomUUID().toString());
            item.setType("SUGGESTION");
            item.setTitle("建议定期跟进项目进度");
            item.setDescription(String.format("当前有 %d 个进行中项目，建议设置里程碑节点进行定期跟进", activeProjectsCount));
            item.setPriority("LOW");
            item.setActionType("VIEW_PROJECT");
            item.setCreateTime(LocalDateTime.now());
            suggestions.add(item);
        }

        // 建议3：任务均衡分配
        long todoCount = todos.stream()
                .filter(todo -> !"DONE".equals(todo.getStatus()))
                .count();

        if (todoCount > 5) {
            WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
            item.setId(UUID.randomUUID().toString());
            item.setType("SUGGESTION");
            item.setTitle("建议合理分配工作量");
            item.setDescription(String.format("当前有 %d 个未完成任务，建议按优先级和时间安排合理分配", todoCount));
            item.setPriority("LOW");
            item.setActionType("VIEW_PROJECT");
            item.setCreateTime(LocalDateTime.now());
            suggestions.add(item);
        }

        // 建议4：添加截止日期
        long todoWithoutDueDateCount = todos.stream()
                .filter(todo -> !"DONE".equals(todo.getStatus()) && todo.getDueDate() == null)
                .count();

        if (todoWithoutDueDateCount > 0) {
            WorkRecommendationDTO.RecommendationItem item = new WorkRecommendationDTO.RecommendationItem();
            item.setId(UUID.randomUUID().toString());
            item.setType("SUGGESTION");
            item.setTitle("建议为任务设置截止日期");
            item.setDescription(String.format("有 %d 个任务未设置截止日期，建议添加以便更好地管理进度", todoWithoutDueDateCount));
            item.setPriority("LOW");
            item.setActionType("VIEW_PROJECT");
            item.setCreateTime(LocalDateTime.now());
            suggestions.add(item);
        }

        log.info("生成了 {} 条智能建议", suggestions.size());
        return suggestions;
    }

    @Override
    public boolean executeRecommendation(String recommendationId, Long userId, Object actionData) {
        log.info("执行推荐操作：{}, 用户：{}", recommendationId, userId);
        // 这里可以根据推荐类型执行相应的操作
        // 例如创建待办、更新项目状态等
        return true;
    }

    @Override
    public boolean markAsHandled(String recommendationId, Long userId) {
        log.info("标记推荐为已处理：{}, 用户：{}", recommendationId, userId);
        // 这里可以将推荐标记为已处理，避免重复推荐
        return true;
    }

    @Override
    public boolean ignoreRecommendation(String recommendationId, Long userId) {
        log.info("忽略推荐：{}, 用户：{}", recommendationId, userId);
        // 这里可以将推荐加入忽略列表
        return true;
    }
}