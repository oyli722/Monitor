# 初始化代码
create database if not exists `monitor`;
use `monitor`;
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