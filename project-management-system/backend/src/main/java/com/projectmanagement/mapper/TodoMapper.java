package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 待办任务Mapper接口
 */
@Mapper
public interface TodoMapper extends BaseMapper<Todo> {

    /**
     * 查询待办任务列表并关联项目和用户信息
     */
    List<Todo> selectTodosWithDetails();

    /**
     * 查询今日待办任务
     */
    List<Todo> selectTodayTodos();

    /**
     * 查询用户的今日待办任务
     */
    List<Todo> selectTodayTodosByUserId(@Param("userId") Long userId);

    /**
     * 查询本周待办任务
     */
    List<Todo> selectWeekTodos();

    /**
     * 查询高优先级待办任务
     */
    List<Todo> selectHighPriorityTodos();

    /**
     * 查询高优先级待办任务（按用户过滤）
     */
    List<Todo> selectHighPriorityTodosByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的本周待办任务
     */
    List<Todo> selectWeekTodosByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的待办任务
     */
    List<Todo> selectTodosByUserId(@Param("userId") Long userId);

    /**
     * 查询即将到期的任务（指定天数内到期）
     */
    List<Todo> selectTasksNearDeadline(@Param("daysAhead") int daysAhead);

    /**
     * 查询指定天数后到期的任务（正好是第N天到期）
     */
    List<Todo> selectTasksForSpecificDay(@Param("daysAfter") int daysAfter);

    /**
     * 查询已逾期的任务
     */
    List<Todo> selectOverdueTasks();
}