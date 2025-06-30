package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

        /**
         * 分页查询操作日志
         */
        IPage<OperationLog> selectPageLogs(Page<OperationLog> page,
                        @Param("username") String username,
                        @Param("module") String module,
                        @Param("operationType") String operationType,
                        @Param("success") Boolean success,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 按时间范围查询操作日志
         */
        List<OperationLog> selectLogsByTimeRange(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 查询用户的操作日志
         */
        List<OperationLog> selectLogsByUserId(@Param("userId") Long userId,
                        @Param("limit") Integer limit);

        /**
         * 统计操作类型分布
         */
        List<Object> selectOperationTypeStats(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 统计模块操作分布
         */
        List<Object> selectModuleStats(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 删除指定时间之前的日志（用于日志清理）
         */
        int deleteLogsBefore(@Param("beforeTime") LocalDateTime beforeTime);

        /**
         * 获取项目相关操作日志
         */
        List<OperationLog> selectProjectRelatedLogs(@Param("projectId") Long projectId, @Param("limit") Integer limit);
}