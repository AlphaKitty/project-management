-- 为todos表添加邮件通知开关字段
-- 执行日期：2025-01-16
ALTER TABLE todos
ADD COLUMN email_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用邮件通知，默认为true';
-- 为现有数据设置默认值
UPDATE todos
SET email_enabled = TRUE
WHERE email_enabled IS NULL;