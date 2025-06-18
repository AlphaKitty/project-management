package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.ProjectDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.entity.User;
import com.projectmanagement.mapper.ProjectMapper;
import com.projectmanagement.mapper.TodoMapper;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // 获取所有项目
        List<Project> projects = projectMapper.selectList(null);

        // 计算本周和下周的日期范围
        LocalDate today = LocalDate.now();
        LocalDate startOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfThisWeek = startOfThisWeek.plusDays(6);
        LocalDate startOfNextWeek = endOfThisWeek.plusDays(1);
        LocalDate endOfNextWeek = startOfNextWeek.plusDays(6);

        for (Project project : projects) {
            // 查询项目相关的待办任务
            QueryWrapper<Todo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", project.getId());
            List<Todo> todos = todoMapper.selectList(queryWrapper);

            // 生成本周工作内容
            String thisWeekWork = generateThisWeekWork(todos, startOfThisWeek, endOfThisWeek);
            String milestoneThisWeekWork = generateMilestoneBasedThisWeekWork(project, startOfThisWeek, endOfThisWeek);

            // 合并任务和项目整体状态
            thisWeekWork = combineWorkContent(thisWeekWork, milestoneThisWeekWork);

            // 生成下周计划内容
            String nextWeekPlan = generateNextWeekPlan(todos, startOfNextWeek, endOfNextWeek);
            String milestoneNextWeekPlan = generateMilestoneBasedNextWeekPlan(project, startOfNextWeek, endOfNextWeek);

            // 合并任务和项目整体计划
            nextWeekPlan = combineWorkContent(nextWeekPlan, milestoneNextWeekPlan);

            // 更新项目
            project.setThisWeekWork(thisWeekWork);
            project.setNextWeekPlan(nextWeekPlan);
            projectMapper.updateById(project);
        }
    }

    /**
     * 生成本周工作内容
     */
    private String generateThisWeekWork(List<Todo> todos, LocalDate startDate, LocalDate endDate) {
        StringBuilder content = new StringBuilder();

        // 本周已完成的任务
        List<Todo> completedTodos = todos.stream()
                .filter(todo -> "DONE".equals(todo.getStatus()) &&
                        todo.getCompletedTime() != null &&
                        !todo.getCompletedTime().toLocalDate().isBefore(startDate) &&
                        !todo.getCompletedTime().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());

        // 本周进行中的任务
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
                if (todo.getDueDate() != null) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), todo.getDueDate());
                    if (daysLeft > 0) {
                        content.append("（剩余").append(daysLeft).append("天）");
                    } else if (daysLeft == 0) {
                        content.append("（今日截止）");
                    } else {
                        content.append("（已逾期").append(Math.abs(daysLeft)).append("天）");
                    }
                }
                content.append("\n");
            }
        }

        return content.length() > 0 ? content.toString().trim() : null;
    }

    /**
     * 生成下周计划内容
     */
    private String generateNextWeekPlan(List<Todo> todos, LocalDate startDate, LocalDate endDate) {
        StringBuilder content = new StringBuilder();

        // 下周计划的任务（未完成的任务）
        List<Todo> plannedTodos = todos.stream()
                .filter(todo -> !"DONE".equals(todo.getStatus()))
                .collect(Collectors.toList());

        // 下周截止的任务
        List<Todo> nextWeekDueTodos = plannedTodos.stream()
                .filter(todo -> todo.getDueDate() != null &&
                        !todo.getDueDate().isBefore(startDate) &&
                        !todo.getDueDate().isAfter(endDate))
                .collect(Collectors.toList());

        if (!nextWeekDueTodos.isEmpty()) {
            content.append("下周截止任务：\n");
            for (Todo todo : nextWeekDueTodos) {
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
            content.append("下周计划\n");
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
     * 基于里程碑生成本周工作内容
     */
    private String generateMilestoneBasedThisWeekWork(Project project, LocalDate startDate, LocalDate endDate) {
        try {
            if (project.getMilestones() == null || project.getMilestones().trim().isEmpty()) {
                return "项目正常推进中，按计划执行各项工作";
            }

            List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            StringBuilder content = new StringBuilder();
            LocalDate today = LocalDate.now();

            // 查找本周相关的里程碑
            for (Map<String, Object> milestone : milestones) {
                String name = (String) milestone.get("name");
                String status = (String) milestone.get("status");
                String dueDateStr = (String) milestone.get("dueDate");

                if (name == null || name.trim().isEmpty())
                    continue;

                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);

                    // 本周到期的里程碑
                    if (!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
                        if ("COMPLETED".equals(status)) {
                            content.append("已完成里程碑：").append(name).append("\n");
                        } else {
                            content.append("推进里程碑：").append(name).append("（本周截止）\n");
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
     * 基于里程碑生成下周计划内容
     */
    private String generateMilestoneBasedNextWeekPlan(Project project, LocalDate startDate, LocalDate endDate) {
        try {
            if (project.getMilestones() == null || project.getMilestones().trim().isEmpty()) {
                return "继续推进项目各项工作，确保按时完成既定目标";
            }

            List<Map<String, Object>> milestones = objectMapper.readValue(project.getMilestones(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            StringBuilder content = new StringBuilder();
            LocalDate today = LocalDate.now();

            // 查找下周相关的里程碑
            for (Map<String, Object> milestone : milestones) {
                String name = (String) milestone.get("name");
                String status = (String) milestone.get("status");
                String dueDateStr = (String) milestone.get("dueDate");

                if (name == null || name.trim().isEmpty())
                    continue;

                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);

                    // 下周到期的里程碑
                    if (!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
                        if (!"COMPLETED".equals(status)) {
                            content.append("完成里程碑：").append(name).append("（下周截止）\n");
                        }
                    }
                    // 即将开始的里程碑
                    else if ("PENDING".equals(status) && dueDate.isAfter(endDate)) {
                        long daysUntilStart = ChronoUnit.DAYS.between(today, dueDate);
                        if (daysUntilStart <= 14) { // 两周内的里程碑
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
                content.append("，已到期，需加快推进");
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
}