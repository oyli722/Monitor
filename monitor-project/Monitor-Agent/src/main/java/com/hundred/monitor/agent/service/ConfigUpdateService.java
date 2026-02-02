package com.hundred.monitor.agent.service;

import com.hundred.monitor.agent.config.ConfigLoader;
import com.hundred.monitor.agent.model.entity.AgentConfig;
import com.hundred.monitor.agent.model.response.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 配置更新服务
 * 负责接收并应用服务端下发的配置更新
 */
@Service
public class ConfigUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ConfigUpdateService.class);

    @Autowired
    private ConfigLoader configLoader;

    /**
     * 更新配置
     */
    public CommonResponse updateConfig(AgentConfig configUpdates) {
        try {
            // 读取当前配置
            AgentConfig currentConfig = configLoader.getConfig();

            // 合并配置
            AgentConfig mergedConfig = mergeConfig(currentConfig, configUpdates);

            // 保存到配置文件
            configLoader.save(mergedConfig);

            log.info("配置更新成功");
            return CommonResponse.success("配置已更新");

        } catch (Exception e) {
            log.error("配置更新失败: {}", e.getMessage(), e);
            return CommonResponse.error("配置更新失败: " + e.getMessage());
        }
    }

    /**
     * 合并配置
     */
    private AgentConfig mergeConfig(AgentConfig current, AgentConfig updates) {
        AgentConfig merged = new AgentConfig();

        // 合并服务端配置
        if (updates.getServer() != null && updates.getServer().getEndpoints() != null
                && updates.getServer().getEndpoints().length > 0) {
            merged.setServer(updates.getServer());
        } else if (current.getServer() != null) {
            merged.setServer(current.getServer());
        }

        // 合并Agent信息
        if (updates.getAgent() != null) {
            merged.setAgent(updates.getAgent());
        } else if (current.getAgent() != null) {
            merged.setAgent(current.getAgent());
        }

        // 合并认证配置
        if (updates.getAuth() != null) {
            merged.setAuth(updates.getAuth());
        } else if (current.getAuth() != null) {
            merged.setAuth(current.getAuth());
        }

        // 合并上报配置
        if (updates.getReporting() != null) {
            merged.setReporting(updates.getReporting());
        } else if (current.getReporting() != null) {
            merged.setReporting(current.getReporting());
        }

        return merged;
    }
}
