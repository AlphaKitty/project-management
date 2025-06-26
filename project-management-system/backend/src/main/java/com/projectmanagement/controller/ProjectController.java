package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.common.ResultCode;
import com.projectmanagement.dto.ProjectDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.User;
import com.projectmanagement.annotation.OperationLog;
import com.projectmanagement.enums.BusinessModule;
import com.projectmanagement.enums.OperationType;
import com.projectmanagement.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.PROJECT, description = "查询项目列表")
    public Result<List<Project>> getProjectList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        // 只返回当前用户作为创建人或责任人的项目
        List<Project> projects = projectService.getProjectListByUser(currentUser.getId());
        return Result.success(projects);
    }

    @GetMapping("/{id}")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.PROJECT, description = "查询项目详情")
    public Result<Project> getProjectDetail(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        Project project = projectService.getProjectDetail(id);
        if (project == null) {
            return Result.error("项目不存在");
        }

        // 检查权限：只有创建人或责任人可以查看
        if (!project.getCreatorId().equals(currentUser.getId()) &&
                (project.getAssigneeId() == null || !project.getAssigneeId().equals(currentUser.getId()))) {
            return Result.error("无权限访问该项目");
        }

        return Result.success(project);
    }

    @PostMapping
    @OperationLog(type = OperationType.CREATE, module = BusinessModule.PROJECT, description = "创建项目")
    public Result<Project> createProject(@Validated @RequestBody ProjectDTO projectDTO, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        // 设置创建人为当前用户
        projectDTO.setCreatorId(currentUser.getId());
        Project project = projectService.createProject(projectDTO);
        return Result.success("项目创建成功", project);
    }

    @PutMapping("/{id}")
    @OperationLog(type = OperationType.UPDATE, module = BusinessModule.PROJECT, description = "更新项目")
    public Result<Project> updateProject(@PathVariable Long id, @Validated @RequestBody ProjectDTO projectDTO,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        // 检查权限：只有创建人可以编辑项目
        Project existingProject = projectService.getProjectDetail(id);
        if (existingProject == null) {
            return Result.error("项目不存在");
        }

        if (!existingProject.getCreatorId().equals(currentUser.getId())) {
            return Result.error("只有项目创建人可以编辑项目");
        }

        Project project = projectService.updateProject(id, projectDTO);
        return Result.success("项目更新成功", project);
    }

    @DeleteMapping("/{id}")
    @OperationLog(type = OperationType.DELETE, module = BusinessModule.PROJECT, description = "删除项目")
    public Result<String> deleteProject(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        // 检查权限：只有创建人可以删除项目
        Project existingProject = projectService.getProjectDetail(id);
        if (existingProject == null) {
            return Result.error("项目不存在");
        }

        if (!existingProject.getCreatorId().equals(currentUser.getId())) {
            return Result.error("只有项目创建人可以删除项目");
        }

        boolean success = projectService.deleteProject(id);
        return success ? Result.success("项目删除成功") : Result.error("项目删除失败");
    }

    @PutMapping("/{id}/progress")
    @OperationLog(type = OperationType.UPDATE, module = BusinessModule.PROJECT, description = "更新项目进度")
    public Result<String> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        boolean success = projectService.updateProjectProgress(id, progress);
        return success ? Result.success("进度更新成功") : Result.error("进度更新失败");
    }

    @PostMapping("/{id}/members/{userId}")
    @OperationLog(type = OperationType.CREATE, module = BusinessModule.PROJECT, description = "添加项目成员")
    public Result<String> addMember(@PathVariable Long id, @PathVariable Long userId) {
        boolean success = projectService.addProjectMember(id, userId);
        return success ? Result.success("成员添加成功") : Result.error("成员添加失败");
    }

    @DeleteMapping("/{id}/members/{userId}")
    @OperationLog(type = OperationType.DELETE, module = BusinessModule.PROJECT, description = "移除项目成员")
    public Result<String> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        boolean success = projectService.removeProjectMember(id, userId);
        return success ? Result.success("成员移除成功") : Result.error("成员移除失败");
    }

    /**
     * 获取项目概览（按创建时间排序）
     */
    @GetMapping("/overview")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.PROJECT, description = "查询项目概览")
    public Result<List<Project>> getProjectOverview(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        // 只返回当前用户相关的项目概览
        List<Project> projects = projectService.getProjectOverviewByUser(currentUser.getId());
        return Result.success(projects);
    }

    /**
     * 更新所有项目的工作计划
     */
    @PostMapping("/update-work-plans")
    @OperationLog(type = OperationType.UPDATE, module = BusinessModule.PROJECT, description = "更新项目工作计划")
    public Result<String> updateWorkPlans(@RequestParam(defaultValue = "7") Integer days, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.unauthorized();
        }

        try {
            projectService.updateAllProjectWorkPlans(days);
            return Result.success("工作计划更新成功");
        } catch (Exception e) {
            return Result.error("工作计划更新失败: " + e.getMessage());
        }
    }
}