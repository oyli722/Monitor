@echo off
REM ZooKeeper 状态检查脚本

echo ========================================
echo Monitor System - 状态检查
echo ========================================
echo.

echo [1/5] 检查Docker状态...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker未运行
    pause
    exit /b 1
)
echo [OK] Docker运行正常
echo.

echo [2/5] 检查ZooKeeper容器状态...
docker ps | findstr monitor-zookeeper >nul
if %errorlevel% neq 0 (
    echo [错误] ZooKeeper容器未运行
    echo.
    echo 请先执行: start-zookeeper.bat
    pause
    exit /b 1
)
echo [OK] ZooKeeper容器运行中
echo.

echo [3/5] 检查ZooKeeper连接...
docker exec monitor-zookeeper nc -zv localhost 2181 >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] ZooKeeper服务未就绪
    echo 请稍后重试
) else (
    echo [OK] ZooKeeper服务就绪 (端口2181)
)
echo.

echo [4/5] 检查ZooKeeper节点结构...
echo.
echo 执行命令: ls /monitor
echo ----------------------------------------
docker exec -it monitor-zookeeper zkCli.sh ls /monitor 2>nul | findstr /V "\["
echo ----------------------------------------
echo.
echo 执行命令: ls /monitor/servers
echo ----------------------------------------
docker exec -it monitor-zookeeper zkCli.sh ls /monitor/servers 2>nul | findstr /V "\["
echo ----------------------------------------
echo.

echo [5/5] 显示容器详细信息...
echo.
docker ps --filter "name=monitor-zookeeper" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.

echo ========================================
echo 状态检查完成！
echo ========================================
echo.
echo 如果看到 /monitor/servers 列表中有 server-xxx 节点，
echo 说明 Monitor-Server 已成功注册到 ZooKeeper。
echo.
echo 常用操作:
echo   - 查看Server元数据: docker exec -it monitor-zookeeper zkCli.sh get /monitor/servers/server-id/info
echo   - 进入ZooKeeper CLI: docker exec -it monitor-zookeeper zkCli.sh
echo   - 查看实时日志: docker logs -f monitor-zookeeper
echo.
pause
