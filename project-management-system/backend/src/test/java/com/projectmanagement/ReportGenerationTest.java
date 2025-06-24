package com.projectmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

/**
 * 报告生成测试类 - 演示节点状态功能
 */
@SpringBootTest
public class ReportGenerationTest {

    @Test
    public void testNodeStatusGeneration() {
        // 模拟项目里程碑数据
        String milestonesJson = "[" +
                "{\"name\":\"需求分析\",\"status\":\"COMPLETED\",\"dueDate\":\"2024-01-15\",\"description\":\"完成需求分析\"}," +
                "{\"name\":\"系统设计\",\"status\":\"PROGRESS\",\"dueDate\":\"2024-02-15\",\"description\":\"正在进行系统设计\"}," +
                "{\"name\":\"开发实现\",\"status\":\"TODO\",\"dueDate\":\"2024-03-15\",\"description\":\"待开发\"}" +
                "]";

        System.out.println("=== 节点状态功能演示 ===");
        System.out.println("里程碑数据：" + milestonesJson);

        // 演示在模糊模式下会生成的状态信息
        System.out.println("\n生成的节点状态示例：");
        System.out.println("【节点状态】当前里程碑系统设计，当前里程碑进度65%，共识别8个待办，进行中3个，已完成2个");

        System.out.println("\n功能说明：");
        System.out.println("1. 自动识别当前里程碑（第一个非COMPLETED状态）");
        System.out.println("2. 基于时间计算里程碑进度（当前时间相对于里程碑时间段的位置）");
        System.out.println("3. 统计项目相关的所有待办任务数量");
        System.out.println("4. 只在模糊模式下显示，为报告提供概览信息");
    }

    @Test
    public void testMilestoneProgressCalculation() {
        // 演示进度计算逻辑
        LocalDate prevMilestoneDate = LocalDate.of(2024, 1, 15); // 上个里程碑结束时间
        LocalDate currentMilestoneDate = LocalDate.of(2024, 2, 15); // 当前里程碑结束时间
        LocalDate today = LocalDate.now();

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(prevMilestoneDate, currentMilestoneDate);
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(prevMilestoneDate, today);
        double progress = Math.max(0, Math.min(100, (double) elapsedDays / totalDays * 100));

        System.out.println("=== 进度计算演示 ===");
        System.out.println("上个里程碑结束：" + prevMilestoneDate);
        System.out.println("当前里程碑结束：" + currentMilestoneDate);
        System.out.println("今天日期：" + today);
        System.out.println("总天数：" + totalDays + "天");
        System.out.println("已过天数：" + elapsedDays + "天");
        System.out.println("计算进度：" + String.format("%.0f", progress) + "%");
    }
}