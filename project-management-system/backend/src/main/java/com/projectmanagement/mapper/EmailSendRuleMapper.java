package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.EmailSendRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送规则Mapper接口
 */
@Mapper
public interface EmailSendRuleMapper extends BaseMapper<EmailSendRule> {
} 