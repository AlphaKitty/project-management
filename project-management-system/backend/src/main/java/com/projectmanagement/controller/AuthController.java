package com.projectmanagement.controller;

import com.projectmanagement.common.ApiResponse;
import com.projectmanagement.dto.LoginDTO;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginDTO loginDTO, HttpSession session) {
        log.info("用户登录请求: {}", loginDTO.getUsername());
        
        try {
            // 验证密码是否为0000
            if (!"0000".equals(loginDTO.getPassword())) {
                return ApiResponse.error("密码错误");
            }
            
            // 根据用户名查找用户
            User user = userService.getUserByUsername(loginDTO.getUsername());
            if (user == null) {
                return ApiResponse.error("用户不存在");
            }
            
            // 检查用户状态
            if (user.getStatus() != 1) {
                return ApiResponse.error("用户已被禁用");
            }
            
            // 生成简单的令牌（这里简化处理，实际项目建议使用JWT）
            String token = "token_" + user.getId() + "_" + System.currentTimeMillis();
            
            // 保存用户信息到session
            session.setAttribute("currentUser", user);
            session.setAttribute("token", token);
            
            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", user);
            result.put("token", token);
            result.put("isAdmin", "barlin.zhang".equals(user.getUsername()));
            
            log.info("用户 {} 登录成功", user.getUsername());
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("登录失败", e);
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                log.info("用户 {} 退出登录", currentUser.getUsername());
            }
            
            // 清除session
            session.invalidate();
            
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("登出失败", e);
            return ApiResponse.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public ApiResponse<User> getCurrentUser(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ApiResponse.error("用户未登录");
        }
        return ApiResponse.success(currentUser);
    }
} 