package com.projectmanagement.service;

import com.projectmanagement.entity.User;
import java.util.List;

public interface UserService {
    /**
     * 获取所有用户列表
     */
    List<User> getAllUsers();
    
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