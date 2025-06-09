package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 报告Mapper接口
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 查询报告列表并关联项目和创建人信息
     */
    @Select("SELECT r.*, p.name as project_name, " +
            "u.username as creator_name, u.nickname as creator_nickname " +
            "FROM reports r " +
            "LEFT JOIN projects p ON r.project_id = p.id " +
            "LEFT JOIN users u ON r.creator_id = u.id " +
            "ORDER BY r.create_time DESC")
    List<Report> selectReportsWithDetails();

    /**
     * 根据项目ID查询报告列表
     */
    @Select("SELECT r.*, p.name as project_name, " +
            "u.username as creator_name, u.nickname as creator_nickname " +
            "FROM reports r " +
            "LEFT JOIN projects p ON r.project_id = p.id " +
            "LEFT JOIN users u ON r.creator_id = u.id " +
            "WHERE r.project_id = #{projectId} " +
            "ORDER BY r.create_time DESC")
    List<Report> selectReportsByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据报告类型查询报告列表
     */
    @Select("SELECT r.*, p.name as project_name, " +
            "u.username as creator_name, u.nickname as creator_nickname " +
            "FROM reports r " +
            "LEFT JOIN projects p ON r.project_id = p.id " +
            "LEFT JOIN users u ON r.creator_id = u.id " +
            "WHERE r.type = #{type} " +
            "ORDER BY r.create_time DESC")
    List<Report> selectReportsByType(@Param("type") String type);

    /**
     * 根据创建人查询报告列表
     */
    @Select("SELECT r.*, p.name as project_name " +
            "FROM reports r " +
            "LEFT JOIN projects p ON r.project_id = p.id " +
            "WHERE r.creator_id = #{creatorId} " +
            "ORDER BY r.create_time DESC")
    List<Report> selectReportsByCreatorId(@Param("creatorId") Long creatorId);
} 