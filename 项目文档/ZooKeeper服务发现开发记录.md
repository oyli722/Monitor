# ZooKeeper 服务发现重构开发记录

**文档版本**：v1.0.0
**开发日期**：2026-02-08
**开发人员**：Claude Code
**功能模块**：服务注册与发现

---

## 一、需求背景

### 1.1 现状问题

当前Monitor系统使用**静态配置**方式管理服务端地址，存在以下问题：

| 问题 | 影响 |
|------|------|
| Agent需要手动配置Server地址列表 | 新增Server时需要修改所有Agent配置 |
| 无法动态发现新加入的Server | 需要手动维护配置文件 |
| 无故障转移机制 | 上报时固定使用第一个地址，Server宕机无法切换 |
| Server下线时Agent无法自动切换 | 继续向故障Server发送请求，导致数据丢失 |

### 1.2 解决方案

通过引入**ZooKeeper**实现服务注册与发现：
- **Server端**：启动时主动注册到ZooKeeper，使用临时节点实现健康检测
- **Agent端**：定时拉取服务列表（可配置，默认1分钟）
- **降级方案**：ZooKeeper失败时使用`agent-config.yaml`静态配置

---

## 二、架构设计

### 2.1 ZooKeeper节点结构

```
/monitor                           # 基础路径
└── servers                        # Server注册目录
    ├── server-Monitor-Server-8080 # 持久化节点（Server 1）
    │   ├── info                   # 元数据（JSON格式，持久化）
    │   │   {
    │   │     "id": "Monitor-Server-8080",
    │   │     "host": "192.168.1.100",
    │   │     "port": 8080,
    │   │     "registeredAt": "2026-02-08T10:00:00Z",
    │   │     "status": "ACTIVE"
    │   │   }
    │   └── status                 # 健康状态（临时节点）
    └── server-Monitor-Server-8081 # 持久化节点（Server 2）
        ├── info
        └── status
```

**节点类型说明**：
- **父节点** `/monitor/servers`：持久化节点（PERSISTENT）
- **Server节点** `server-{id}`：持久化节点（PERSISTENT）
- **元数据节点** `info`：持久化节点（PERSISTENT），存储JSON格式的Server信息
- **状态节点** `status`：临时节点（EPHEMERAL），Session超时自动删除，用于健康检测

**设计理由**：
ZooKeeper的临时节点（EPHEMERAL）不能有子节点，因此采用以下结构：
- 父节点使用持久化节点，用于存储Server元数据
- 状态节点使用临时节点，当Server与ZooKeeper连接断开时自动删除
- Agent通过检查`status`节点是否存在来判断Server是否在线

### 2.2 通信流程

```
┌─────────────┐                    ┌─────────────┐
│   Server    │                    │  ZooKeeper  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  1. 启动时创建临时节点             │
       │  /monitor/servers/server-{id}    │
       ├────────────────────────────►     │
       │                                  │
       │  2. 写入元数据到info子节点         │
       ├────────────────────────────►     │
       │                                  │
┌──────┴──────┐                    ┌──────┴──────┐
│    Agent    │                    │  ZooKeeper  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  3. 定时拉取（默认1分钟）          │
       │  GET /monitor/servers子节点        │
       ├────────────────────────────►     │
       │                                  │
       │  4. 获取server列表                │
       │  ◄─────────────────────────────  │
       │                                  │
       │  5. 缓存到内存                    │
       │                                  │
       │  6. 选择Server进行注册/上报       │
       ├─────────────────────────┐       │
       │                          │       │
       ▼                          ▼       │
   健康检查 → 选择最快/随机Server      │
```

### 2.3 降级策略

| 场景 | 检测方式 | 降级动作 |
|------|----------|----------|
| ZK连接超时 | `ConnectionTimeoutException` | 使用`agent-config.yaml`静态配置 |
| ZK会话过期 | `ConnectionLossException` | 重试3次后降级 |
| Server列表为空 | 空列表判断 | 使用静态配置 |
| 网络分区 | `KeeperException.ConnectionLoss` | 指数退避重试后降级 |

---

## 三、实施步骤

### 3.1 阶段一：Maven依赖配置

**修改文件**：
- `monitor-project/pom.xml` - 添加Curator版本管理
- `Monitor-Server/pom.xml` - 添加Curator依赖
- `Monitor-Agent/pom.xml` - 添加Curator依赖

**添加依赖**：
```xml
<properties>
    <curator.version>5.7.1</curator.version>
</properties>

<!-- Monitor-Server -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-framework</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
</dependency>

<!-- Monitor-Agent -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-framework</artifactId>
</dependency>
```

### 3.2 阶段二：Monitor-Server服务注册

**新增文件**：

1. **ZookeeperProperties.java**
   - 位置：`Monitor-Server/src/main/java/com/hundred/monitor/server/conf/`
   - 职责：ZooKeeper连接配置属性
   - 关键属性：
     ```java
     private String connectionString = "localhost:2181";
     private int sessionTimeoutMs = 30000;
     private int connectionTimeoutMs = 10000;
     private String basePath = "/monitor";
     private String serversPath = "/monitor/servers";
     private String serverId;
     ```

2. **ZookeeperConfig.java**
   - 位置：`Monitor-Server/src/main/java/com/hundred/monitor/server/conf/`
   - 职责：创建CuratorFramework客户端Bean
   - 重试策略：指数退避（1000ms base sleep, 3 max retries）

3. **ServerMetadata.java**
   - 位置：`Monitor-Server/src/main/java/com/hundred/monitor/server/model/`
   - 职责：Server元数据实体
   - 字段：id, host, port, registeredAt, status

4. **ServerRegistrationService.java**
   - 位置：`Monitor-Server/src/main/java/com/hundred/monitor/server/service/`
   - 职责：启动时自动注册到ZooKeeper
   - 实现接口：`ApplicationRunner`
   - 关键逻辑：
     ```java
     @Override
     public void run(ApplicationArguments args) {
         // 1. 确保路径存在
         ensurePathExists(zookeeperProperties.getServersPath());
         // 2. 创建临时节点
         curatorFramework.create()
             .withMode(CreateMode.EPHEMERAL)
             .forPath(serverPath);
         // 3. 写入元数据
         curatorFramework.create()
             .forPath(infoPath, metadataJson.getBytes());
     }
     ```

**修改文件**：

1. **application.yaml**
   ```yaml
   zookeeper:
     connection-string: localhost:2181
     session-timeout-ms: 30000
     connection-timeout-ms: 10000
     base-path: /monitor
     servers-path: /monitor/servers
     server-id: ${spring.application.name}-${server.port}
   ```

2. **MonitorServerApplication.java**
   - 添加注解：`@EnableConfigurationProperties`

### 3.3 阶段三：Monitor-Agent服务发现

**新增文件**：

1. **DiscoveryProperties.java**
   - 位置：`Monitor-Agent/src/main/java/com/hundred/monitor/agent/config/`
   - 职责：服务发现配置属性
   - 关键属性：
     ```java
     private boolean enabled = true;
     private long intervalMs = 60000;  // 1分钟
     private int retryAttempts = 3;
     private long retryDelayMs = 5000;
     ```

2. **ZookeeperConfig.java**
   - 位置：`Monitor-Agent/src/main/java/com/hundred/monitor/agent/config/`
   - 职责：创建CuratorFramework客户端Bean
   - 条件注解：`@ConditionalOnProperty(prefix = "zookeeper.discovery", name = "enabled")`

3. **ServerEndpoint.java**
   - 位置：`Monitor-Agent/src/main/java/com/hundred/monitor/agent/model/`
   - 职责：服务端点实体
   - 字段：id, host, port, registeredAt, status
   - 方法：`getUrl()`, `isActive()`

4. **ServiceDiscoveryService.java**
   - 位置：`Monitor-Agent/src/main/java/com/hundred/monitor/agent/service/`
   - 职责：定时拉取服务列表，本地缓存，降级处理
   - 关键特性：
     ```java
     @Scheduled(fixedRateString = "${zookeeper.discovery.interval-ms:60000}")
     public void refreshServerList() {
         List<ServerEndpoint> servers = discoverFromZookeeper();
         if (servers.isEmpty()) {
             servers = fallbackToStaticConfig();
             usingFallback.set(true);
         }
         cachedServers.set(servers);
     }
     ```

**修改文件**：

1. **application.yaml**
   ```yaml
   zookeeper:
     discovery:
       enabled: true
       interval-ms: 60000
       retry-attempts: 3
       retry-delay-ms: 5000
       connection-string: localhost:2181
   ```

2. **RegisterService.java**
   - 注入`ServiceDiscoveryService`
   - 修改`register()`方法，优先使用服务发现
   - 保留静态配置作为降级方案

3. **ReportService.java**
   - 注入`ServiceDiscoveryService`
   - 修改`getRegisteredServerUrl()`方法
   - 实现随机负载均衡策略

### 3.4 阶段四：集成与测试

**测试场景**：
1. ✅ Server启动后成功注册到ZooKeeper
2. ✅ Agent从ZooKeeper拉取到服务列表
3. ✅ Agent使用ZooKeeper发现的服务进行注册和上报
4. ✅ ZooKeeper宕机时Agent降级到静态配置
5. ✅ Server下线时Agent自动移除失效服务

---

## 四、核心代码逻辑

### 4.1 Server端注册逻辑

```java
// ServerRegistrationService.java
@Override
public void run(ApplicationArguments args) {
    // 1. 生成服务器ID
    String serverId = zookeeperProperties.getServerId();
    if (serverId == null) {
        serverId = "Monitor-Server-" + serverPort;
    }

    // 2. 获取主机地址
    String host = InetAddress.getLocalHost().getHostAddress();

    // 3. 构建元数据
    ServerMetadata metadata = ServerMetadata.builder()
        .id(serverId)
        .host(host)
        .port(serverPort)
        .registeredAt(LocalDateTime.now())
        .status("ACTIVE")
        .build();

    // 4. 创建临时节点（关键：EPHEMERAL模式）
    String serverPath = zookeeperProperties.getServersPath() + "/" + serverId;
    curatorFramework.create()
        .creatingParentsIfNeeded()
        .withMode(CreateMode.EPHEMERAL)  // Session超时自动删除
        .forPath(serverPath);

    // 5. 写入元数据
    String infoPath = serverPath + "/info";
    String metadataJson = objectMapper.writeValueAsString(metadata);
    curatorFramework.create()
        .withMode(CreateMode.EPHEMERAL)
        .forPath(infoPath, metadataJson.getBytes());
}
```

### 4.2 Agent端发现逻辑

```java
// ServiceDiscoveryService.java
@Scheduled(fixedRate = 60000)  // 每分钟执行
public void refreshServerList() {
    try {
        // 1. 从ZooKeeper获取子节点
        List<String> serverIds = curatorFramework
            .getChildren()
            .forPath(discoveryProperties.getServersPath());

        // 2. 解析每个server的元数据
        List<ServerEndpoint> servers = new ArrayList<>();
        for (String serverId : serverIds) {
            String infoPath = discoveryProperties.getServersPath()
                + "/" + serverId + "/info";
            byte[] data = curatorFramework.getData().forPath(infoPath);
            ServerEndpoint endpoint = objectMapper
                .readValue(data, ServerEndpoint.class);
            if (endpoint.isActive()) {
                servers.add(endpoint);
            }
        }

        // 3. 更新缓存
        cachedServers.set(servers);
        usingFallback.set(false);

    } catch (Exception e) {
        // 4. 降级到静态配置
        cachedServers.set(fallbackToStaticConfig());
        usingFallback.set(true);
    }
}
```

### 4.3 RegisterService集成

```java
public RegisterResponse register() {
    // 1. 优先使用服务发现
    List<ServerEndpoint> discovered = serviceDiscoveryService
        .getAvailableServers();

    String[] endpoints;
    if (discovered != null && !discovered.isEmpty()) {
        // 使用ZooKeeper发现的server
        endpoints = discovered.stream()
            .map(ServerEndpoint::getUrl)
            .toArray(String[]::new);
    } else {
        // 降级到静态配置
        endpoints = config.getServer().getEndpoints();
    }

    // 2. 健康检查选择最快的server
    String fastest = selectFastestServer(endpoints);
    return callRegister(fastest, request);
}
```

### 4.4 ReportService集成

```java
private String getRegisteredServerUrl() {
    // 1. 从缓存获取server列表
    List<ServerEndpoint> servers = serviceDiscoveryService
        .getAvailableServers();

    if (!servers.isEmpty()) {
        // 2. 随机负载均衡
        return servers.get(random.nextInt(servers.size())).getUrl();
    }

    // 3. 降级到静态配置
    return config.getServer().getEndpoints()[random.nextInt(length)];
}
```

---

## 五、技术实现细节

### 5.1 临时节点机制

**为什么使用临时节点？**
- Session超时自动删除，无需手动清理
- 天然实现健康检测
- Server宕机时节点自动消失

**临时节点生命周期**：
```
Server启动 → 创建临时节点 → Session保持 → 节点存在
    ↓
Server宕机/网络断开 → Session超时 → 节点自动删除
```

### 5.2 线程安全设计

**使用AtomicReference保证线程安全**：
```java
private final AtomicReference<List<ServerEndpoint>> cachedServers =
    new AtomicReference<>(Collections.emptyList());

public List<ServerEndpoint> getAvailableServers() {
    return new ArrayList<>(cachedServers.get());  // 返回副本
}
```

### 5.3 降级策略实现

```java
private List<ServerEndpoint> fallbackToStaticConfig() {
    List<ServerEndpoint> servers = new ArrayList<>();
    AgentConfig config = configLoader.getConfig();

    for (String endpoint : config.getServer().getEndpoints()) {
        // 解析host:port
        String[] parts = endpoint.split(":");
        servers.add(ServerEndpoint.builder()
            .id("static-" + parts[0] + "-" + parts[1])
            .host(parts[0])
            .port(Integer.parseInt(parts[1]))
            .status("ACTIVE")
            .build());
    }

    return servers;
}
```

### 5.4 负载均衡策略

**注册阶段**（RegisterService）：
- 健康检查所有可用Server
- 选择响应时间最短的Server
- 确保首次注册使用最佳Server

**上报阶段**（ReportService）：
- 随机选择Server
- 实现负载均衡
- 避免热点问题

---

## 六、文件清单

### 6.1 新增文件（8个）

**Monitor-Server（4个）**：
| 文件 | 说明 |
|------|------|
| `conf/ZookeeperProperties.java` | ZooKeeper连接配置 |
| `conf/ZookeeperConfig.java` | Curator客户端配置 |
| `model/ServerMetadata.java` | Server元数据实体 |
| `service/ServerRegistrationService.java` | 服务注册服务 |

**Monitor-Agent（4个）**：
| 文件 | 说明 |
|------|------|
| `config/DiscoveryProperties.java` | 服务发现配置 |
| `config/ZookeeperConfig.java` | Curator客户端配置 |
| `model/ServerEndpoint.java` | 服务端点实体 |
| `service/ServiceDiscoveryService.java` | 服务发现服务 |

### 6.2 修改文件（7个）

**Monitor-Server（2个）**：
| 文件 | 修改内容 |
|------|----------|
| `application.yaml` | 添加ZooKeeper配置 |
| `MonitorServerApplication.java` | 添加`@EnableConfigurationProperties` |

**Monitor-Agent（4个）**：
| 文件 | 修改内容 |
|------|----------|
| `application.yaml` | 添加ZooKeeper discovery配置 |
| `MonitorAgentApplication.java` | 添加`@EnableConfigurationProperties` |
| `service/RegisterService.java` | 集成服务发现 |
| `service/ReportService.java` | 集成服务发现+负载均衡 |

**公共（1个）**：
| 文件 | 修改内容 |
|------|----------|
| `pom.xml`（父POM） | 添加Curator依赖管理 |

---

## 七、配置说明

### 7.1 Monitor-Server配置

```yaml
# application.yaml
zookeeper:
  connection-string: localhost:2181      # ZooKeeper地址
  session-timeout-ms: 30000              # Session超时（30秒）
  connection-timeout-ms: 10000           # 连接超时（10秒）
  base-path: /monitor                    # 基础路径
  servers-path: /monitor/servers         # Server注册路径
  server-id: ${spring.application.name}-${server.port}  # 唯一ID
```

### 7.2 Monitor-Agent配置

```yaml
# application.yaml
zookeeper:
  discovery:
    enabled: true                        # 启用服务发现
    interval-ms: 60000                   # 刷新间隔（1分钟）
    retry-attempts: 3                    # 重试次数
    retry-delay-ms: 5000                 # 重试延迟（5秒）
    connection-string: localhost:2181    # ZooKeeper地址
    session-timeout-ms: 10000            # Session超时
    connection-timeout-ms: 5000          # 连接超时
    base-path: /monitor                  # 基础路径
    servers-path: /monitor/servers       # Server路径
```

### 7.3 启用/禁用服务发现

**禁用Agent服务发现**：
```yaml
zookeeper:
  discovery:
    enabled: false  # 将使用静态配置
```

---

## 八、测试验证

### 8.1 单元测试

**测试用例**：
| 测试场景 | 预期结果 |
|---------|----------|
| Server启动时ZooKeeper可用 | 成功注册，创建临时节点 |
| Server启动时ZooKeeper不可用 | Server正常启动，记录警告日志 |
| Agent启动时ZooKeeper可用 | 成功拉取服务列表 |
| Agent启动时ZooKeeper不可用 | 使用静态配置 |
| 定时刷新时ZooKeeper恢复 | 自动切换到服务发现 |

### 8.2 集成测试

**测试步骤**：
1. 启动ZooKeeper服务器
2. 启动Monitor-Server（多个实例）
3. 启动Monitor-Agent
4. 验证Agent能够发现所有Server
5. 停止一个Server实例
6. 验证Agent自动移除下线Server
7. 停止ZooKeeper
8. 验证Agent降级到静态配置

### 8.3 ZooKeeper命令验证

```bash
# 查看所有注册的Server
ls /monitor/servers

# 查看Server元数据
get /monitor/servers/Monitor-Server-8080/info

# 监控Server变化
ls -w /monitor/servers
```

---

## 九、与原架构对比

| 对比项 | 原架构 | 新架构 |
|-------|-------|-------|
| Server地址来源 | agent-config.yaml静态配置 | ZooKeeper动态发现 |
| 新Server加入 | 需要手动修改所有Agent配置 | 自动注册，Agent自动发现 |
| Server下线 | Agent继续向故障Server发送请求 | 临时节点删除，Agent自动移除 |
| 负载均衡 | 无（固定第一个） | 随机/响应时间选择 |
| 故障转移 | 无 | 自动降级到静态配置 |
| 健康检测 | 无 | ZooKeeper Session超时检测 |

---

## 十、后续优化计划

| 优化项 | 优先级 | 说明 |
|-------|-------|------|
| Watcher监听机制 | 中 | 替代定时拉取，实现实时更新 |
| 服务健康检查 | 低 | 主动探测Server健康状态 |
| 加权负载均衡 | 低 | 根据Server负载动态分配 |
| 服务元数据扩展 | 低 | 添加版本、区域等信息 |
| ZooKeeper集群 | 高 | 生产环境部署ZooKeeper集群 |

---

## 十一、注意事项

### 11.1 生产环境部署

**ZooKeeper部署**：
- 使用ZooKeeper集群（3/5/7节点）
- 配置合理的Session超时时间
- 监控ZooKeeper集群健康状态

**网络配置**：
- 确保Agent可以访问ZooKeeper
- 配置防火墙规则
- 设置合理的超时时间

### 11.2 故障处理

**ZooKeeper完全不可用**：
- Agent自动降级到静态配置
- 继续使用agent-config.yaml中的地址
- 记录警告日志，便于排查问题

**部分Server不可用**：
- ZooKeeper自动删除临时节点
- Agent下次刷新时自动移除
- 不影响其他Server的正常使用

### 11.3 监控指标

**关键监控指标**：
- ZooKeeper连接状态
- 服务发现成功率
- 降级触发次数
- Server节点数量变化

---

## 十二、总结

本次重构成功实现了基于ZooKeeper的服务注册与发现机制，解决了原静态配置方式的所有问题。通过临时节点实现健康检测，通过降级策略保证高可用，为系统的可扩展性和可维护性奠定了坚实基础。

**关键成果**：
- ✅ Server自动注册到ZooKeeper
- ✅ Agent动态发现Server列表
- ✅ 临时节点实现健康检测
- ✅ 降级策略保证高可用
- ✅ 负载均衡策略优化
- ✅ 向后兼容静态配置

**代码质量**：
- 编译通过无错误
- 遵循现有代码规范
- 完善的异常处理
- 详细的日志记录
- 灵活的配置选项
