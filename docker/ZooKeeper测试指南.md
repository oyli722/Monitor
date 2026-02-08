# ZooKeeper 测试指南

## 快速启动

### 方式一：仅启动ZooKeeper（推荐用于服务发现测试）

```bash
cd docker
docker-compose -f docker-compose-zookeeper.yml up -d
```

### 方式二：启动完整环境（包含MySQL、Redis、RabbitMQ）

```bash
cd docker
docker-compose up -d
```

## 验证ZooKeeper运行状态

### 1. 检查容器状态

```bash
docker ps | grep zookeeper
```

应该看到：
```
CONTAINER ID   IMAGE                  STATUS         PORTS
xxx        zookeeper:3.8.4        Up 30 seconds   0.0.0.0:2181->2181/tcp
```

### 2. 查看日志

```bash
docker logs -f monitor-zookeeper
```

### 3. 测试连接

```bash
# 使用nc命令测试
nc -zv localhost 2181

# 或使用telnet
telnet localhost 2181
```

### 4. 使用ZooKeeper客户端验证

```bash
# 进入ZooKeeper容器
docker exec -it monitor-zookeeper bash

# 启动CLI客户端
zkCli.sh

# 在CLI中执行以下命令验证
ls /
create /test "hello"
get /test
ls /
delete /test
```

## 测试Monitor服务发现

### 1. 启动Monitor-Server

Server会自动注册到ZooKeeper：

```bash
cd monitor-project
mvn clean install
mvn spring-boot:run -pl Monitor-Server
```

查看Server日志，应该看到：
```
Successfully connected to Zookeeper at: localhost:2181
Successfully registered server with Zookeeper: id=Monitor-Server-8080, host=xxx, port=8080
```

### 2. 验证Server注册

在ZooKeeper CLI中执行：

```bash
# 查看monitor路径
ls /monitor

# 查看servers路径
ls /monitor/servers

# 查看Server元数据
get /monitor/servers/Monitor-Server-8080/info
```

应该看到类似输出：

```
{"id":"Monitor-Server-8080","host":"192.168.1.100","port":8080,"registeredAt":"2026-02-08T10:00:00Z","status":"ACTIVE"}
```

### 3. 启动Monitor-Agent

Agent会自动发现Server：

```bash
cd monitor-project
mvn spring-boot:run -pl Monitor-Agent
```

查看Agent日志，应该看到：
```
Initializing service discovery from Zookeeper...
Discovered 1 servers from Zookeeper: http://localhost:8080
```

### 4. 测试多Server场景

启动多个Server实例（不同端口）：

```bash
# Server实例1（端口8080）
mvn spring-boot:run -pl Monitor-Server -Dspring-boot.run.arguments="--server.port=8080"

# Server实例2（端口8081）
mvn spring-boot:run -pl Monitor-Server -Dspring-boot.run.arguments="--server.port=8081"
```

在ZooKeeper中验证：

```bash
ls /monitor/servers
# 输出: [server-Monitor-Server-8080, server-Monitor-Server-8081]
```

Agent会自动发现所有Server：

```bash
# Agent日志
Discovered 2 servers from Zookeeper: http://localhost:8080, http://localhost:8081
```

### 5. 测试故障转移

**场景1：Server下线**

```bash
# 停止一个Server实例
docker stop monitor-zookeeper  # 或 Ctrl+C 停止Server进程

# Agent会在下次刷新时自动移除失效Server
# Agent日志: Server节点丢失，自动移除
```

**场景2：ZooKeeper故障恢复**

```bash
# 1. 停止ZooKeeper
docker-compose -f docker-compose-zookeeper.yml stop zookeeper

# 2. Agent会降级到静态配置
# Agent日志: ZooKeeper连接失败，使用fallback配置

# 3. 重启ZooKeeper
docker-compose -f docker-compose-zookeeper.yml start zookeeper

# 4. Agent会在下次刷新时自动恢复服务发现
# Agent日志: ZooKeeper恢复，切换到服务发现模式
```

## 常用ZooKeeper命令

### 查看节点结构

```bash
# 递归查看所有节点
ls -R /monitor

# 查看节点状态
stat /monitor/servers/Monitor-Server-8080
```

### 监控节点变化

```bash
# 添加监听器
ls -w /monitor/servers

# 当Server加入或离开时会收到通知
```

### 删除测试节点

```bash
# 删除单个节点
delete /monitor/servers/server-test

# 递归删除
deleteall /monitor/test
```

## 停止服务

### 停止ZooKeeper

```bash
docker-compose -f docker-compose-zookeeper.yml down
```

### 停止完整环境

```bash
docker-compose down
```

### 清理数据（谨慎使用）

```bash
docker-compose -f docker-compose-zookeeper.yml down -v
```

## 故障排查

### ZooKeeper无法启动

```bash
# 查看日志
docker logs monitor-zookeeper

# 检查端口占用
netstat -ano | findstr :2181
```

### Server无法注册

1. 检查ZooKeeper是否运行：`docker ps | grep zookeeper`
2. 检查配置：`application.yaml`中的`zookeeper.connection-string`
3. 查看Server日志中的错误信息

### Agent无法发现Server

1. 确认Server已注册到ZooKeeper：`ls /monitor/servers`
2. 检查Agent配置中的`zookeeper.discovery.enabled`
3. 查看Agent日志中的服务发现信息

## 性能测试

### 模拟大量Server

```bash
# 循环启动多个Server实例
for i in {8080..8090}; do
  mvn spring-boot:run -pl Monitor-Server -Dspring-boot.run.arguments="--server.port=$i" &
done
```

### 监控ZooKeeper性能

```bash
# 查看ZooKeeper统计信息
echo "stats" | nc localhost 2181

# 或使用四字命令
echo "mntr" | nc localhost 2181
```

## 下一步

测试完成后，你可以：
1. 修改`application.yaml`中的ZooKeeper配置
2. 调整服务发现的刷新间隔
3. 测试不同的负载均衡策略
4. 验证降级机制
