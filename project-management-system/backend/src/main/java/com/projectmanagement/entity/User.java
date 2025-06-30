package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private Integer status; // 0-禁用, 1-启用
    private String role; // 角色：ADMIN-管理员，USER-普通用户
    private String avatar; // 头像URL
    private String department; // 部门
    private String position; // 职位
    private Long superiorId; // 主管ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime; // 最后登录时间
} 