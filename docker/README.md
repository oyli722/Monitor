# Docker 部署脚本

本目录包含 Monitor 系统的 Docker 部署脚本和配置文件。

## 快速开始

### Windows 用户

#### 启动 ZooKeeper

双击运行 `start-zookeeper.bat`

或在命令行中执行：
```bash
cd docker
start-zookeeper.bat
```

#### 检查状态

双击运行 `check-status.bat`

#### 停止 ZooKeeper

双击运行 `stop-zookeeper.bat`

### Linux/Mac 用户

#### 启动 ZooKeeper

```bash
cd docker
docker-compose -f docker-compose-zookeeper.yml up -d
```

#### 检查状态

```bash
docker ps | grep zookeeper
docker exec -it monitor-zookeeper zkCli.sh ls /monitor/servers
```

#### 停止 ZooKeeper

```bash
docker-compose -f docker-compose-zookeeper.yml down
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `docker-compose-zookeeper.yml` | 仅启动 ZooKeeper 的配置 |
| `docker-compose.yml` | 完整环境（ZooKeeper + MySQL + Redis + RabbitMQ）|
| `start-zookeeper.bat` | Windows 快速启动脚本 |
| `stop-zookeeper.bat` | Windows 快速停止脚本 |
| `check-status.bat` | Windows 状态检查脚本 |
| `ZooKeeper测试指南.md` | 详细的测试指南 |

## 服务端口

| 服务 | 端口 |
|------|------|
| ZooKeeper | 2181 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 (管理界面: 15672) |

## 测试流程

1. **启动 ZooKeeper**
   ```bash
   start-zookeeper.bat
   ```

2. **启动 Monitor-Server**
   ```bash
   cd ../monitor-project
   mvn clean install
   mvn spring-boot:run -pl Monitor-Server
   ```

3. **验证 Server 注册**
   ```bash
   check-status.bat
   ```

4. **启动 Monitor-Agent**
   ```bash
   mvn spring-boot:run -pl Monitor-Agent
   ```

5. **查看服务发现日志**

   Server 日志应显示：
   ```
   Successfully registered server with Zookeeper
   ```

   Agent 日志应显示：
   ```
   Discovered N servers from Zookeeper
   ```

## 故障排查

### ZooKeeper 无法启动

```bash
# 查看日志
docker logs monitor-zookeeper

# 检查端口占用
netstat -ano | findstr :2181
```

### Server 无法注册

1. 确认 ZooKeeper 正在运行
2. 检查 `Monitor-Server/src/main/resources/application.yaml` 配置
3. 查看 Server 启动日志

### Agent 无法发现 Server

1. 确认 Server 已注册：`check-status.bat`
2. 检查 `Monitor-Agent/src/main/resources/application.yaml` 配置
3. 查看 Agent 启动日志

## 清理数据

⚠️ **警告**：此操作将删除所有数据！

```bash
docker-compose -f docker-compose-zookeeper.yml down -v
```

## 更多信息

详细测试指南请参考：`ZooKeeper测试指南.md`
