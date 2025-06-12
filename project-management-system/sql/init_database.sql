-- ========================================
-- 项目管理系统数据库初始化脚本
-- 支持重复执行，每次执行都会全量更新
-- ========================================
-- 设置字符集和时区
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET TIME_ZONE = '+08:00';
-- ========================================
-- 1. 用户表
-- ========================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `superior_id` BIGINT(20) COMMENT '主管ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(100) COMMENT '昵称',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员，USER-普通用户',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `department` VARCHAR(100) COMMENT '部门',
    `position` VARCHAR(100) COMMENT '职位',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';
-- ========================================
-- 2. 项目表
-- ========================================
DROP TABLE IF EXISTS `projects`;
CREATE TABLE `projects` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name` VARCHAR(200) NOT NULL COMMENT '项目名称',
    `description` TEXT COMMENT '项目描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PROGRESS' COMMENT '状态：PENDING-待启动，PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW-低，MEDIUM-中，HIGH-高',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `progress` INT(3) NOT NULL DEFAULT 0 COMMENT '进度百分比（0-100）',
    `budget` DECIMAL(15, 2) COMMENT '预算',
    `actual_cost` DECIMAL(15, 2) COMMENT '实际成本',
    `assignee_id` BIGINT(20) COMMENT '项目负责人ID',
    `creator_id` BIGINT(20) NOT NULL COMMENT '创建人ID',
    `milestones` TEXT COMMENT '里程碑节点JSON格式存储',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`),
    KEY `idx_assignee_id` (`assignee_id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目表';
-- ========================================
-- 3. 任务表
-- ========================================
DROP TABLE IF EXISTS `todos`;
CREATE TABLE `todos` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT COMMENT '任务描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT '状态：TODO-待办，PROGRESS-进行中，DONE-已完成',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW-低，MEDIUM-中，HIGH-高',
    `project_id` BIGINT(20) COMMENT '所属项目ID',
    `assignee_id` BIGINT(20) COMMENT '负责人ID',
    `creator_id` BIGINT(20) NOT NULL COMMENT '创建人ID',
    `due_date` DATE COMMENT '截止日期',
    `completed_time` DATETIME COMMENT '完成时间',
    `estimated_hours` DECIMAL(5, 2) COMMENT '预估工时',
    `actual_hours` DECIMAL(5, 2) COMMENT '实际工时',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_assignee_id` (`assignee_id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务表';
-- ========================================
-- 4. 邮件模板表
-- ========================================
DROP TABLE IF EXISTS `email_templates`;
CREATE TABLE `email_templates` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(100) NOT NULL COMMENT '模板代码（唯一标识）',
    `template_name` VARCHAR(200) NOT NULL COMMENT '模板名称',
    `subject_template` VARCHAR(500) NOT NULL COMMENT '邮件主题模板',
    `content_template` TEXT NOT NULL COMMENT '邮件内容模板（支持HTML和Thymeleaf语法）',
    `template_type` VARCHAR(20) NOT NULL DEFAULT 'HTML' COMMENT '模板类型（TEXT/HTML）',
    `description` TEXT COMMENT '模板描述',
    `supported_variables` TEXT COMMENT '支持的变量列表（JSON格式）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT(20) COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '邮件模板表';
-- ========================================
-- 5. 项目成员表
-- ========================================
DROP TABLE IF EXISTS `project_members`;
CREATE TABLE `project_members` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `project_id` BIGINT(20) NOT NULL COMMENT '项目ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：MANAGER-项目经理，MEMBER-成员',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `leave_time` DATETIME COMMENT '离开时间',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-在项目中，0-已离开',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目成员表';
-- ========================================
-- 6. 任务评论表
-- ========================================
DROP TABLE IF EXISTS `task_comments`;
CREATE TABLE `task_comments` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '评论人ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT(20) COMMENT '父评论ID（用于回复）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务评论表';
-- ========================================
-- 7. 文件附件表
-- ========================================
DROP TABLE IF EXISTS `attachments`;
CREATE TABLE `attachments` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `entity_type` VARCHAR(20) NOT NULL COMMENT '关联实体类型：PROJECT-项目，TASK-任务',
    `entity_id` BIGINT(20) NOT NULL COMMENT '关联实体ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT(20) NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) COMMENT '文件类型',
    `uploader_id` BIGINT(20) NOT NULL COMMENT '上传人ID',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_uploader_id` (`uploader_id`),
    KEY `idx_upload_time` (`upload_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件附件表';
-- ========================================
-- 8. 系统日志表
-- ========================================
DROP TABLE IF EXISTS `system_logs`;
CREATE TABLE `system_logs` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT(20) COMMENT '用户ID',
    `action` VARCHAR(100) NOT NULL COMMENT '操作类型',
    `entity_type` VARCHAR(50) COMMENT '操作对象类型',
    `entity_id` BIGINT(20) COMMENT '操作对象ID',
    `description` TEXT COMMENT '操作描述',
    `ip_address` VARCHAR(45) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统日志表';
-- ========================================
-- 初始化数据
-- ========================================
-- 1. 初始化管理员用户
DELETE FROM `users`
WHERE `username` = 'admin';
INSERT INTO `users` (
        `username`,
        `nickname`,
        `email`,
        `password`,
        `role`,
        `status`,
        `department`,
        `position`
    )
VALUES (
        'admin',
        '系统管理员',
        'admin@projectmanagement.com',
        '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC',
        'ADMIN',
        1,
        'IT部门',
        '系统管理员'
    );
-- 2. 初始化测试用户
DELETE FROM `users`
WHERE `username` IN ('zhang.san', 'li.si', 'wang.wu');
INSERT INTO `users` (
        `username`,
        `nickname`,
        `email`,
        `password`,
        `role`,
        `status`,
        `department`,
        `position`
    )
VALUES (
        'zhang.san',
        '张三',
        'zhang.san@projectmanagement.com',
        '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC',
        'USER',
        1,
        '开发部',
        '高级开发工程师'
    ),
    (
        'li.si',
        '李四',
        'li.si@projectmanagement.com',
        '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC',
        'USER',
        1,
        '产品部',
        '产品经理'
    ),
    (
        'wang.wu',
        '王五',
        'wang.wu@projectmanagement.com',
        '$2a$10$N.ZQRZKm2uMgZ4KtxpI5zOYJtgEGZwfJ5ZwOBTpOgH5E0zKwKLfyC',
        'USER',
        1,
        '设计部',
        'UI设计师'
    );
-- 3. 初始化示例项目
DELETE FROM `projects`
WHERE `name` IN ('项目管理系统开发', '移动端应用开发', '数据分析平台');
INSERT INTO `projects` (
        `name`,
        `description`,
        `status`,
        `priority`,
        `start_date`,
        `end_date`,
        `progress`,
        `manager_id`,
        `creator_id`
    )
VALUES (
        '项目管理系统开发',
        '开发一个功能完整的项目管理系统，支持任务管理、团队协作、进度跟踪等功能',
        'ACTIVE',
        'HIGH',
        '2024-01-01',
        '2024-06-30',
        75,
        2,
        1
    ),
    (
        '移动端应用开发',
        '开发配套的移动端应用，支持iOS和Android平台',
        'ACTIVE',
        'MEDIUM',
        '2024-02-01',
        '2024-08-31',
        30,
        3,
        1
    ),
    (
        '数据分析平台',
        '构建数据分析和报表平台，为决策提供数据支持',
        'ACTIVE',
        'MEDIUM',
        '2024-03-01',
        '2024-09-30',
        15,
        4,
        1
    );
-- 4. 初始化项目成员
DELETE FROM `project_members`;
INSERT INTO `project_members` (`project_id`, `user_id`, `role`)
VALUES (1, 2, 'MANAGER'),
    (1, 3, 'MEMBER'),
    (1, 4, 'MEMBER'),
    (2, 3, 'MANAGER'),
    (2, 4, 'MEMBER'),
    (3, 4, 'MANAGER'),
    (3, 2, 'MEMBER');
-- 5. 初始化示例任务
DELETE FROM `todos`;
INSERT INTO `todos` (
        `title`,
        `description`,
        `status`,
        `priority`,
        `project_id`,
        `assignee_id`,
        `creator_id`,
        `due_date`
    )
VALUES (
        '需求分析和设计',
        '完成项目需求分析文档和系统设计文档',
        'DONE',
        'HIGH',
        1,
        3,
        1,
        '2024-01-15'
    ),
    (
        '数据库设计',
        '设计数据库表结构和关系',
        'DONE',
        'HIGH',
        1,
        2,
        1,
        '2024-01-20'
    ),
    (
        '后端API开发',
        '开发用户管理、项目管理、任务管理等API接口',
        'PROGRESS',
        'HIGH',
        1,
        2,
        1,
        '2024-03-01'
    ),
    (
        '前端界面开发',
        '开发前端用户界面和交互功能',
        'PROGRESS',
        'HIGH',
        1,
        4,
        1,
        '2024-03-15'
    ),
    (
        '系统测试',
        '进行功能测试、性能测试和安全测试',
        'TODO',
        'MEDIUM',
        1,
        3,
        1,
        '2024-04-01'
    ),
    (
        '移动端UI设计',
        '设计移动端应用的用户界面',
        'PROGRESS',
        'MEDIUM',
        2,
        4,
        1,
        '2024-03-01'
    ),
    (
        'iOS应用开发',
        '开发iOS版本的移动应用',
        'TODO',
        'MEDIUM',
        2,
        3,
        1,
        '2024-05-01'
    ),
    (
        'Android应用开发',
        '开发Android版本的移动应用',
        'TODO',
        'MEDIUM',
        2,
        3,
        1,
        '2024-05-15'
    ),
    (
        '数据收集方案设计',
        '设计数据收集和清洗方案',
        'TODO',
        'MEDIUM',
        3,
        2,
        1,
        '2024-04-01'
    ),
    (
        '报表模板开发',
        '开发各种业务报表模板',
        'TODO',
        'LOW',
        3,
        4,
        1,
        '2024-05-01'
    );
-- 6. 初始化邮件模板
DELETE FROM `email_templates`;
-- 待办任务提醒模板
INSERT INTO `email_templates` (
        `template_code`,
        `template_name`,
        `subject_template`,
        `content_template`,
        `template_type`,
        `description`,
        `supported_variables`,
        `enabled`,
        `creator_id`
    )
VALUES (
        'TODO_REMINDER',
        '待办任务提醒',
        '[项目管理系统] 您有 ${todoCount} 个待办任务需要处理',
        '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; }
        .todo-item { background: white; margin: 10px 0; padding: 15px; border-left: 4px solid #007bff; border-radius: 4px; }
        .todo-title { font-weight: bold; color: #007bff; }
        .todo-due { color: #dc3545; font-size: 0.9em; }
        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 0.9em; }
        .btn { display: inline-block; padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📋 待办任务提醒</h1>
            <p>您有 <strong>${todoCount}</strong> 个任务需要处理</p>
        </div>
        
        <div class="content">
            <p>您好！</p>
            <p>以下是您今日的待办任务列表：</p>
            
            <!-- 遍历待办任务列表 -->
            <div th:each="todo : ${todoList}">
                <div class="todo-item">
                    <div class="todo-title" th:text="${todo.title}">任务标题</div>
                    <div th:if="${todo.description}" th:text="${todo.description}">任务描述</div>
                    <div th:if="${todo.dueDate}" class="todo-due">
                        📅 截止时间：<span th:text="${todo.dueDate}">2024-01-01</span>
                    </div>
                </div>
            </div>
            
            <p>请及时处理以上任务，确保工作进度不受影响。</p>
            
            <div style="text-align: center;">
                <a href="#" class="btn">查看详细任务</a>
            </div>
        </div>
        
        <div class="footer">
            <p>此邮件由项目管理系统自动发送，请勿直接回复。</p>
            <p>发送时间：${currentDate}</p>
        </div>
    </div>
</body>
</html>',
        'HTML',
        '用于发送待办任务提醒的邮件模板，支持任务列表展示和截止时间提醒',
        '{"email":"收件人邮箱","todoList":"待办任务列表","todoCount":"任务数量","currentDate":"当前日期"}',
        1,
        1
    );
-- 用户欢迎邮件模板
INSERT INTO `email_templates` (
        `template_code`,
        `template_name`,
        `subject_template`,
        `content_template`,
        `template_type`,
        `description`,
        `supported_variables`,
        `enabled`,
        `creator_id`
    )
VALUES (
        'USER_WELCOME',
        '用户欢迎邮件',
        '欢迎加入项目管理系统 - ${username}',
        '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #28a745; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; }
        .welcome-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; }
        .btn { display: inline-block; padding: 12px 24px; background: #28a745; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎉 欢迎加入项目管理系统</h1>
        </div>
        
        <div class="content">
            <div class="welcome-box">
                <h2>您好，${username}！</h2>
                <p>欢迎加入我们的项目管理系统！</p>
                <p>您的账户已成功创建，现在您可以开始使用系统的各种功能。</p>
            </div>
            
            <h3>🚀 系统主要功能：</h3>
            <ul>
                <li>📋 任务管理：创建、分配和跟踪任务进度</li>
                <li>📊 项目管理：管理项目信息和成员</li>
                <li>👥 团队协作：与团队成员高效协作</li>
                <li>📧 邮件提醒：及时获取任务和项目更新</li>
            </ul>
            
            <div style="text-align: center;">
                <a href="#" class="btn">立即开始使用</a>
            </div>
            
            <p>如果您在使用过程中遇到任何问题，请随时联系我们的支持团队。</p>
        </div>
        
        <div class="footer">
            <p>感谢您选择我们的项目管理系统！</p>
            <p>此邮件由系统自动发送，请勿直接回复。</p>
        </div>
    </div>
</body>
</html>',
        'HTML',
        '新用户注册后发送的欢迎邮件模板',
        '{"username":"用户名","email":"用户邮箱"}',
        1,
        1
    );
-- 简单通知模板
INSERT INTO `email_templates` (
        `template_code`,
        `template_name`,
        `subject_template`,
        `content_template`,
        `template_type`,
        `description`,
        `supported_variables`,
        `enabled`,
        `creator_id`
    )
VALUES (
        'SIMPLE_NOTIFICATION',
        '简单通知模板',
        '系统通知 - ${title}',
        '您好 ${username}！

${content}

如有疑问，请联系系统管理员。

此致
项目管理系统团队
${currentDate}',
        'TEXT',
        '简单的文本通知邮件模板，适用于各种通知场景',
        '{"username":"用户名","title":"通知标题","content":"通知内容","currentDate":"当前日期"}',
        1,
        1
    );
-- 项目状态变更通知模板
INSERT INTO `email_templates` (
        `template_code`,
        `template_name`,
        `subject_template`,
        `content_template`,
        `template_type`,
        `description`,
        `supported_variables`,
        `enabled`,
        `creator_id`
    )
VALUES (
        'PROJECT_STATUS_CHANGE',
        '项目状态变更通知',
        '项目状态变更通知 - ${projectName}',
        '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #17a2b8; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; }
        .status-box { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; border-left: 4px solid #17a2b8; }
        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 项目状态变更通知</h1>
        </div>
        
        <div class="content">
            <p>您好！</p>
            <p>项目 <strong>${projectName}</strong> 的状态已发生变更：</p>
            
            <div class="status-box">
                <p><strong>原状态：</strong>${oldStatus}</p>
                <p><strong>新状态：</strong>${newStatus}</p>
                <p><strong>变更时间：</strong>${changeTime}</p>
                <p><strong>变更人：</strong>${changedBy}</p>
            </div>
            
            <p th:if="${remark}">
                <strong>备注：</strong>${remark}
            </p>
            
            <p>请相关团队成员及时了解项目状态变化，调整工作计划。</p>
        </div>
        
        <div class="footer">
            <p>此邮件由项目管理系统自动发送，请勿直接回复。</p>
        </div>
    </div>
</body>
</html>',
        'HTML',
        '项目状态变更时发送的通知邮件模板',
        '{"projectName":"项目名称","oldStatus":"原状态","newStatus":"新状态","changeTime":"变更时间","changedBy":"变更人","remark":"备注"}',
        1,
        1
    );
-- 任务分配通知模板
INSERT INTO `email_templates` (
        `template_code`,
        `template_name`,
        `subject_template`,
        `content_template`,
        `template_type`,
        `description`,
        `supported_variables`,
        `enabled`,
        `creator_id`
    )
VALUES (
        'TASK_ASSIGNMENT',
        '任务分配通知',
        '新任务分配 - ${taskTitle}',
        '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #fd7e14; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; }
        .task-box { background: white; padding: 20px; border-radius: 8px; margin: 15px 0; border-left: 4px solid #fd7e14; }
        .priority-high { border-left-color: #dc3545; }
        .priority-medium { border-left-color: #ffc107; }
        .priority-low { border-left-color: #28a745; }
        .btn { display: inline-block; padding: 10px 20px; background: #fd7e14; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📋 新任务分配</h1>
        </div>
        
        <div class="content">
            <p>您好 ${assigneeName}！</p>
            <p>您有一个新任务被分配：</p>
            
            <div class="task-box">
                <h3>${taskTitle}</h3>
                <p><strong>项目：</strong>${projectName}</p>
                <p><strong>优先级：</strong>${priority}</p>
                <p th:if="${dueDate}"><strong>截止时间：</strong>${dueDate}</p>
                <p th:if="${description}"><strong>任务描述：</strong></p>
                <p th:if="${description}">${description}</p>
                <p><strong>分配人：</strong>${assignedBy}</p>
                <p><strong>分配时间：</strong>${assignedTime}</p>
            </div>
            
            <div style="text-align: center;">
                <a href="#" class="btn">查看任务详情</a>
            </div>
            
            <p>请及时查看任务详情并开始处理。如有疑问，请联系任务分配人。</p>
        </div>
        
        <div class="footer">
            <p>此邮件由项目管理系统自动发送，请勿直接回复。</p>
        </div>
    </div>
</body>
</html>',
        'HTML',
        '任务分配时发送给负责人的通知邮件',
        '{"assigneeName":"负责人姓名","taskTitle":"任务标题","projectName":"项目名称","priority":"优先级","dueDate":"截止时间","description":"任务描述","assignedBy":"分配人","assignedTime":"分配时间"}',
        1,
        1
    );
-- ========================================
-- 创建索引优化
-- ========================================
-- 为常用查询添加复合索引
CREATE INDEX `idx_todos_status_assignee` ON `todos` (`status`, `assignee_id`);
CREATE INDEX `idx_todos_project_status` ON `todos` (`project_id`, `status`);
CREATE INDEX `idx_todos_due_date_status` ON `todos` (`due_date`, `status`);
-- ========================================
-- 设置自增起始值
-- ========================================
ALTER TABLE `users` AUTO_INCREMENT = 1;
ALTER TABLE `projects` AUTO_INCREMENT = 1;
ALTER TABLE `todos` AUTO_INCREMENT = 1;
ALTER TABLE `email_templates` AUTO_INCREMENT = 1;
ALTER TABLE `project_members` AUTO_INCREMENT = 1;
ALTER TABLE `task_comments` AUTO_INCREMENT = 1;
ALTER TABLE `attachments` AUTO_INCREMENT = 1;
ALTER TABLE `system_logs` AUTO_INCREMENT = 1;
-- ========================================
-- 恢复外键检查
-- ========================================
SET FOREIGN_KEY_CHECKS = 1;
-- ========================================
-- 执行完成提示
-- ========================================
SELECT '数据库初始化完成！' AS message,
    (
        SELECT COUNT(*)
        FROM users
    ) AS users_count,
    (
        SELECT COUNT(*)
        FROM projects
    ) AS projects_count,
    (
        SELECT COUNT(*)
        FROM todos
    ) AS todos_count,
    (
        SELECT COUNT(*)
        FROM email_templates
    ) AS email_templates_count;