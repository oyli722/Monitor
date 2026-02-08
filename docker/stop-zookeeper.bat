@echo off
REM ZooKeeper 停止脚本 (Windows)

echo ========================================
echo Monitor System - ZooKeeper 停止脚本
echo ========================================
echo.

REM 检查Docker是否运行
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker未运行
    pause
    exit /b 1
)

echo [1/3] 检查Docker状态... OK
echo.

REM 进入docker目录
cd /d "%~dp0"
echo [2/3] 切换到docker目录... OK
echo.

REM 停止ZooKeeper
echo [3/3] 停止ZooKeeper容器...
docker-compose -f docker-compose-zookeeper.yml down

if %errorlevel% neq 0 (
    echo [错误] ZooKeeper停止失败
    pause
    exit /b 1
)

echo [OK] ZooKeeper已停止
echo.
echo ========================================
echo ZooKeeper 已成功停止！
echo ========================================
echo.
echo 数据已保留在Docker volume中
echo 如需清理数据，请执行: docker-compose -f docker-compose-zookeeper.yml down -v
echo.
pause
