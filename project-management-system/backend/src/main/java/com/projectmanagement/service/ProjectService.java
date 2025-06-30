package com.projectmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.projectmanagement.dto.ProjectDTO;
import com.projectmanagement.entity.Project;

import java.util.List;
import java.util.Map;

/**
 * 项目服务接口
 */
public interface ProjectService extends IService<Project> {

    /**
     * 获取项目列表
     */
    List<Project> getProjectList();

    /**
     * 根据用户ID获取项目列表（用户作为创建人或责任人的项目）
     */
    List<Project> getProjectListByUser(Long userId);

    /**
     * 获取项目详情
     */
    Project getProjectDetail(Long projectId);

    /**
     * 创建项目
     */
    Project createProject(ProjectDTO projectDTO);

    /**
     * 更新项目
     */
    Project updateProject(Long projectId, ProjectDTO projectDTO);

    /**
     * 删除项目
     */
    boolean deleteProject(Long projectId);

    /**
     * 获取用户参与的项目列表
     */
    List<Project> getUserProjects(Long userId);

    /**
     * 添加项目成员
     */
    boolean addProjectMember(Long projectId, Long userId);

    /**
     * 移除项目成员
     */
    boolean removeProjectMember(Long projectId, Long userId);

    /**
     * 更新项目进度
     */
    boolean updateProjectProgress(Long projectId, Integer progress);

    /**
     * 获取项目概览（按创建时间排序）
     */
    List<Project> getProjectOverview();

    /**
     * 根据用户ID获取项目概览（用户相关的项目）
     */
    List<Project> getProjectOverviewByUser(Long userId);

    /**
     * 更新所有项目的工作计划
     */
    void updateAllProjectWorkPlans();

    /**
     * 更新所有项目的工作计划（指定天数）
     */
    void updateAllProjectWorkPlans(Integer days);

    /**
     * 获取项目时间轴
     */

    
    /**
     * 获取项目甘特图数据
     */
    Map<String, Object> getProjectGanttData(Long projectId);
    
    /**
     * 获取用户相关的所有项目甘特图数据
     */
    Map<String, Object> getUserProjectsGanttData(Long userId);
}