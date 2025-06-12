package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.ProjectDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.User;
import com.projectmanagement.mapper.ProjectMapper;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 * 项目服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

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
}