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
    private Integer status; // 0-禁用, 1-启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 