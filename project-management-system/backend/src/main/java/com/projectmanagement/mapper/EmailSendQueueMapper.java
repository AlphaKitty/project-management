package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.EmailSendQueue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送队列Mapper接口
 */
@Mapper
public interface EmailSendQueueMapper extends BaseMapper<EmailSendQueue> {
} 