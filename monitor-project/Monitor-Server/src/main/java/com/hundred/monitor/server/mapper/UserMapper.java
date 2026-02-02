package com.hundred.monitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hundred.monitor.server.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
