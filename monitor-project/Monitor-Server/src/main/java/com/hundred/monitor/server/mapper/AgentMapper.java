package com.hundred.monitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hundred.monitor.server.model.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent表Mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
