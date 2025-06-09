package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/search")
    public Result<List<User>> searchUsers(@RequestParam(required = false) String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return Result.success(users);
    }
} 