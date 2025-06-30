package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.ProjectDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.entity.User;
import com.projectmanagement.entity.OperationLog;
import com.projectmanagement.mapper.ProjectMapper;
import com.projectmanagement.mapper.TodoMapper;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.ProjectService;
import com.projectmanagement.service.UserService;
import com.projectmanagement.service.TodoService;
import com.projectmanagement.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 项目服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;
    private final TodoService todoService;
    private final OperationLogService operationLogService;

    @Override
    public List<Project> getProjectList() {
        List<Project> projects = projectMapper.selectProjectsWithCreator();

        if (projects.isEmpty()) {
            return projects;
        }

        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                userIds.add(project.getCreatorId());
            }
            if (project.getAssigneeId() != null) {
                userIds.add(project.getAssigneeId());
            }
        }

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 设置creator和assignee对象
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                project.setCreator(userMap.get(project.getCreatorId()));
            }
            if (project.getAssigneeId() != null) {
                project.setAssignee(userMap.get(project.getAssigneeId()));
            }
        }

        return projects;
    }

    @Override
    public List<Project> getProjectListByUser(Long userId) {
        List<Project> projects = projectMapper.selectProjectsByCreatorOrAssignee(userId);

        if (projects.isEmpty()) {
            return projects;
        }

        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                userIds.add(project.getCreatorId());
            }
            if (project.getAssigneeId() != null) {
                userIds.add(project.getAssigneeId());
            }
        }

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 设置creator和assignee对象
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                project.setCreator(userMap.get(project.getCreatorId()));
            }
            if (project.getAssigneeId() != null) {
                project.setAssignee(userMap.get(project.getAssigneeId()));
            }
        }

        return projects;
    }

    @Override
    public Project getProjectDetail(Long projectId) {
        Project project = projectMapper.selectProjectWithCreator(projectId);
        if (project != null) {
            // 设置创建人信息
            if (project.getCreatorId() != null) {
                User creator = userMapper.selectById(project.getCreatorId());
                project.setCreator(creator);
            }

            // 设置责任人信息
            if (project.getAssigneeId() != null) {
                User assignee = userMapper.selectById(project.getAssigneeId());
                project.setAssignee(assignee);
            }

            // 加载成员信息（只在详情页面加载）
            List<User> members = userMapper.selectMembersByProjectId(projectId);
            project.setMembers(members);
        }
        return project;
    }

    @Override
    @Transactional
    public Project createProject(ProjectDTO projectDTO) {
        Project project = new Project();
        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setStatus(projectDTO.getStatus() != null ? projectDTO.getStatus() : "PENDING");
        project.setStartDate(projectDTO.getStartDate());
        project.setEndDate(projectDTO.getEndDate());
        project.setProgress(projectDTO.getProgress() != null ? projectDTO.getProgress() : 0);
        project.setCreatorId(projectDTO.getCreatorId());
        project.setAssigneeId(projectDTO.getAssigneeId());
        project.setMilestones(projectDTO.getMilestones());
        projectMapper.insert(project);

        // 添加项目成员
        if (projectDTO.getMemberIds() != null) {
            for (Long memberId : projectDTO.getMemberIds()) {
                addProjectMember(project.getId(), memberId);
            }
        }

        return getProjectDetail(project.getId());
    }

    @Override
    @Transactional
    public Project updateProject(Long projectId, ProjectDTO projectDTO) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setStatus(projectDTO.getStatus());
        project.setStartDate(projectDTO.getStartDate());
        project.setEndDate(projectDTO.getEndDate());
        project.setProgress(projectDTO.getProgress());
        project.setMilestones(projectDTO.getMilestones());

        projectMapper.updateById(project);
        return getProjectDetail(projectId);
    }

    @Override
    @Transactional
    public boolean deleteProject(Long projectId) {
        return projectMapper.deleteById(projectId) > 0;
    }

    @Override
    public List<Project> getUserProjects(Long userId) {
        return projectMapper.selectProjectsByUserId(userId);
    }

    @Override
    @Transactional
    public boolean addProjectMember(Long projectId, Long userId) {
        // 使用原生SQL插入项目成员关系
        try {
            projectMapper.selectById(projectId); // 验证项目存在
            userMapper.selectById(userId); // 验证用户存在
            // 这里应该有插入project_members表的逻辑，暂时返回true
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeProjectMember(Long projectId, Long userId) {
        // 删除项目成员关系的逻辑
        return true;
    }

    @Override
    @Transactional
    public boolean updateProjectProgress(Long projectId, Integer progress) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        project.setProgress(progress);
        return projectMapper.updateById(project) > 0;
    }

    @Override
    public List<Project> getProjectOverview() {
        // 获取所有项目，按创建时间排序
        List<Project> projects = projectMapper.selectProjectsOrderByCreateTime();

        if (projects.isEmpty()) {
            return projects;
        }

        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                userIds.add(project.getCreatorId());
            }
            if (project.getAssigneeId() != null) {
                userIds.add(project.getAssigneeId());
            }
        }

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 设置creator和assignee对象
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                project.setCreator(userMap.get(project.getCreatorId()));
            }
            if (project.getAssigneeId() != null) {
                project.setAssignee(userMap.get(project.getAssigneeId()));
            }
        }

        return projects;
    }

    @Override
    public List<Project> getProjectOverviewByUser(Long userId) {
        // 获取用户相关的项目ID集合
        Set<Long> userRelatedProjectIds = new HashSet<>();

        // 1. 获取用户作为创建人或责任人的项目
        List<Project> userProjects = projectMapper.selectProjectsByCreatorOrAssignee(userId);
        for (Project project : userProjects) {
            userRelatedProjectIds.add(project.getId());
        }

        // 2. 获取用户负责的待办事项所在的项目
        QueryWrapper<Todo> todoQuery = new QueryWrapper<>();
        todoQuery.eq("assignee_id", userId);
        List<Todo> userTodos = todoMapper.selectList(todoQuery);
        for (Todo todo : userTodos) {
            if (todo.getProjectId() != null) {
                userRelatedProjectIds.add(todo.getProjectId());
            }
        }

        // 如果没有相关项目，返回空列表
        if (userRelatedProjectIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取这些项目的详细信息（按创建时间排序）
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.in("id", userRelatedProjectIds)
                .orderByAsc("create_time");
        List<Project> projects = projectMapper.selectList(projectQuery);

        if (projects.isEmpty()) {
            return projects;
        }

        // 4. 批量设置用户信息
        Set<Long> userIds = new HashSet<>();
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                userIds.add(project.getCreatorId());
            }
            if (project.getAssigneeId() != null) {
                userIds.add(project.getAssigneeId());
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                project.setCreator(userMap.get(project.getCreatorId()));
            }
            if (project.getAssigneeId() != null) {
                project.setAssignee(userMap.get(project.getAssigneeId()));
            }
        }

        return projects;
    }

    @Override
    @Transactional
    public void updateAllProjectWorkPlans() {
        updateAllProjectWorkPlans(7); // 默认7天
    }

    @Override
    @Transactional
    public void updateAllProjectWorkPlans(Integer days) {
        // 获取所有项目
        List<Project> projects = projectMapper.selectList(null);

        // 计算指定天数的日期范围
        LocalDate today = LocalDate.now();
        LocalDate startOfThisPeriod = today.minusDays(days - 1);
        LocalDate endOfThisPeriod = today;
        LocalDate startOfNextPeriod = today.plusDays(1);
        LocalDate endOfNextPeriod = today.plusDays(days);

        for (Project project : projects) {
            // 查询项目相关的待办任务
            QueryWrapper<Todo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", project.getId());
            List<Todo> todos = todoMapper.selectList(queryWrapper);

            // 生成当前周期工作内容
            String thisWeekWork = generatePeriodWork(todos, startOfThisPeriod, endOfThisPeriod, days);
            String milestoneThisWeekWork = generateMilestoneBasedPeriodWork(project, startOfThisPeriod, endOfThisPeriod, days);

            // 合并任务和项目整体状态
            thisWeekWork = combineWorkContent(thisWeekWork, milestoneThisWeekWork);

            // 生成下个周期计划内容
            String nextWeekPlan = generatePeriodPlan(todos, startOfNextPeriod, endOfNextPeriod, days);
            String milestoneNextWeekPlan = generateMilestoneBasedPeriodPlan(project, startOfNextPeriod, endOfNextPeriod, days);

            // 合并任务和项目整体计划
            nextWeekPlan = combineWorkContent(nextWeekPlan, milestoneNextWeekPlan);

            // 更新项目
            project.setThisWeekWork(thisWeekWork);
            project.setNextWeekPlan(nextWeekPlan);
            projectMapper.updateById(project);
        }
    }

    /**
     * 生成指定周期工作内容
     */
    private String generatePeriodWork(List<Todo> todos, LocalDate startDate, LocalDate endDate, Integer days) {
        StringBuilder content = new StringBuilder();
        String periodDesc = days == 7 ? "本周" : "近" + days + "天";

        // 指定周期内已完成的任务
        List<Todo> completedTodos = todos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()) &&
                        todo.getCompletedTime() != null &&
                        !todo.getCompletedTime().toLocalDate().isBefore(startDate) &&
                        !todo.getCompletedTime().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());

        // 进行中的任务
        List<Todo> inProgressTodos = todos.stream()
                .filter(todo -> "PROGRESS".equals(todo.getStatus()))
                .collect(Collectors.toList());

        if (!completedTodos.isEmpty()) {
            content.append("已完成任务：\n");
            for (Todo todo : completedTodos) {
                content.append("- ").append(todo.getTitle());
                if (todo.getCompletedTime() != null) {
                    content.append("（完成时间：").append(todo.getCompletedTime().toLocalDate()).append("）");
                }
                content.append("\n");
            }
        }

        if (!inProgressTodos.isEmpty()) {
            if (content.length() > 0)
                content.append("\n");
            content.append("进行中任务：\n");
            for (Todo todo : inProgressTodos) {
                content.append("- ").append(todo.getTitle());
                content.append("\n");
            }
        }

        return content.length() > 0 ? content.toString().trim() : null;
    }

    /**
     * 生成指定周期计划内容
     */
    private String generatePeriodPlan(List<Todo> todos, LocalDate startDate, LocalDate endDate, Integer days) {
        StringBuilder content = new StringBuilder();
        String periodDesc = days == 7 ? "下周" : "未来" + days + "天";

        // 计划任务（未完成的任务）
        List<Todo> plannedTodos = todos.stream()
                .filter(todo -> !"DONE".equals(todo.getStatus()))
                .collect(Collectors.toList());

        // 指定周期内截止的任务
        List<Todo> periodDueTodos = plannedTodos.stream()
                .filter(todo -> todo.getDueDate() != null &&
                        !todo.getDueDate().isBefore(startDate) &&
                        !todo.getDueDate().isAfter(endDate))
                .collect(Collectors.toList());

        if (!periodDueTodos.isEmpty()) {
            content.append(periodDesc).append("截止任务：\n");
            for (Todo todo : periodDueTodos) {
                content.append("- ").append(todo.getTitle());
                content.append("（截止：").append(todo.getDueDate()).append("）");
                content.append("\n");
            }
        }

        // 其他待办任务
        List<Todo> otherTodos = plannedTodos.stream()
                .filter(todo -> todo.getDueDate() == null ||
                        todo.getDueDate().isBefore(startDate) ||
                        todo.getDueDate().isAfter(endDate))
                .limit(5) // 限制显示数量
                .collect(Collectors.toList());

        if (!otherTodos.isEmpty()) {
            if (content.length() > 0)
                content.append("\n");
            content.append(periodDesc).append("计划：\n");
            for (Todo todo : otherTodos) {
                content.append("- ").append(todo.getTitle());
                if (todo.getDueDate() != null) {
                    content.append("（截止：").append(todo.getDueDate()).append("）");
                }
                content.append("\n");
            }
        }

        return content.length() > 0 ? content.toString().trim() : null;
    }

    /**
     * 基于里程碑生成指定周期工作内容
     */
    private String generateMilestoneBasedPeriodWork(Project project, LocalDate startDate, LocalDate endDate, Integer days) {
        try {
            if (project.getMilestones() == null || project.getMilestones().trim().isEmpty()) {
                return "项目正常推进中，按计划执行各项工作";
            }

            List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            StringBuilder content = new StringBuilder();
            LocalDate today = LocalDate.now();
            String periodDesc = days == 7 ? "本周" : "近" + days + "天";

            // 查找指定周期相关的里程碑
            for (Map<String, Object> milestone : milestones) {
                String name = (String) milestone.get("name");
                String status = (String) milestone.get("status");
                String dueDateStr = (String) milestone.get("dueDate");

                if (name == null || name.trim().isEmpty())
                    continue;

                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);

                    // 指定周期内到期的里程碑
                    if (!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
                        if ("COMPLETED".equals(status)) {
                            content.append("已完成里程碑：").append(name).append("\n");
                        } else {
                            content.append("推进里程碑：").append(name).append("（").append(periodDesc).append("截止）\n");
                        }
                    }
                    // 进行中的里程碑
                    else if ("PROGRESS".equals(status)) {
                        long daysLeft = ChronoUnit.DAYS.between(today, dueDate);
                        content.append("正常进行：").append(name);
                        if (daysLeft > 0) {
                            content.append("（剩余").append(daysLeft).append("天）");
                        } else if (daysLeft < 0) {
                            content.append("（已逾期").append(Math.abs(daysLeft)).append("天）");
                        }
                        content.append("\n");
                    }
                }
            }

            if (content.length() == 0) {
                // 根据当前里程碑的预估进度生成工作内容
                String workContent = generateWorkContentByMilestoneProgress(milestones, today);
                content.append(workContent);
            }

            return content.toString().trim();
        } catch (Exception e) {
            return "完成上线，交付试用";
        }
    }

    /**
     * 基于里程碑生成指定周期计划内容
     */
    private String generateMilestoneBasedPeriodPlan(Project project, LocalDate startDate, LocalDate endDate, Integer days) {
        try {
            if (project.getMilestones() == null || project.getMilestones().trim().isEmpty()) {
                return "继续推进项目各项工作，确保按时完成既定目标";
            }

            List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            StringBuilder content = new StringBuilder();
            LocalDate today = LocalDate.now();
            String periodDesc = days == 7 ? "下周" : "未来" + days + "天";

            // 查找指定周期相关的里程碑
            for (Map<String, Object> milestone : milestones) {
                String name = (String) milestone.get("name");
                String status = (String) milestone.get("status");
                String dueDateStr = (String) milestone.get("dueDate");

                if (name == null || name.trim().isEmpty())
                    continue;

                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);

                    // 指定周期内到期的里程碑
                    if (!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
                        if (!"COMPLETED".equals(status)) {
                            content.append("完成里程碑：").append(name).append("（").append(periodDesc).append("截止）\n");
                        }
                    }
                    // 即将开始的里程碑
                    else if ("PENDING".equals(status) && dueDate.isAfter(endDate)) {
                        long daysUntilStart = ChronoUnit.DAYS.between(today, dueDate);
                        if (daysUntilStart <= days * 2) { // 两个周期内的里程碑
                            content.append("准备启动：").append(name).append("（").append(daysUntilStart).append("天后开始）\n");
                        }
                    }
                }
            }

            if (content.length() == 0) {
                // 根据当前里程碑的预估进度生成计划内容
                String planContent = generatePlanContentByMilestoneProgress(milestones, today);
                content.append(planContent);
            }

            return content.toString().trim();
        } catch (Exception e) {
            return "收集和处理用户反馈，持续优化用户体验";
        }
    }

    /**
     * 根据当前里程碑的预估进度生成工作内容
     */
    private String generateWorkContentByMilestoneProgress(List<Map<String, Object>> milestones, LocalDate today) {
        // 找到当前正在进行的里程碑或最近的里程碑
        Map<String, Object> currentMilestone = findCurrentMilestone(milestones, today);

        if (currentMilestone == null) {
            return "完成上线，交付试用";
        }

        String name = (String) currentMilestone.get("name");
        String status = (String) currentMilestone.get("status");
        String dueDateStr = (String) currentMilestone.get("dueDate");

        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            return "推进里程碑：" + name + "（进度未知），按计划完成相关工作";
        }

        try {
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            double progress = calculateMilestoneTimeProgress(milestones, currentMilestone, today);

            StringBuilder content = new StringBuilder();
            content.append("推进里程碑：").append(name);
            content.append("（预估进度").append(String.format("%.0f", progress * 100)).append("%）");

            if ("COMPLETED".equals(status)) {
                content.append("，已完成");
            } else if ("PROGRESS".equals(status)) {
                content.append("，进行中");
            } else {
                if (progress < 0.3) {
                    content.append("，启动阶段工作，完善需求和方案设计");
                } else if (progress < 0.7) {
                    content.append("，开发阶段工作，推进核心功能实现");
                } else {
                    content.append("，收尾阶段工作，完成测试和优化");
                }
            }

            return content.toString();
        } catch (Exception e) {
            return "推进里程碑：" + name + "（进度未知），按计划完成相关工作";
        }
    }

    /**
     * 根据当前里程碑的预估进度生成计划内容
     */
    private String generatePlanContentByMilestoneProgress(List<Map<String, Object>> milestones, LocalDate today) {
        // 找到下一个里程碑或当前里程碑
        Map<String, Object> nextMilestone = findNextMilestone(milestones, today);

        if (nextMilestone == null) {
            return "收集和处理用户反馈，持续优化用户体验";
        }

        String name = (String) nextMilestone.get("name");
        String status = (String) nextMilestone.get("status");
        String dueDateStr = (String) nextMilestone.get("dueDate");

        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            return "计划推进里程碑：" + name + "（进度未知），制定详细实施方案";
        }

        try {
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            double progress = calculateMilestoneTimeProgress(milestones, nextMilestone, today);
            long daysLeft = ChronoUnit.DAYS.between(today, dueDate);

            StringBuilder content = new StringBuilder();
            content.append("持续推进：").append(name);
            content.append("（预估进度").append(String.format("%.0f", progress * 100)).append("%）");

            if (daysLeft <= 0) {
                content.append("，重点推进");
            } else if (daysLeft <= 7) {
                content.append("，").append(daysLeft).append("天内截止，重点推进");
            } else {
                if (progress < 0.3) {
                } else if (progress < 0.7) {
                } else {
                    content.append("，完善功能测试，准备交付和上线");
                }
            }

            return content.toString();
        } catch (Exception e) {
            return "计划推进里程碑：" + name + "，制定详细实施方案";
        }
    }

    /**
     * 找到当前正在进行的里程碑
     */
    private Map<String, Object> findCurrentMilestone(List<Map<String, Object>> milestones, LocalDate today) {
        // 优先找进行中的里程碑
        for (Map<String, Object> milestone : milestones) {
            String status = (String) milestone.get("status");
            if ("PROGRESS".equals(status)) {
                return milestone;
            }
        }

        // 找最近的未完成里程碑
        Map<String, Object> nearestMilestone = null;
        long minDays = Long.MAX_VALUE;

        for (Map<String, Object> milestone : milestones) {
            String status = (String) milestone.get("status");
            String dueDateStr = (String) milestone.get("dueDate");

            if (!"COMPLETED".equals(status) && dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                try {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    long days = Math.abs(ChronoUnit.DAYS.between(today, dueDate));
                    if (days < minDays) {
                        minDays = days;
                        nearestMilestone = milestone;
                    }
                } catch (Exception e) {
                    // 忽略日期解析错误
                }
            }
        }

        return nearestMilestone;
    }

    /**
     * 找到下一个里程碑
     */
    private Map<String, Object> findNextMilestone(List<Map<String, Object>> milestones, LocalDate today) {
        Map<String, Object> nextMilestone = null;
        long minDays = Long.MAX_VALUE;

        for (Map<String, Object> milestone : milestones) {
            String status = (String) milestone.get("status");
            String dueDateStr = (String) milestone.get("dueDate");

            if (!"COMPLETED".equals(status) && dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                try {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    long days = ChronoUnit.DAYS.between(today, dueDate);
                    if (days >= 0 && days < minDays) {
                        minDays = days;
                        nextMilestone = milestone;
                    }
                } catch (Exception e) {
                    // 忽略日期解析错误
                }
            }
        }

        // 如果没有找到未来的里程碑，返回当前里程碑
        if (nextMilestone == null) {
            nextMilestone = findCurrentMilestone(milestones, today);
        }

        return nextMilestone;
    }

    /**
     * 计算里程碑的预估进度
     */
    private double calculateMilestoneTimeProgress(List<Map<String, Object>> milestones, Map<String, Object> milestone,
            LocalDate today) {
        String dueDateStr = (String) milestone.get("dueDate");
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            return 0.5; // 默认50%进度
        }

        try {
            LocalDate dueDate = LocalDate.parse(dueDateStr);

            // 找到上一个里程碑的结束时间作为当前里程碑的开始时间
            LocalDate startDate = findPreviousMilestoneEndDate(milestones, milestone, today);

            // 如果今天在开始时间之前，进度为0
            if (today.isBefore(startDate)) {
                return 0.0;
            }

            // 如果今天在截止时间之后，进度为100%
            if (today.isAfter(dueDate)) {
                return 1.0;
            }

            // 计算预估进度
            long totalDays = ChronoUnit.DAYS.between(startDate, dueDate);
            long passedDays = ChronoUnit.DAYS.between(startDate, today);

            if (totalDays <= 0) {
                return 1.0;
            }

            return Math.min(1.0, Math.max(0.0, (double) passedDays / totalDays));
        } catch (Exception e) {
            return 0.5; // 默认50%进度
        }
    }

    /**
     * 找到上一个里程碑的结束时间作为当前里程碑的开始时间
     */
    private LocalDate findPreviousMilestoneEndDate(List<Map<String, Object>> milestones,
            Map<String, Object> currentMilestone, LocalDate today) {
        String currentDueDateStr = (String) currentMilestone.get("dueDate");
        if (currentDueDateStr == null || currentDueDateStr.trim().isEmpty()) {
            // 如果当前里程碑没有截止时间，默认开始时间为30天前
            return today.minusDays(30);
        }

        try {
            LocalDate currentDueDate = LocalDate.parse(currentDueDateStr);
            LocalDate previousEndDate = null;

            // 找到在当前里程碑之前且最接近的里程碑
            for (Map<String, Object> milestone : milestones) {
                String dueDateStr = (String) milestone.get("dueDate");
                if (dueDateStr != null && !dueDateStr.trim().isEmpty() && !milestone.equals(currentMilestone)) {
                    try {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        // 找到在当前里程碑截止时间之前的里程碑
                        if (dueDate.isBefore(currentDueDate)) {
                            if (previousEndDate == null || dueDate.isAfter(previousEndDate)) {
                                previousEndDate = dueDate;
                            }
                        }
                    } catch (Exception e) {
                        // 忽略日期解析错误
                    }
                }
            }

            // 如果找到了上一个里程碑，使用其结束时间作为开始时间
            if (previousEndDate != null) {
                return previousEndDate;
            } else {
                // 如果没有找到上一个里程碑，使用当前里程碑截止时间前30天作为开始时间
                return currentDueDate.minusDays(30);
            }
        } catch (Exception e) {
            // 如果解析失败，默认开始时间为30天前
            return today.minusDays(30);
        }
    }

    /**
     * 合并任务内容和项目整体状态
     */
    private String combineWorkContent(String taskContent, String milestoneContent) {
        StringBuilder combined = new StringBuilder();

        // 添加任务内容
        if (taskContent != null && !taskContent.trim().isEmpty()) {
            combined.append(taskContent.trim());
        }

        // 添加项目整体状态
        if (milestoneContent != null && !milestoneContent.trim().isEmpty()) {
            if (combined.length() > 0) {
                combined.append("\n\n");
            }
            combined.append(milestoneContent.trim());
        }

        // 如果都为空，返回默认内容
        if (combined.length() == 0) {
            return "项目正常推进中，按计划执行各项工作";
        }

        return combined.toString();
    }


    

    

    

    
    @Override
    public Map<String, Object> getProjectGanttData(Long projectId) {
        Map<String, Object> ganttData = new HashMap<>();
        
        try {
            // 1. 获取项目基本信息
            Project project = getProjectDetail(projectId);
            if (project == null) {
                return ganttData;
            }
            
            // 2. 计算项目时间范围
            Map<String, Object> timeRange = calculateProjectTimeRange(project, projectId);
            ganttData.put("timeRange", timeRange);
            
            // 3. 获取里程碑数据
            List<Map<String, Object>> milestones = extractMilestonesForGantt(project);
            ganttData.put("milestones", milestones);
            
            // 4. 获取任务轨道数据
            List<Map<String, Object>> taskTracks = buildTaskTracks(projectId, milestones);
            ganttData.put("taskTracks", taskTracks);
            
            // 5. 项目基本信息
            ganttData.put("projectInfo", buildProjectInfo(project));
            
        } catch (Exception e) {
            log.error("获取项目甘特图数据失败", e);
        }
        
        return ganttData;
    }
    
    /**
     * 计算项目时间范围
     */
    private Map<String, Object> calculateProjectTimeRange(Project project, Long projectId) {
        LocalDate startDate = project.getCreateTime().toLocalDate();
        LocalDate endDate = LocalDate.now().plusMonths(3); // 默认显示到3个月后
        
        try {
            // 从里程碑中获取最晚日期
            if (project.getMilestones() != null && !project.getMilestones().isEmpty()) {
                TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
                List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(), typeRef);
                
                for (Map<String, Object> milestone : milestones) {
                    String dueDateStr = (String) milestone.get("dueDate");
                    if (dueDateStr != null && !dueDateStr.isEmpty()) {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        if (dueDate.isAfter(endDate)) {
                            endDate = dueDate.plusWeeks(2); // 里程碑后再加2周缓冲
                        }
                    }
                }
            }
            
            // 从待办任务中获取最晚日期
            List<Todo> projectTodos = todoService.getCompletedTodosByProject(projectId);
            for (Todo todo : projectTodos) {
                if (todo.getDueDate() != null && todo.getDueDate().isAfter(endDate)) {
                    endDate = todo.getDueDate().plusWeeks(1);
                }
            }
            
        } catch (Exception e) {
            log.warn("计算项目时间范围时发生错误: {}", e.getMessage());
        }
        
        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("startDate", startDate.toString());
        timeRange.put("endDate", endDate.toString());
        timeRange.put("totalDays", ChronoUnit.DAYS.between(startDate, endDate));
        timeRange.put("currentDate", LocalDate.now().toString());
        
        return timeRange;
    }
    
    /**
     * 提取里程碑数据用于甘特图
     */
    private List<Map<String, Object>> extractMilestonesForGantt(Project project) {
        List<Map<String, Object>> milestones = new ArrayList<>();
        
        if (project.getMilestones() == null || project.getMilestones().isEmpty()) {
            return milestones;
        }
        
        try {
            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
            List<Map<String, Object>> originalMilestones = objectMapper.readValue(project.getMilestones(), typeRef);
            
            for (Map<String, Object> milestone : originalMilestones) {
                String dueDateStr = (String) milestone.get("dueDate");
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    Map<String, Object> ganttMilestone = new HashMap<>();
                    ganttMilestone.put("id", "milestone_" + milestone.hashCode());
                    ganttMilestone.put("name", milestone.get("name"));
                    ganttMilestone.put("description", milestone.get("description"));
                    ganttMilestone.put("dueDate", dueDateStr);
                    ganttMilestone.put("status", milestone.get("status"));
                    ganttMilestone.put("color", getMilestoneColor((String) milestone.get("status")));
                    milestones.add(ganttMilestone);
                }
            }
            
            // 按日期排序
            milestones.sort((a, b) -> {
                String dateA = (String) a.get("dueDate");
                String dateB = (String) b.get("dueDate");
                return dateA.compareTo(dateB);
            });
            
        } catch (Exception e) {
            log.warn("提取里程碑数据失败: {}", e.getMessage());
        }
        
        return milestones;
    }
    
    /**
     * 构建任务轨道数据
     */
    private List<Map<String, Object>> buildTaskTracks(Long projectId, List<Map<String, Object>> milestones) {
        List<Map<String, Object>> taskTracks = new ArrayList<>();
        
        try {
            // 获取项目的所有待办任务
            List<Todo> allTodos = todoService.getCompletedTodosByProject(projectId);
            
            // 按模块/类型分组任务
            Map<String, List<Todo>> todoGroups = groupTodosByCategory(allTodos);
            
            // 为每个分组创建任务轨道
            for (Map.Entry<String, List<Todo>> entry : todoGroups.entrySet()) {
                Map<String, Object> track = new HashMap<>();
                track.put("id", "track_" + entry.getKey().hashCode());
                track.put("name", entry.getKey());
                track.put("tasks", buildTaskBars(entry.getValue()));
                track.put("milestoneRelation", findTrackMilestoneRelation(entry.getValue(), milestones));
                taskTracks.add(track);
            }
            
            // 如果没有足够的任务分组，创建默认轨道
            if (taskTracks.isEmpty()) {
                taskTracks.add(createDefaultTrack(allTodos));
            }
            
        } catch (Exception e) {
            log.warn("构建任务轨道数据失败: {}", e.getMessage());
        }
        
        return taskTracks;
    }
    
    /**
     * 按类别分组待办任务
     */
    private Map<String, List<Todo>> groupTodosByCategory(List<Todo> todos) {
        Map<String, List<Todo>> groups = new HashMap<>();
        
        for (Todo todo : todos) {
            String category = determineTodoCategory(todo);
            groups.computeIfAbsent(category, k -> new ArrayList<>()).add(todo);
        }
        
        return groups;
    }
    
    /**
     * 确定待办任务的类别
     */
    private String determineTodoCategory(Todo todo) {
        String title = todo.getTitle().toLowerCase();
        String description = todo.getDescription() != null ? todo.getDescription().toLowerCase() : "";
        
        // 根据关键词判断类别
        if (title.contains("前端") || title.contains("ui") || title.contains("页面") || title.contains("界面")) {
            return "前端开发";
        } else if (title.contains("后端") || title.contains("api") || title.contains("接口") || title.contains("服务")) {
            return "后端开发";
        } else if (title.contains("测试") || title.contains("bug") || title.contains("调试")) {
            return "测试验证";
        } else if (title.contains("部署") || title.contains("上线") || title.contains("发布")) {
            return "部署上线";
        } else if (title.contains("设计") || title.contains("原型") || title.contains("需求")) {
            return "需求设计";
        } else {
            return "通用任务";
        }
    }
    
    /**
     * 构建任务条数据
     */
    private List<Map<String, Object>> buildTaskBars(List<Todo> todos) {
        List<Map<String, Object>> taskBars = new ArrayList<>();
        
        // 🚀 批量查询用户信息，避免N+1查询问题
        Map<Long, User> userCache = batchQueryUsers(todos);
        
        for (Todo todo : todos) {
            Map<String, Object> taskBar = new HashMap<>();
            taskBar.put("id", "task_" + todo.getId());
            taskBar.put("title", todo.getTitle());
            taskBar.put("description", todo.getDescription());
            taskBar.put("priority", todo.getPriority());
            taskBar.put("status", todo.getStatus());
            
            // 添加处理人信息（从缓存中获取）
            if (todo.getAssigneeId() != null) {
                taskBar.put("assigneeId", todo.getAssigneeId());
                User assignee = userCache.get(todo.getAssigneeId());
                if (assignee != null) {
                    Map<String, Object> assigneeInfo = new HashMap<>();
                    assigneeInfo.put("id", assignee.getId());
                    assigneeInfo.put("username", assignee.getUsername());
                    assigneeInfo.put("nickname", assignee.getNickname());
                    taskBar.put("assignee", assigneeInfo);
                }
            }
            
            // 时间信息
            LocalDate startDate = todo.getCreateTime() != null ? todo.getCreateTime().toLocalDate() : LocalDate.now();
            LocalDate endDate = todo.getDueDate() != null ? todo.getDueDate() : startDate.plusDays(7); // 默认7天
            LocalDate completeDate = todo.getCompletedTime() != null ? todo.getCompletedTime().toLocalDate() : null;
            
            taskBar.put("startDate", startDate.toString());
            taskBar.put("endDate", endDate.toString());
            if (completeDate != null) {
                taskBar.put("completeDate", completeDate.toString());
            }
            
            // 状态和颜色
            TaskBarStatus barStatus = calculateTaskBarStatus(todo, startDate, endDate, completeDate);
            taskBar.put("color", barStatus.color);
            taskBar.put("statusText", barStatus.statusText);
            taskBar.put("progress", barStatus.progress);
            
            if (barStatus.delayDays > 0) {
                taskBar.put("delayDays", barStatus.delayDays);
            }
            
            taskBars.add(taskBar);
        }
        
        // 按开始时间排序
        taskBars.sort((a, b) -> {
            String dateA = (String) a.get("startDate");
            String dateB = (String) b.get("startDate");
            return dateA.compareTo(dateB);
        });
        
        return taskBars;
    }
    
    /**
     * 批量查询用户信息，避免N+1查询问题
     */
    private Map<Long, User> batchQueryUsers(List<Todo> todos) {
        // 收集所有需要查询的用户ID
        Set<Long> userIds = todos.stream()
            .filter(todo -> todo.getAssigneeId() != null)
            .map(Todo::getAssigneeId)
            .collect(Collectors.toSet());
        
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // 🎯 一次性批量查询所有用户
            List<User> users = userMapper.selectBatchIds(userIds);
            log.info("批量查询{}个不同用户信息", userIds.size());
            
            // 转换为Map方便查找
            return users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        } catch (Exception e) {
            log.error("批量查询用户信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 构建任务条（使用预缓存的用户信息）
     */
    private List<Map<String, Object>> buildTaskBarsWithCache(List<Todo> todos, Map<Long, User> userCache) {
        List<Map<String, Object>> taskBars = new ArrayList<>();
        
        for (Todo todo : todos) {
            Map<String, Object> taskBar = new HashMap<>();
            taskBar.put("id", "task_" + todo.getId());
            taskBar.put("title", todo.getTitle());
            taskBar.put("description", todo.getDescription());
            taskBar.put("priority", todo.getPriority());
            taskBar.put("status", todo.getStatus());
            
            // 添加处理人信息（直接从缓存获取）
            if (todo.getAssigneeId() != null) {
                taskBar.put("assigneeId", todo.getAssigneeId());
                User assignee = userCache.get(todo.getAssigneeId());
                if (assignee != null) {
                    Map<String, Object> assigneeInfo = new HashMap<>();
                    assigneeInfo.put("id", assignee.getId());
                    assigneeInfo.put("username", assignee.getUsername());
                    assigneeInfo.put("nickname", assignee.getNickname());
                    taskBar.put("assignee", assigneeInfo);
                }
            }
            
            // 时间信息
            LocalDate startDate = todo.getCreateTime() != null ? todo.getCreateTime().toLocalDate() : LocalDate.now();
            LocalDate endDate = todo.getDueDate() != null ? todo.getDueDate() : startDate.plusDays(7); // 默认7天
            LocalDate completeDate = todo.getCompletedTime() != null ? todo.getCompletedTime().toLocalDate() : null;
            
            taskBar.put("startDate", startDate.toString());
            taskBar.put("endDate", endDate.toString());
            if (completeDate != null) {
                taskBar.put("completeDate", completeDate.toString());
            }
            
            // 状态和颜色
            TaskBarStatus barStatus = calculateTaskBarStatus(todo, startDate, endDate, completeDate);
            taskBar.put("color", barStatus.color);
            taskBar.put("statusText", barStatus.statusText);
            taskBar.put("progress", barStatus.progress);
            
            if (barStatus.delayDays > 0) {
                taskBar.put("delayDays", barStatus.delayDays);
            }
            
            taskBars.add(taskBar);
        }
        
        // 按开始时间排序
        taskBars.sort((a, b) -> {
            String dateA = (String) a.get("startDate");
            String dateB = (String) b.get("startDate");
            return dateA.compareTo(dateB);
        });
        
        return taskBars;
    }

    /**
     * 计算任务条状态
     */
    private TaskBarStatus calculateTaskBarStatus(Todo todo, LocalDate startDate, LocalDate endDate, LocalDate completeDate) {
        LocalDate today = LocalDate.now();
        
        // 🔧 修复状态判断 - Todo的状态是"DONE"而不是"COMPLETED"
        if ("DONE".equals(todo.getStatus())) {
            // 已完成任务的精确状态分类
            if (completeDate != null && endDate != null) {
                long delayDays = ChronoUnit.DAYS.between(endDate, completeDate);
                if (delayDays < 0) {
                    // 提前完成
                    return new TaskBarStatus("#00b42a", "提前完成", 100, 0); // 深绿色
                } else if (delayDays == 0) {
                    // 按时完成
                    return new TaskBarStatus("#52c41a", "按时完成", 100, 0); // 绿色
                } else if (delayDays <= 2) {
                    // 轻微延期完成
                    return new TaskBarStatus("#fa8c16", "轻微延期完成", 100, (int) delayDays); // 橙色
                } else {
                    // 严重延期完成
                    return new TaskBarStatus("#f5222d", "延期完成", 100, (int) delayDays); // 红色
                }
            } else {
                // 没有截止日期或完成时间，默认为按时完成
                return new TaskBarStatus("#52c41a", "已完成", 100, 0);
            }
        } else {
            // 进行中任务的精确状态分类
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
            long passedDays = ChronoUnit.DAYS.between(startDate, today);
            
            int progress;
            if (totalDays == 0) {
                // 当天任务的进度计算
                if (today.equals(startDate)) {
                    if ("PROGRESS".equals(todo.getStatus())) {
                        progress = 50; // 进行中显示50%
                    } else if ("TODO".equals(todo.getStatus())) {
                        progress = 10; // 待办显示10%（已开始但未完成）
                    } else {
                        progress = 0; // 其他状态显示0%
                    }
                } else if (today.isAfter(startDate)) {
                    // 已过期的当天任务
                    progress = 90; // 显示90%表示应该完成但未完成
                } else {
                    // 未来的当天任务
                    progress = 0;
                }
            } else {
                // 多天任务的正常计算
                progress = Math.min(100, Math.max(0, (int) (passedDays * 100 / totalDays)));
            }
            
            // 进行中任务的状态分类
            if (today.isAfter(endDate)) {
                // 已经逾期的进行中任务
                long overdueDays = ChronoUnit.DAYS.between(endDate, today);
                return new TaskBarStatus("#a8071a", "逾期进行", progress, (int) overdueDays); // 深红色
            } else if (ChronoUnit.DAYS.between(today, endDate) <= 1) {
                // 即将到期的进行中任务
                return new TaskBarStatus("#722ed1", "即将到期", progress, 0); // 紫色
            } else {
                // 正常进行中的任务
                return new TaskBarStatus("#1890ff", "正常进行", progress, 0); // 蓝色
            }
        }
    }
    
    /**
     * 查找轨道与里程碑的关系
     */
    private List<String> findTrackMilestoneRelation(List<Todo> todos, List<Map<String, Object>> milestones) {
        List<String> relations = new ArrayList<>();
        
        for (Map<String, Object> milestone : milestones) {
            String milestoneDate = (String) milestone.get("dueDate");
            LocalDate mDate = LocalDate.parse(milestoneDate);
            
            for (Todo todo : todos) {
                LocalDate todoEnd = todo.getDueDate() != null ? todo.getDueDate() : LocalDate.now();
                if (Math.abs(ChronoUnit.DAYS.between(mDate, todoEnd)) <= 7) { // 7天内关联
                    relations.add((String) milestone.get("id"));
                    break;
                }
            }
        }
        
        return relations;
    }
    
    /**
     * 创建默认任务轨道
     */
    private Map<String, Object> createDefaultTrack(List<Todo> todos) {
        Map<String, Object> track = new HashMap<>();
        track.put("id", "track_default");
        track.put("name", "项目任务");
        track.put("tasks", buildTaskBars(todos));
        track.put("milestoneRelation", new ArrayList<>());
        return track;
    }
    
    /**
     * 构建项目基本信息
     */
    private Map<String, Object> buildProjectInfo(Project project) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", project.getId());
        info.put("name", project.getName());
        info.put("description", project.getDescription());
        info.put("status", project.getStatus());
        info.put("progress", project.getProgress());
        info.put("createTime", project.getCreateTime().toLocalDate().toString());
        return info;
    }
    
    /**
     * 获取里程碑颜色
     */
    private String getMilestoneColor(String status) {
        switch (status) {
            case "COMPLETED": return "#52c41a";
            case "PROGRESS": return "#1890ff";
            case "PENDING": return "#8c8c8c";
            default: return "#8c8c8c";
        }
    }
    
    /**
     * 任务条状态内部类
     */
    private static class TaskBarStatus {
        final String color;
        final String statusText;
        final int progress;
        final int delayDays;
        
        TaskBarStatus(String color, String statusText, int progress, int delayDays) {
            this.color = color;
            this.statusText = statusText;
            this.progress = progress;
            this.delayDays = delayDays;
        }
    }
    
    @Override
    public Map<String, Object> getUserProjectsGanttData(Long userId) {
        Map<String, Object> ganttData = new HashMap<>();
        
        try {
            // 1. 获取当前用户相关的所有项目
            List<Project> userProjects = getProjectListByUser(userId);
            if (userProjects.isEmpty()) {
                return ganttData;
            }
            
            // 2. 🚀 一次性批量查询所有项目的待办任务（性能优化关键）
            List<Long> projectIds = userProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
            Map<Long, List<Todo>> projectTodosMap = todoService.getTodosByProjects(projectIds);
            log.info("批量查询{}个项目的待办任务，共{}条记录", projectIds.size(), 
                projectTodosMap.values().stream().mapToInt(List::size).sum());
            
            // 3. 计算全局时间范围（使用缓存的待办任务）
            Map<String, Object> globalTimeRange = calculateGlobalTimeRangeOptimized(userProjects, projectTodosMap);
            ganttData.put("timeRange", globalTimeRange);
            
            // 4. 聚合所有项目的里程碑
            List<Map<String, Object>> allMilestones = aggregateAllMilestones(userProjects);
            ganttData.put("milestones", allMilestones);
            
            // 5. 构建项目轨道（使用缓存的待办任务）
            List<Map<String, Object>> projectTracks = buildProjectTracksOptimized(userProjects, allMilestones, projectTodosMap);
            ganttData.put("taskTracks", projectTracks);
            
            // 6. 全局项目信息
            ganttData.put("projectInfo", buildGlobalProjectInfo(userProjects));
            
        } catch (Exception e) {
            log.error("获取用户全局甘特图数据失败", e);
        }
        
        return ganttData;
    }
    
    /**
     * 计算全局时间范围（优化版 - 使用缓存的待办任务）
     */
    private Map<String, Object> calculateGlobalTimeRangeOptimized(List<Project> projects, Map<Long, List<Todo>> projectTodosMap) {
        LocalDate earliestDate = LocalDate.now();
        LocalDate latestDate = LocalDate.now().plusMonths(1);
        
        for (Project project : projects) {
            // 项目创建时间
            LocalDate projectStart = project.getCreateTime().toLocalDate();
            if (projectStart.isBefore(earliestDate)) {
                earliestDate = projectStart;
            }
            
            // 项目里程碑时间
            if (project.getMilestones() != null && !project.getMilestones().isEmpty()) {
                try {
                    TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
                    List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(), typeRef);
                    
                    for (Map<String, Object> milestone : milestones) {
                        String dueDateStr = (String) milestone.get("dueDate");
                        if (dueDateStr != null && !dueDateStr.isEmpty()) {
                            LocalDate dueDate = LocalDate.parse(dueDateStr);
                            if (dueDate.isBefore(earliestDate)) {
                                earliestDate = dueDate;
                            }
                            if (dueDate.isAfter(latestDate)) {
                                latestDate = dueDate;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析项目{}里程碑时间失败: {}", project.getId(), e.getMessage());
                }
            }
            
            // 使用缓存的待办任务计算时间范围
            List<Todo> projectTodos = projectTodosMap.getOrDefault(project.getId(), new ArrayList<>());
            for (Todo todo : projectTodos) {
                if (todo.getCreateTime() != null) {
                    LocalDate todoStart = todo.getCreateTime().toLocalDate();
                    if (todoStart.isBefore(earliestDate)) {
                        earliestDate = todoStart;
                    }
                }
                if (todo.getDueDate() != null) {
                    LocalDate todoEnd = todo.getDueDate();
                    if (todoEnd.isAfter(latestDate)) {
                        latestDate = todoEnd;
                    }
                }
            }
        }
        
        // 确保时间范围至少有3个月
        if (ChronoUnit.DAYS.between(earliestDate, latestDate) < 90) {
            latestDate = earliestDate.plusMonths(3);
        }
        
        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("startDate", earliestDate.toString());
        timeRange.put("endDate", latestDate.toString());
        timeRange.put("totalDays", ChronoUnit.DAYS.between(earliestDate, latestDate));
        timeRange.put("currentDate", LocalDate.now().toString());
        
        return timeRange;
    }
    
    /**
     * 计算全局时间范围
     */
    private Map<String, Object> calculateGlobalTimeRange(List<Project> projects) {
        LocalDate earliestDate = LocalDate.now();
        LocalDate latestDate = LocalDate.now().plusMonths(1);
        
        for (Project project : projects) {
            // 项目创建时间
            LocalDate projectStart = project.getCreateTime().toLocalDate();
            if (projectStart.isBefore(earliestDate)) {
                earliestDate = projectStart;
            }
            
            // 项目里程碑时间
            if (project.getMilestones() != null && !project.getMilestones().isEmpty()) {
                try {
                    TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
                    List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(), typeRef);
                    
                    for (Map<String, Object> milestone : milestones) {
                        String dueDateStr = (String) milestone.get("dueDate");
                        if (dueDateStr != null && !dueDateStr.isEmpty()) {
                            LocalDate dueDate = LocalDate.parse(dueDateStr);
                            if (dueDate.isBefore(earliestDate)) {
                                earliestDate = dueDate;
                            }
                            if (dueDate.isAfter(latestDate)) {
                                latestDate = dueDate;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析项目{}里程碑时间失败: {}", project.getId(), e.getMessage());
                }
            }
            
            // 项目待办任务时间
            try {
                List<Todo> projectTodos = getAllTodosByProject(project.getId());
                for (Todo todo : projectTodos) {
                    if (todo.getCreateTime() != null) {
                        LocalDate todoStart = todo.getCreateTime().toLocalDate();
                        if (todoStart.isBefore(earliestDate)) {
                            earliestDate = todoStart;
                        }
                    }
                    if (todo.getDueDate() != null) {
                        LocalDate todoEnd = todo.getDueDate();
                        if (todoEnd.isAfter(latestDate)) {
                            latestDate = todoEnd;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取项目{}待办任务时间失败: {}", project.getId(), e.getMessage());
            }
        }
        
        // 确保时间范围至少有3个月
        if (ChronoUnit.DAYS.between(earliestDate, latestDate) < 90) {
            latestDate = earliestDate.plusMonths(3);
        }
        
        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("startDate", earliestDate.toString());
        timeRange.put("endDate", latestDate.toString());
        timeRange.put("totalDays", ChronoUnit.DAYS.between(earliestDate, latestDate));
        timeRange.put("currentDate", LocalDate.now().toString());
        
        return timeRange;
    }
    
    /**
     * 聚合所有项目的里程碑
     */
    private List<Map<String, Object>> aggregateAllMilestones(List<Project> projects) {
        List<Map<String, Object>> allMilestones = new ArrayList<>();
        
        for (Project project : projects) {
            List<Map<String, Object>> projectMilestones = extractMilestonesForGantt(project);
            for (Map<String, Object> milestone : projectMilestones) {
                // 添加项目信息到里程碑
                milestone.put("projectId", project.getId());
                milestone.put("projectName", project.getName());
                allMilestones.add(milestone);
            }
        }
        
        // 按日期排序
        allMilestones.sort((a, b) -> {
            String dateA = (String) a.get("dueDate");
            String dateB = (String) b.get("dueDate");
            return dateA.compareTo(dateB);
        });
        
        return allMilestones;
    }
    
    /**
     * 构建项目轨道
     */
    private List<Map<String, Object>> buildProjectTracks(List<Project> projects, List<Map<String, Object>> allMilestones) {
        List<Map<String, Object>> projectTracks = new ArrayList<>();
        
        // 按创建时间排序（从远及近）
        List<Project> sortedProjects = projects.stream()
            .sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()))
            .collect(Collectors.toList());
        
        for (Project project : sortedProjects) {
            Map<String, Object> track = new HashMap<>();
            track.put("id", "project_" + project.getId());
            track.put("name", "📋 " + project.getName());
            track.put("projectId", project.getId());
            track.put("status", project.getStatus());
            track.put("progress", project.getProgress());
            
            // 获取项目的所有待办任务
            List<Todo> projectTodos = getAllTodosByProject(project.getId());
            track.put("tasks", buildTaskBars(projectTodos));
            
            // 获取项目的里程碑（每个项目轨道包含自己的里程碑）
            List<Map<String, Object>> projectMilestones = allMilestones.stream()
                .filter(m -> project.getId().equals(m.get("projectId")))
                .collect(Collectors.toList());
            track.put("milestones", projectMilestones);
            
            // 关联的里程碑（保留向后兼容性）
            List<String> relatedMilestones = projectMilestones.stream()
                .map(m -> (String) m.get("id"))
                .collect(Collectors.toList());
            track.put("milestoneRelation", relatedMilestones);
            
            projectTracks.add(track);
        }
        
        return projectTracks;
    }

    /**
     * 构建项目轨道（优化版 - 使用缓存的待办任务）
     */
    private List<Map<String, Object>> buildProjectTracksOptimized(List<Project> projects, 
            List<Map<String, Object>> allMilestones, Map<Long, List<Todo>> projectTodosMap) {
        List<Map<String, Object>> projectTracks = new ArrayList<>();
        
        // 🚀 预先批量查询所有相关用户信息，进一步优化性能
        List<Todo> allTodos = projectTodosMap.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        Map<Long, User> globalUserCache = batchQueryUsers(allTodos);
        
        // 按创建时间排序（从远及近）
        List<Project> sortedProjects = projects.stream()
            .sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()))
            .collect(Collectors.toList());
        
        for (Project project : sortedProjects) {
            Map<String, Object> track = new HashMap<>();
            track.put("id", "project_" + project.getId());
            track.put("name", "📋 " + project.getName());
            track.put("projectId", project.getId());
            track.put("status", project.getStatus());
            track.put("progress", project.getProgress());
            
            // 🚀 直接使用缓存的待办任务，避免重复SQL查询
            List<Todo> projectTodos = projectTodosMap.getOrDefault(project.getId(), new ArrayList<>());
            track.put("tasks", buildTaskBarsWithCache(projectTodos, globalUserCache));
            
            // 获取项目的里程碑（每个项目轨道包含自己的里程碑）
            List<Map<String, Object>> projectMilestones = allMilestones.stream()
                .filter(m -> project.getId().equals(m.get("projectId")))
                .collect(Collectors.toList());
            track.put("milestones", projectMilestones);
            
            // 关联的里程碑（保留向后兼容性）
            List<String> relatedMilestones = projectMilestones.stream()
                .map(m -> (String) m.get("id"))
                .collect(Collectors.toList());
            track.put("milestoneRelation", relatedMilestones);
            
            projectTracks.add(track);
        }
        
        return projectTracks;
    }
    
    /**
     * 获取项目的所有待办任务（改进版）
     */
    private List<Todo> getAllTodosByProject(Long projectId) {
        try {
            // 直接从todoService获取项目的所有待办任务
            return todoService.getTodosByProject(projectId);
        } catch (Exception e) {
            log.warn("获取项目{}的待办任务失败: {}", projectId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 构建全局项目信息
     */
    private Map<String, Object> buildGlobalProjectInfo(List<Project> projects) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "全局项目甘特图");
        info.put("description", "当前用户相关的所有项目概览");
        info.put("totalProjects", projects.size());
        
        // 计算整体进度
        double avgProgress = projects.stream()
            .mapToInt(Project::getProgress)
            .average()
            .orElse(0.0);
        info.put("progress", (int) avgProgress);
        
        // 统计状态
        Map<String, Long> statusCount = projects.stream()
            .collect(Collectors.groupingBy(Project::getStatus, Collectors.counting()));
        info.put("statusCount", statusCount);
        
        return info;
    }
}