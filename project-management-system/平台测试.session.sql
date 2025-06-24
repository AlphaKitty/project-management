-- 用户搜索性能优化 SQL 脚本
-- 解决 /api/users/search 接口性能问题
-- ================================================
-- 分析当前性能问题
-- ================================================
-- 1. users表有36,755条记录，全部status=1
-- 2. 当前使用 LIKE '%keyword%' 无法使用索引
-- 3. OR条件降低查询效率
-- 4. 没有针对搜索场景的复合索引
-- ================================================
-- 索引优化方案
-- ================================================
-- 1. 创建前缀搜索索引（支持 LIKE 'keyword%'）
CREATE INDEX idx_users_username_prefix ON users (status, username(20));
CREATE INDEX idx_users_nickname_prefix ON users (status, nickname(20));
-- 2. 创建精确匹配的复合索引
CREATE INDEX idx_users_exact_search ON users (status, username, id);
-- 3. 针对ID搜索的优化索引（已存在PRIMARY KEY，无需额外创建）
-- 4. 统计信息更新
ANALYZE TABLE users;
-- ================================================
-- 性能测试查询
-- ================================================
-- 测试1：精确匹配（应该很快）
-- SELECT * FROM users WHERE status = 1 AND username = 'test_user';
-- 测试2：前缀匹配（应该较快）
-- SELECT * FROM users WHERE status = 1 AND username LIKE 'test%' LIMIT 50;
-- 测试3：ID搜索（应该很快）
-- SELECT * FROM users WHERE status = 1 AND id = 12345;
-- ================================================
-- 查询执行计划分析
-- ================================================
-- 分析当前查询性能
EXPLAIN
SELECT *
FROM users
WHERE status = 1
    AND (
        username LIKE '%张%'
        OR nickname LIKE '%张%'
        OR id = 12345
    )
ORDER BY id
LIMIT 50;
-- 分析优化后的查询性能
EXPLAIN
SELECT *
FROM users
WHERE status = 1
    AND username LIKE '张%'
ORDER BY id
LIMIT 50;
-- ================================================
-- 监控查询
-- ================================================
-- 查看慢查询日志配置
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
-- 查看当前连接数
SHOW STATUS LIKE 'Threads_connected';
-- 查看缓存命中率
SHOW STATUS LIKE 'Qcache%';
-- ================================================
-- 可选：分区优化（如果数据量继续增长）
-- ================================================
-- 注意：分区需要谨慎评估，建议先尝试索引优化
-- 可以考虑按部门、创建时间等字段进行分区
-- ================================================
-- 应用层优化建议
-- ================================================
-- 1. 实现搜索结果缓存（Redis）
-- 2. 使用分层搜索策略（精确->前缀->模糊）
-- 3. 限制搜索关键字最小长度（>=2字符）
-- 4. 实现搜索防抖（客户端300ms延迟）
-- 5. 限制搜索结果数量（最多50条）
-- ================================================
-- 清理脚本（如果需要回滚）
-- ================================================
-- DROP INDEX idx_users_username_prefix ON users;
-- DROP INDEX idx_users_nickname_prefix ON users;
-- DROP INDEX idx_users_exact_search ON users;