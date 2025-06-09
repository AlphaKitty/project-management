#!/bin/bash

# 邮件通知测试脚本
# 使用方法：./test_email_notifications.sh

BASE_URL="http://localhost:8080/api"
COOKIE_FILE="session.cookie"

echo "==========================================="
echo "📧 邮件通知功能测试脚本"
echo "==========================================="

# 1. 登录获取session
echo "1️⃣ 正在登录..."
curl -c "$COOKIE_FILE" -s -X POST "$BASE_URL/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username":"barlin.zhang","password":"123456"}' | jq .

if [ $? -ne 0 ]; then
    echo "❌ 登录失败，请检查用户名密码"
    exit 1
fi

echo "✅ 登录成功"
echo

# 2. 测试任务分配通知
echo "2️⃣ 测试任务分配通知（需要提供todoId）..."
read -p "请输入要测试的任务ID（或按回车跳过）: " TODO_ID

if [ ! -z "$TODO_ID" ]; then
    echo "发送任务分配通知测试..."
    curl -b "$COOKIE_FILE" -s -X POST "$BASE_URL/todos/test-email" \
         -d "type=assignment&todoId=$TODO_ID" | jq .
    echo
fi

# 3. 测试状态变更通知
echo "3️⃣ 测试状态变更通知（需要提供todoId）..."
read -p "请输入要测试状态变更的任务ID（或按回车跳过）: " STATUS_TODO_ID

if [ ! -z "$STATUS_TODO_ID" ]; then
    echo "发送状态变更通知测试..."
    curl -b "$COOKIE_FILE" -s -X POST "$BASE_URL/todos/test-email" \
         -d "type=status_change&todoId=$STATUS_TODO_ID" | jq .
    echo
fi

# 4. 测试待办任务提醒
echo "4️⃣ 测试待办任务提醒..."
read -p "请输入接收邮件的地址（或按回车使用当前用户邮箱）: " EMAIL

EMAIL_PARAM=""
if [ ! -z "$EMAIL" ]; then
    EMAIL_PARAM="&email=$EMAIL"
fi

echo "发送待办任务提醒测试..."
curl -b "$COOKIE_FILE" -s -X POST "$BASE_URL/todos/test-email" \
     -d "type=reminder$EMAIL_PARAM" | jq .
echo

# 5. 测试邮件队列处理
echo "5️⃣ 测试邮件队列处理..."
curl -b "$COOKIE_FILE" -s -X POST "$BASE_URL/todos/test-email" \
     -d "type=queue" | jq .
echo

# 6. 查看邮件队列状态
echo "6️⃣ 查看当前邮件队列状态..."
curl -b "$COOKIE_FILE" -s -X GET "$BASE_URL/email-queue/status" | jq . 2>/dev/null || echo "邮件队列状态接口暂未实现"

echo
echo "==========================================="
echo "✅ 邮件通知测试完成！"
echo "==========================================="
echo "💡 使用说明："
echo "   - assignment: 测试任务分配通知（需要todoId参数）"
echo "   - status_change: 测试状态变更通知（需要todoId参数）"
echo "   - reminder: 测试待办任务提醒（可选email参数）"
echo "   - queue: 手动处理邮件队列"
echo
echo "🔧 手动测试命令示例："
echo "   curl -b session.cookie -X POST '$BASE_URL/todos/test-email' -d 'type=queue'"
echo "   curl -b session.cookie -X POST '$BASE_URL/todos/test-email' -d 'type=reminder&email=test@example.com'"

# 清理临时文件
rm -f "$COOKIE_FILE" 