package com.projectmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectmanagement.entity.EmailTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 邮件模板Mapper接口
 */
@Mapper
public interface EmailTemplateMapper extends BaseMapper<EmailTemplate> {

    /**
     * 根据模板代码查询启用的模板
     */
    @Select("SELECT * FROM email_templates WHERE template_code = #{templateCode} AND enabled = 1")
    EmailTemplate selectByTemplateCode(@Param("templateCode") String templateCode);
} 