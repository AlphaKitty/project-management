package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目Mapper接口
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

        /**
         * 查询项目列表
         */
        @Select("SELECT * FROM projects ORDER BY create_time DESC")
        List<Project> selectProjectsWithCreator();

        /**
         * 查询用户作为创建人或责任人的项目列表
         */
        @Select("SELECT * FROM projects " +
                        "WHERE creator_id = #{userId} OR assignee_id = #{userId} " +
                        "ORDER BY create_time DESC")
        List<Project> selectProjectsByCreatorOrAssignee(@Param("userId") Long userId);

        /**
         * 根据项目ID查询项目详情
         */
        @Select("SELECT * FROM projects WHERE id = #{projectId}")
        Project selectProjectWithCreator(@Param("projectId") Long projectId);

        /**
         * 查询用户参与的项目列表
         */
        @Select("SELECT DISTINCT p.* FROM projects p " +
                        "INNER JOIN project_members pm ON p.id = pm.project_id " +
                        "WHERE pm.user_id = #{userId} " +
                        "ORDER BY p.create_time DESC")
        List<Project> selectProjectsByUserId(@Param("userId") Long userId);
}