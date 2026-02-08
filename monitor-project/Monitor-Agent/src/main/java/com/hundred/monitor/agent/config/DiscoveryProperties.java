package com.hundred.monitor.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Service discovery configuration properties for Zookeeper
 */
@Data
@Component
@ConfigurationProperties(prefix = "zookeeper.discovery")
public class DiscoveryProperties {

    /**
     * Enable/disable service discovery from Zookeeper
     * When disabled, agent will use static configuration from agent-config.yaml
     */
    private boolean enabled = true;

    /**
     * Interval in milliseconds for refreshing server list from Zookeeper
     * Default: 60000ms (1 minute)
     */
    private long intervalMs = 60000;

    /**
     * Maximum retry attempts when Zookeeper connection fails
     */
    private int retryAttempts = 3;

    /**
     * Delay between retries in milliseconds
     */
    private long retryDelayMs = 5000;

    /**
     * Zookeeper connection string
     */
    private String connectionString = "localhost:2181";

    /**
     * Session timeout in milliseconds
     */
    private int sessionTimeoutMs = 10000;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeoutMs = 5000;

    /**
     * Base path in Zookeeper for monitor services
     */
    private String basePath = "/monitor";

    /**
     * Path where servers are registered
     */
    private String serversPath = "/monitor/servers";
}
