@echo off
echo "=============== Updating Project Management System backend project ==============="

@REM echo "=============== Fetching from dev1.0 ==============="
@REM git pull origin dev1.0

echo "=============== Cleaning & Building ==============="
mvn clean package -f pom.xml -DskipTests

if %ERRORLEVEL% neq 0 (
    echo "=============== Error: Maven build failed. ==============="
    exit /b %ERRORLEVEL%
)
echo "=============== Build completed successfully. ==============="
