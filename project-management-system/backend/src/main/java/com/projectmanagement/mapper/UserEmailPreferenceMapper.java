package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.UserEmailPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户邮件偏好设置Mapper接口
 */
@Mapper
public interface UserEmailPreferenceMapper extends BaseMapper<UserEmailPreference> {
} 