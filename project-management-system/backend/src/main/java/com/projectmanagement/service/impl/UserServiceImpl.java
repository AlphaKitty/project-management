package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.projectmanagement.entity.User;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> getAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1); // 只查询启用状态的用户
        wrapper.orderByAsc(User::getId);
        return userMapper.selectList(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getStatus, 1); // 只查询启用状态的用户
        
        return userMapper.selectOne(wrapper);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1); // 只查询启用状态的用户
        
        if (StringUtils.hasText(keyword)) {
            // 有关键字时进行搜索
            wrapper.and(w -> w.like(User::getUsername, keyword)
                            .or()
                            .like(User::getNickname, keyword)
                            .or()
                            .eq(User::getId, keyword.matches("\\d+") ? Long.parseLong(keyword) : -1));
        }
        
        wrapper.orderByAsc(User::getId)
               .last("LIMIT 50"); // 限制返回数量，提高性能
        
        return userMapper.selectList(wrapper);
    }
} 