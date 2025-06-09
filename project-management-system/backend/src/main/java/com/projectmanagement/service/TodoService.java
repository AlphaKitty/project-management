package com.projectmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.projectmanagement.dto.TodoDTO;
import com.projectmanagement.entity.Todo;

import java.util.List;

/**
 * 待办任务服务接口
 */
public interface TodoService extends IService<Todo> {

    /**
     * 获取待办任务列表
     */
    List<Todo> getTodoList();

    /**
     * 获取今日待办
     */
    List<Todo> getTodayTodos();

    /**
     * 获取今日待办（按用户过滤）
     */
    List<Todo> getTodayTodos(Long userId);

    /**
     * 获取本周待办
     */
    List<Todo> getWeekTodos();

    /**
     * 获取高优先级待办
     */
    List<Todo> getHighPriorityTodos();

    /**
     * 获取高优先级待办（按用户过滤）
     */
    List<Todo> getHighPriorityTodos(Long userId);

    /**
     * 获取本周待办（按用户过滤）
     */
    List<Todo> getWeekTodos(Long userId);

    /**
     * 获取用户待办任务
     */
    List<Todo> getUserTodos(Long userId);

    /**
     * 获取待办任务详情
     */
    Todo getTodoDetail(Long todoId);

    /**
     * 创建待办任务
     */
    Todo createTodo(TodoDTO todoDTO);

    /**
     * 更新待办任务
     */
    Todo updateTodo(Long todoId, TodoDTO todoDTO);

    /**
     * 删除待办任务
     */
    boolean deleteTodo(Long todoId);

    /**
     * 更新任务状态
     */
    boolean updateTodoStatus(Long todoId, String status);

    /**
     * 发送待办邮件
     */
    boolean sendTodoEmail(String email, Long userId);

    /**
     * 获取即将到期的任务
     */
    List<Todo> getTasksNearDeadline(int daysAhead);

    /**
     * 获取已逾期的任务
     */
    List<Todo> getOverdueTasks();
}