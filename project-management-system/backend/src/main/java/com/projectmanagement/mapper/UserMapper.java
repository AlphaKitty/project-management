package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
} 