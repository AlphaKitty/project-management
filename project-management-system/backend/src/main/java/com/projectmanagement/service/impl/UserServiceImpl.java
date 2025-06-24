package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.projectmanagement.entity.User;
import com.projectmanagement.mapper.UserMapper;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<Map<String, Object>> getDashboardUsers() {
        // 使用性能优化版本
        return userMapper.selectDashboardUsersFast();
    }

    @Override
    public List<Map<String, Object>> getDashboardUsersFast() {
        return userMapper.selectDashboardUsersFast();
    }

    @Override
    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
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
    @Cacheable(value = "userSearch", key = "#keyword", unless = "#result.size() == 0", condition = "#keyword != null && #keyword.length() >= 2")
    public List<User> searchUsers(String keyword) {
        // 空关键字直接返回空列表，避免无意义的全表查询
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        String trimmedKeyword = keyword.trim();

        // 如果关键字长度小于2，返回空结果（避免性能问题）
        if (trimmedKeyword.length() < 2) {
            return new ArrayList<>();
        }

        // 策略1：精确匹配优先（使用索引，性能最好）
        List<User> exactMatches = findExactMatches(trimmedKeyword);
        if (!exactMatches.isEmpty()) {
            return exactMatches.size() > 50 ? exactMatches.subList(0, 50) : exactMatches;
        }

        // 策略2：前缀匹配（可以使用索引，性能较好）
        List<User> prefixMatches = findPrefixMatches(trimmedKeyword);
        if (!prefixMatches.isEmpty()) {
            return prefixMatches.size() > 50 ? prefixMatches.subList(0, 50) : prefixMatches;
        }

        // 策略3：模糊匹配（性能较差，但结果更全面）
        return findFuzzyMatches(trimmedKeyword);
    }

    /**
     * 精确匹配（使用索引，性能最好）
     */
    private List<User> findExactMatches(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);

        // 尝试ID匹配
        if (keyword.matches("\\d+")) {
            try {
                Long userId = Long.parseLong(keyword);
                wrapper.eq(User::getId, userId);
                List<User> userById = userMapper.selectList(wrapper);
                if (!userById.isEmpty()) {
                    return userById;
                }
            } catch (NumberFormatException ignored) {
                // 忽略数字格式错误
            }
        }

        // 精确用户名匹配
        wrapper.clear();
        wrapper.eq(User::getStatus, 1)
                .eq(User::getUsername, keyword);
        List<User> usersByUsername = userMapper.selectList(wrapper);
        if (!usersByUsername.isEmpty()) {
            return usersByUsername;
        }

        // 精确昵称匹配
        wrapper.clear();
        wrapper.eq(User::getStatus, 1)
                .eq(User::getNickname, keyword);
        return userMapper.selectList(wrapper);
    }

    /**
     * 前缀匹配（可以使用索引，性能较好）
     */
    private List<User> findPrefixMatches(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);

        // 用户名前缀匹配
        wrapper.and(w -> w.likeRight(User::getUsername, keyword)
                .or()
                .likeRight(User::getNickname, keyword));

        wrapper.orderByAsc(User::getId)
                .last("LIMIT 50");

        return userMapper.selectList(wrapper);
    }

    /**
     * 模糊匹配（性能较差，但结果更全面）
     */
    private List<User> findFuzzyMatches(String keyword) {
        // 对于模糊搜索，限制结果数量和添加更多约束
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);

        // 使用UNION ALL分别查询，避免复杂的OR条件
        // 1. 用户名包含关键字
        List<User> usernameMatches = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .like(User::getUsername, keyword)
                        .orderByAsc(User::getId)
                        .last("LIMIT 30"));

        // 2. 昵称包含关键字
        List<User> nicknameMatches = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .like(User::getNickname, keyword)
                        .orderByAsc(User::getId)
                        .last("LIMIT 30"));

        // 合并结果并去重
        Set<Long> userIds = new HashSet<>();
        List<User> allMatches = new ArrayList<>();

        // 添加用户名匹配结果
        for (User user : usernameMatches) {
            if (userIds.add(user.getId()) && allMatches.size() < 50) {
                allMatches.add(user);
            }
        }

        // 添加昵称匹配结果
        for (User user : nicknameMatches) {
            if (userIds.add(user.getId()) && allMatches.size() < 50) {
                allMatches.add(user);
            }
        }

        return allMatches;
    }
}