-- ========================================
-- 数据看板用户字段更新脚本
-- 用于补充User实体缺失的字段和真实部门数据
-- ========================================

-- 检查并添加缺失的用户表字段（如果不存在）
SET @sql = '';
SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN password VARCHAR(255) DEFAULT NULL COMMENT "密码（加密后）" AFTER phone;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT "USER" COMMENT "角色：ADMIN-管理员，USER-普通用户" AFTER status;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'avatar';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN avatar VARCHAR(500) DEFAULT NULL COMMENT "头像URL" AFTER role;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'department';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN department VARCHAR(100) DEFAULT NULL COMMENT "部门" AFTER avatar;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'position';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN position VARCHAR(100) DEFAULT NULL COMMENT "职位" AFTER department;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'superior_id';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN superior_id BIGINT(20) DEFAULT NULL COMMENT "主管ID" AFTER position;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @count FROM information_schema.columns 
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'last_login_time';
SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT "最后登录时间" AFTER update_time;', '');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 更新现有用户的部门和职位信息（如果字段为空）
UPDATE users SET 
    department = CASE username
        WHEN 'admin' THEN 'IT部门'
        WHEN 'barlin.zhang' THEN 'IT部门'
        WHEN 'zhang.san' THEN '开发部'
        WHEN 'li.si' THEN '产品部'
        WHEN 'wang.wu' THEN '设计部'
        ELSE '未分配'
    END,
    position = CASE username
        WHEN 'admin' THEN '系统管理员'
        WHEN 'barlin.zhang' THEN '技术总监'
        WHEN 'zhang.san' THEN '高级开发工程师'
        WHEN 'li.si' THEN '产品经理'
        WHEN 'wang.wu' THEN 'UI设计师'
        ELSE '员工'
    END,
    role = CASE username
        WHEN 'admin' THEN 'ADMIN'
        WHEN 'barlin.zhang' THEN 'ADMIN'
        ELSE 'USER'
    END
WHERE department IS NULL OR department = '' OR position IS NULL OR position = '';

-- 添加更多测试用户，丰富部门数据（仅在用户不存在时插入）
INSERT IGNORE INTO users (
    username, nickname, email, password, role, status, department, position
) VALUES
    ('zhao.liu', '赵六', 'zhao.liu@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '开发部', '前端开发工程师'),
    ('chen.qi', '陈七', 'chen.qi@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '测试部', '测试工程师'),
    ('liu.ba', '刘八', 'liu.ba@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '运营部', '运营专员'),
    ('yang.jiu', '杨九', 'yang.jiu@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '市场部', '市场推广专员'),
    ('huang.shi', '黄十', 'huang.shi@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '财务部', '财务分析师'),
    ('wu.shiyi', '吴十一', 'wu.shiyi@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '人事部', 'HR专员'),
    ('zheng.shier', '郑十二', 'zheng.shier@company.com', '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC', 'USER', 1, '开发部', '后端开发工程师');

-- 创建相关索引（如果不存在）
SET @sql = 'CREATE INDEX IF NOT EXISTS idx_users_department ON users (department)';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = 'CREATE INDEX IF NOT EXISTS idx_users_position ON users (position)';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = 'CREATE INDEX IF NOT EXISTS idx_users_role ON users (role)';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 显示更新结果
SELECT '用户部门数据更新完成!' AS message;
SELECT department, COUNT(*) as user_count 
FROM users 
WHERE status = 1 
GROUP BY department 
ORDER BY user_count DESC; 