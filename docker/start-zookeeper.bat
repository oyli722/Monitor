@echo off
REM ZooKeeper 快速启动脚本 (Windows)

echo ========================================
echo Monitor System - ZooKeeper 启动脚本
echo ========================================
echo.

REM 检查Docker是否运行
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker未运行，请先启动Docker Desktop
    pause
    exit /b 1
)

echo [1/4] 检查Docker状态... OK
echo.

REM 进入docker目录
cd /d "%~dp0"
echo [2/4] 切换到docker目录... OK
echo.

REM 启动ZooKeeper
echo [3/4] 启动ZooKeeper容器...
docker-compose -f docker-compose-zookeeper.yml up -d

if %errorlevel% neq 0 (
    echo [错误] ZooKeeper启动失败
    pause
    exit /b 1
)

echo [OK] ZooKeeper启动成功
echo.

REM 等待ZooKeeper完全启动
echo [4/4] 等待ZooKeeper就绪...
timeout /t 5 /nobreak >nul

echo.
echo ========================================
echo ZooKeeper 已启动并就绪！
echo ========================================
echo.
echo 连接信息:
echo   - 地址: localhost:2181
echo   - 容器名: monitor-zookeeper
echo.
echo 常用命令:
echo   查看日志: docker logs -f monitor-zookeeper
echo   停止服务: docker-compose -f docker-compose-zookeeper.yml down
echo   CLI客户端: docker exec -it monitor-zookeeper zkCli.sh
echo.
echo 下一步:
echo   1. 启动 Monitor-Server (端口8080)
echo   2. 启动 Monitor-Agent
echo   3. 验证服务发现功能
echo.
echo ========================================

REM 验证ZooKeeper运行状态
docker ps | findstr zookeeper

if %errorlevel% neq 0 (
    echo [警告] ZooKeeper容器未在运行
) else (
    echo [OK] ZooKeeper容器运行正常
)

echo.
pause
