package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

        /**
         * 根据项目ID查询项目成员列表
         */
        @Select("SELECT u.* FROM users u " +
                        "INNER JOIN project_members pm ON u.id = pm.user_id " +
                        "WHERE pm.project_id = #{projectId} " +
                        "ORDER BY pm.role DESC, u.username ASC")
        List<User> selectMembersByProjectId(@Param("projectId") Long projectId);

        /**
         * 根据用户名查询用户
         */
        @Select("SELECT * FROM users WHERE username = #{username} AND status = 1")
        User selectByUsername(@Param("username") String username);

        /**
         * 查询所有正常状态的用户
         */
        @Select("SELECT * FROM users WHERE status = 1 ORDER BY username ASC")
        List<User> selectActiveUsers();

        /**
         * 查询数据看板用户数据（性能优化版）
         * 只返回有任务分配的用户，且只返回必要字段
         */
        @Select("SELECT DISTINCT " +
                        "u.id, " +
                        "u.username, " +
                        "u.nickname, " +
                        "u.department, " +
                        "u.position, " +
                        "u.status " +
                        "FROM users u " +
                        "WHERE u.status = 1 " +
                        "AND u.id IN (" +
                        "  SELECT DISTINCT user_id FROM (" +
                        "    SELECT assignee_id as user_id FROM todos WHERE assignee_id IS NOT NULL " +
                        "    UNION " +
                        "    SELECT assignee_id as user_id FROM projects WHERE assignee_id IS NOT NULL " +
                        "    UNION " +
                        "    SELECT creator_id as user_id FROM projects WHERE creator_id IS NOT NULL " +
                        "  ) related_users " +
                        ") " +
                        "ORDER BY u.username ASC")
        List<Map<String, Object>> selectDashboardUsers();

        /**
         * 查询数据看板用户数据（极速版 - 使用JOIN）
         * 性能更好的实现方式
         */
        @Select("SELECT DISTINCT " +
                        "u.id, " +
                        "u.username, " +
                        "u.nickname, " +
                        "u.department, " +
                        "u.position, " +
                        "u.status " +
                        "FROM users u " +
                        "WHERE u.status = 1 " +
                        "AND (" +
                        "  u.id IN (SELECT DISTINCT assignee_id FROM todos WHERE assignee_id IS NOT NULL) " +
                        "  OR u.id IN (SELECT DISTINCT assignee_id FROM projects WHERE assignee_id IS NOT NULL) " +
                        "  OR u.id IN (SELECT DISTINCT creator_id FROM projects WHERE creator_id IS NOT NULL) " +
                        ") " +
                        "ORDER BY u.username ASC")
        List<Map<String, Object>> selectDashboardUsersFast();
}