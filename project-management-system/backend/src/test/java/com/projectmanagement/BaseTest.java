package com.projectmanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 测试基类
 * 提供通用的测试配置和工具方法
 */
@SpringBootTest
@SpringJUnitConfig
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    @BeforeEach
    void baseSetUp() {
        // 通用的测试初始化逻辑
    }

    /**
     * 创建测试用户数据
     */
    protected com.projectmanagement.entity.User createTestUser() {
        com.projectmanagement.entity.User user = new com.projectmanagement.entity.User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setNickname("测试用户");
        user.setPhone("13800138000");
        user.setStatus(1);
        return user;
    }

    /**
     * 创建测试项目数据
     */
    protected com.projectmanagement.entity.Project createTestProject() {
        com.projectmanagement.entity.Project project = new com.projectmanagement.entity.Project();
        project.setId(1L);
        project.setName("测试项目");
        project.setDescription("这是一个测试项目");
        project.setStatus("PROGRESS");
        project.setProgress(50);
        project.setCreatorId(1L);
        return project;
    }

    /**
     * 创建测试待办任务数据
     */
    protected com.projectmanagement.entity.Todo createTestTodo() {
        com.projectmanagement.entity.Todo todo = new com.projectmanagement.entity.Todo();
        todo.setId(1L);
        todo.setTitle("测试任务");
        todo.setDescription("这是一个测试任务");
        todo.setStatus("TODO");
        todo.setPriority("MEDIUM");
        todo.setProjectId(1L);
        todo.setAssigneeId(1L);
        todo.setEmailEnabled(true);
        return todo;
    }
}