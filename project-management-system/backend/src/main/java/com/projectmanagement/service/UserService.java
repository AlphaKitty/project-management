package com.projectmanagement.service;

import com.projectmanagement.entity.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * 获取所有用户列表
     */
    List<User> getAllUsers();

    /**
     * 获取数据看板用户数据（性能优化版）
     * 只返回有任务的活跃用户和必要字段
     */
    List<Map<String, Object>> getDashboardUsers();

    /**
     * 获取数据看板用户数据（极速版）
     * 性能更好的实现方式
     */
    List<Map<String, Object>> getDashboardUsersFast();

    /**
     * 根据ID获取用户
     */
    User getUserById(Long id);

    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);

    /**
     * 根据关键字搜索用户（支持用户名、昵称模糊搜索）
     */
    List<User> searchUsers(String keyword);
}