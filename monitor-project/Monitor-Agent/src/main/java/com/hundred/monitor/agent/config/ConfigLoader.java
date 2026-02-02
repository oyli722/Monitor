package com.hundred.monitor.agent.config;

import com.hundred.monitor.agent.model.entity.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 配置文件加载器
 * 使用SnakeYAML解析agent-config.yaml
 */
@Component
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CONFIG_FILE = "agent-config.yaml";

    @Value("${config.path:}")
    private String configPath;

    private AgentConfig config;

    /**
     * 加载配置文件
     */
    public AgentConfig load() {
        String filePath = getConfigFilePath();
        File file = new File(filePath);

        if (!file.exists()) {
            log.warn("配置文件不存在: {}, 将使用默认配置", filePath);
            config = new AgentConfig();
            return config;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            config = yaml.loadAs(fis, AgentConfig.class);
            log.info("配置文件加载成功: {}", filePath);
            return config;
        } catch (IOException e) {
            log.error("配置文件加载失败: {}", filePath, e);
            config = new AgentConfig();
            return config;
        }
    }

    /**
     * 保存配置文件
     */
    public void save(AgentConfig config) {
        String filePath = getConfigFilePath();
        File file = new File(filePath);

        try (FileWriter writer = new FileWriter(file)) {
            Yaml yaml = new Yaml();
            yaml.dump(config, writer);
            log.info("配置文件保存成功: {}", filePath);
            this.config = config;
        } catch (IOException e) {
            log.error("配置文件保存失败: {}", filePath, e);
        }
    }

    /**
     * 获取配置对象
     */
    public AgentConfig getConfig() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    /**
     * 获取配置文件路径
     */
    private String getConfigFilePath() {
        if (configPath != null && !configPath.isEmpty()) {
            return configPath;
        }
        return CONFIG_FILE;
    }
}
