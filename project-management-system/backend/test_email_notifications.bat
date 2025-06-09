@echo off
chcp 65001 >nul 2>&1
:: 邮件通知测试脚本 (Windows版本)

set BASE_URL=http://localhost:8080/api
set COOKIE_FILE=session.cookie

echo ===========================================
echo 邮件通知功能测试脚本
echo ===========================================
echo.

:: 1. 登录
echo [1] 正在登录...
curl -c "%COOKIE_FILE%" -s -X POST "%BASE_URL%/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"barlin.zhang\",\"password\":\"0000\"}"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] 登录失败
    goto end
)

echo [OK] 登录成功
echo.

:: 2. 快速测试
echo [2] 快速测试 - 处理邮件队列...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=queue"
echo.

:: 3. 主菜单
:menu
echo ===========================================
echo 邮件通知测试菜单
echo ===========================================
echo 1. 任务分配通知测试
echo 2. 任务状态变更通知测试
echo 3. 任务完成通知测试
echo 4. 截止日期提醒测试
echo 5. 逾期任务提醒测试
echo 6. 待办任务提醒测试
echo 7. 处理邮件队列
echo 8. 查看任务列表
echo 9. 退出测试
echo.
set /p choice="请选择 (1-9): "

if "%choice%"=="1" goto assignment
if "%choice%"=="2" goto status_change
if "%choice%"=="3" goto completion
if "%choice%"=="4" goto deadline
if "%choice%"=="5" goto overdue
if "%choice%"=="6" goto reminder
if "%choice%"=="7" goto queue
if "%choice%"=="8" goto list
if "%choice%"=="9" goto end
echo [ERROR] 无效选择
goto menu

:assignment
echo.
echo [TEST] 任务分配通知
set /p tid="请输入任务ID: "
if "%tid%"=="" (
    echo [ERROR] 任务ID不能为空
    goto menu
)
echo 发送任务分配通知测试...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=assignment&todoId=%tid%"
echo.
pause
goto menu

:status_change
echo.
echo [TEST] 任务状态变更通知
set /p tid="请输入任务ID: "
if "%tid%"=="" (
    echo [ERROR] 任务ID不能为空
    goto menu
)
echo 发送状态变更通知测试...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=status_change&todoId=%tid%"
echo.
pause
goto menu

:completion
echo.
echo [TEST] 任务完成通知
set /p tid="请输入任务ID: "
if "%tid%"=="" (
    echo [ERROR] 任务ID不能为空
    goto menu
)
echo 发送任务完成通知测试...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=completion&todoId=%tid%"
echo.
pause
goto menu

:deadline
echo.
echo [TEST] 截止日期提醒
echo 处理所有即将到期的任务...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=deadline"
echo.
pause
goto menu

:overdue
echo.
echo [TEST] 逾期任务提醒
echo 处理所有逾期任务...
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=overdue"
echo.
pause
goto menu

:reminder
echo.
echo [TEST] 待办任务提醒
set /p email="请输入接收邮件的地址（直接回车使用当前用户邮箱）: "

if "%email%"=="" (
    curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=reminder"
) else (
    curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=reminder&email=%email%"
)
echo.
pause
goto menu

:queue
echo.
echo [TEST] 处理邮件队列
curl -b "%COOKIE_FILE%" -s -X POST "%BASE_URL%/todos/test-email" -d "type=queue"
echo.
pause
goto menu

:list
echo.
echo [LIST] 当前任务列表
curl -b "%COOKIE_FILE%" -s -X GET "%BASE_URL%/todos"
echo.
pause
goto menu

:end
echo.
echo ===========================================
echo [DONE] 邮件通知测试完成！
echo ===========================================

:: 清理临时文件
if exist "%COOKIE_FILE%" del "%COOKIE_FILE%"
pause 