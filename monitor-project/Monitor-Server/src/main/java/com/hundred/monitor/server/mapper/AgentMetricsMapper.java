package com.hundred.monitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hundred.monitor.server.model.entity.AgentMetrics;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent监控数据Mapper
 */
@Mapper
public interface AgentMetricsMapper extends BaseMapper<AgentMetrics> {
}
