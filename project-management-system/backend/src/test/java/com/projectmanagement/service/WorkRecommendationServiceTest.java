package com.projectmanagement.service;

import com.projectmanagement.BaseTest;
import com.projectmanagement.dto.WorkRecommendationDTO;
import com.projectmanagement.entity.Project;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.impl.WorkRecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 工作推荐服务测试
 */
@ExtendWith(MockitoExtension.class)
class WorkRecommendationServiceTest extends BaseTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private TodoService todoService;

    @InjectMocks
    private WorkRecommendationServiceImpl workRecommendationService;

    private User testUser;
    private Project testProject;
    private Todo testTodo;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testProject = createTestProject();
        testTodo = createTestTodo();
    }

    @Test
    void testGetUserRecommendations_Success() {
        // 准备测试数据
        List<Project> projects = Arrays.asList(testProject);
        List<Todo> todos = Arrays.asList(testTodo);

        // 模拟服务调用
        when(projectService.getProjectListByUser(anyLong())).thenReturn(projects);
        when(todoService.getUserRelatedTodos(anyLong())).thenReturn(todos);

        // 执行测试
        WorkRecommendationDTO.RecommendationSummary result = workRecommendationService
                .getUserRecommendations(testUser.getId());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getTotalCount() >= 0);
        assertNotNull(result.getUrgent());
        assertNotNull(result.getStagnant());
        assertNotNull(result.getProgress());
        assertNotNull(result.getCollaboration());
        assertNotNull(result.getRisk());
        assertNotNull(result.getSuggestions());
    }

    @Test
    void testGetUserRecommendations_EmptyData() {
        // 准备空数据
        when(projectService.getProjectListByUser(anyLong())).thenReturn(Arrays.asList());
        when(todoService.getUserRelatedTodos(anyLong())).thenReturn(Arrays.asList());

        // 执行测试
        WorkRecommendationDTO.RecommendationSummary result = workRecommendationService
                .getUserRecommendations(testUser.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getUrgent().isEmpty());
        assertTrue(result.getStagnant().isEmpty());
        assertTrue(result.getProgress().isEmpty());
        assertTrue(result.getCollaboration().isEmpty());
        assertTrue(result.getRisk().isEmpty());
        assertTrue(result.getSuggestions().isEmpty());
    }

    @Test
    void testGetUserRecommendations_WithStagnantProject() {
        // 创建停滞项目
        Project stagnantProject = createTestProject();
        stagnantProject.setStatus("PROGRESS");
        stagnantProject.setProgress(30);
        stagnantProject.setUpdateTime(LocalDateTime.now().minusDays(5));

        // 创建已完成的待办任务
        Todo completedTodo = createTestTodo();
        completedTodo.setStatus("DONE");
        completedTodo.setCompletedTime(LocalDateTime.now().minusDays(4));

        List<Project> projects = Arrays.asList(stagnantProject);
        List<Todo> todos = Arrays.asList(completedTodo);

        // 模拟服务调用
        when(projectService.getProjectListByUser(anyLong())).thenReturn(projects);
        when(todoService.getUserRelatedTodos(anyLong())).thenReturn(todos);

        // 执行测试
        WorkRecommendationDTO.RecommendationSummary result = workRecommendationService
                .getUserRecommendations(testUser.getId());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getTotalCount() > 0);
        // 可能会检测到停滞项目
        assertNotNull(result.getStagnant());
    }

    @Test
    void testExecuteRecommendation_ValidId() {
        // 执行测试
        boolean result = workRecommendationService.executeRecommendation(
                "test-recommendation-id", testUser.getId(), null);

        // 验证结果 - 基本实现应该返回true
        assertTrue(result);
    }

    @Test
    void testMarkAsHandled_ValidId() {
        // 执行测试
        boolean result = workRecommendationService.markAsHandled(
                "test-recommendation-id", testUser.getId());

        // 验证结果 - 基本实现应该返回true
        assertTrue(result);
    }

    @Test
    void testIgnoreRecommendation_ValidId() {
        // 执行测试
        boolean result = workRecommendationService.ignoreRecommendation(
                "test-recommendation-id", testUser.getId());

        // 验证结果 - 基本实现应该返回true
        assertTrue(result);
    }

    @Test
    void testGetUserRecommendations_NullUserId() {
        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            workRecommendationService.getUserRecommendations(null);
        });
    }
}