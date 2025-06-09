@echo off
echo "=============== Updating Project Management System frontend project ==============="

echo "=============== Deleting Old version archive ==============="
del /q "D:\Users\barlin.zhang\Desktop\shell\zip\project-management-system.zip"
del /q ".\dist\*"

@REM echo "=============== Fetching from dev1.0 ==============="
@REM git pull origin dev1.0

echo "=============== Npm Building ==============="
npm run build

@REM if %ERRORLEVEL% neq 0 (
@REM     echo "=============== Error: Npm build failed. ==============="
@REM     exit /b %ERRORLEVEL%
@REM ) else (
@REM     7z a "D:\Users\barlin.zhang\Desktop\shell\zip\dist.zip" ".\dist\*"
@REM     echo "=============== Build completed successfully. ==============="
@REM )
