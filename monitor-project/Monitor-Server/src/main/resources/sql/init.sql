-- 监控系统数据库初始化脚本
create database if not exists `monitor`;
use `monitor`;

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS `user`;
CREATE TABLE user (
    user_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    user_role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：admin/user等',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱，唯一',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(50) COMMENT '昵称',
    phone_number VARCHAR(20) COMMENT '电话号码',
    sex ENUM('male', 'female', 'other') DEFAULT 'male' COMMENT '性别：男性/女性/其他，默认男性',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==================== Agent信息表 ====================
DROP TABLE IF EXISTS `agent`;
CREATE TABLE agent (
    agent_id VARCHAR(64) PRIMARY KEY COMMENT 'Agent ID，主键',
    agent_name VARCHAR(100) COMMENT 'Agent名称',
    hostname VARCHAR(100) COMMENT '主机名',
    ip VARCHAR(50) COMMENT 'IP地址',

    cpu_model VARCHAR(200) COMMENT 'CPU型号',
    cpu_cores INT COMMENT 'CPU核心数',
    memory_gb BIGINT COMMENT '内存总量(GB)',

    gpu_info JSON COMMENT 'GPU信息(JSON数组)',
    network_interfaces JSON COMMENT '网络接口列表(JSON数组)',

    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_ip (ip),
    INDEX idx_hostname (hostname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent信息表';

-- ==================== Agent监控数据表 ====================
DROP TABLE IF EXISTS `agent_metrics`;
CREATE TABLE agent_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    agent_id VARCHAR(64) NOT NULL COMMENT 'Agent ID',

    cpu_percent DECIMAL(5,2) COMMENT 'CPU使用率(%)',
    memory_percent DECIMAL(5,2) COMMENT '内存使用率(%)',
    disk_usages JSON COMMENT '磁盘使用信息(JSON数组)',

    network_up_mbps DECIMAL(10,2) COMMENT '网络上行速率(Mbps)',
    network_down_mbps DECIMAL(10,2) COMMENT '网络下行速率(Mbps)',

    ssh_running TINYINT(1) COMMENT 'SSH进程是否运行',
    ssh_port_listening TINYINT(1) COMMENT 'SSH端口是否监听',
    ssh_port INT COMMENT 'SSH端口',

    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间戳',

    INDEX idx_agent_timestamp (agent_id, timestamp),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent监控数据表';

-- ==================== SSH凭证表 ====================
DROP TABLE IF EXISTS `ssh_credential`;
CREATE TABLE ssh_credential (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    agent_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Agent ID',
    username VARCHAR(100) NOT NULL COMMENT 'SSH用户名',
    password VARCHAR(500) NOT NULL COMMENT 'SSH密码（加密存储）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSH凭证表';



