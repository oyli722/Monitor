package com.hundred.monitor.server.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Zookeeper connection configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperProperties {

    /**
     * Zookeeper connection string (e.g., localhost:2181)
     */
    private String connectionString = "localhost:2181";

    /**
     * Session timeout in milliseconds
     */
    private int sessionTimeoutMs = 30000;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeoutMs = 10000;

    /**
     * Base path for all monitor nodes in Zookeeper
     */
    private String basePath = "/monitor";

    /**
     * Path where servers register themselves
     */
    private String serversPath = "/monitor/servers";

    /**
     * Unique identifier for this server instance
     * Defaults to application name with port
     */
    private String serverId;
}
