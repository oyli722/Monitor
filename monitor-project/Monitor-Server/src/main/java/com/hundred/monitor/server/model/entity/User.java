package com.hundred.monitor.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import lombok.Data;

@Data
@TableName("user")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long userId;

    // 用户角色
    private String userRole = "user";

    // 用户名
    private String username;

    // 邮箱
    private String email;

    // 密码
    private String password;

    // 昵称
    private String nickname;

    // 电话号码
    private String phoneNumber;

    // 性别
    private String sex = "male";
}