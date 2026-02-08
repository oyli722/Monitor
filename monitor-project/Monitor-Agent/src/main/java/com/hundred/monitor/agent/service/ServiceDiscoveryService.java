package com.hundred.monitor.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.agent.config.ConfigLoader;
import com.hundred.monitor.agent.config.DiscoveryProperties;
import com.hundred.monitor.agent.model.ServerEndpoint;
import com.hundred.monitor.agent.model.entity.AgentConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service discovery service for Monitor-Agent
 * Periodically pulls server list from Zookeeper and caches them locally
 * Falls back to static configuration when Zookeeper is unavailable
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zookeeper.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceDiscoveryService {

    private final CuratorFramework curatorFramework;
    private final DiscoveryProperties discoveryProperties;
    private final ConfigLoader configLoader;
    private final ObjectMapper objectMapper;

    // Thread-safe cache for discovered servers
    private final AtomicReference<List<ServerEndpoint>> cachedServers =
            new AtomicReference<>(Collections.emptyList());

    // Flag to track if we're using fallback (static config)
    private final AtomicReference<Boolean> usingFallback = new AtomicReference<>(false);

    /**
     * Initialize service discovery on startup
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing service discovery from Zookeeper...");
        refreshServerList();
    }

    /**
     * Periodically refresh server list from Zookeeper
     * Default interval: 1 minute (configurable via zookeeper.discovery.interval-ms)
     */
    @Scheduled(fixedRateString = "${zookeeper.discovery.interval-ms:60000}")
    public void refreshServerList() {
        try {
            log.debug("Refreshing server list from Zookeeper...");
            List<ServerEndpoint> servers = discoverFromZookeeper();

            if (servers.isEmpty()) {
                log.warn("No servers discovered from Zookeeper, using fallback configuration");
                servers = fallbackToStaticConfig();
                usingFallback.set(true);
            } else {
                log.info("Discovered {} servers from Zookeeper: {}",
                        servers.size(),
                        servers.stream().map(ServerEndpoint::getUrl).collect(Collectors.joining(", ")));
                usingFallback.set(false);
            }

            cachedServers.set(servers);

        } catch (Exception e) {
            log.error("Failed to refresh server list from Zookeeper, using fallback configuration", e);
            cachedServers.set(fallbackToStaticConfig());
            usingFallback.set(true);
        }
    }

    /**
     * Discover servers from Zookeeper
     *
     * New node structure:
     * /monitor/servers/Monitor-Server-8080 (PERSISTENT)
     *   ├── info (PERSISTENT) - server metadata
     *   └── status (EPHEMERAL) - health indicator
     *
     * A server is considered active only if both 'info' and 'status' nodes exist.
     */
    private List<ServerEndpoint> discoverFromZookeeper() throws Exception {
        // Ensure we're connected
        if (!curatorFramework.getZookeeperClient().isConnected()) {
            throw new IllegalStateException("Not connected to Zookeeper");
        }

        // Get server nodes
        String serversPath = discoveryProperties.getServersPath();
        List<String> serverIds;

        try {
            serverIds = curatorFramework.getChildren().forPath(serversPath);
        } catch (KeeperException.NoNodeException e) {
            log.warn("Server path does not exist in Zookeeper: {}", serversPath);
            return Collections.emptyList();
        }

        List<ServerEndpoint> servers = new ArrayList<>();

        for (String serverId : serverIds) {
            try {
                String serverPath = serversPath + "/" + serverId;
                String infoPath = serverPath + "/info";
                String statusPath = serverPath + "/status";

                // Check if status node exists (ephemeral - indicates server is alive)
                try {
                    curatorFramework.checkExists().forPath(statusPath);
                } catch (Exception e) {
                    log.debug("Server {} is offline (no status node)", serverId);
                    continue; // Skip this server as it's not alive
                }

                // Read metadata from info node
                byte[] data = curatorFramework.getData().forPath(infoPath);
                String json = new String(data);
                ServerEndpoint endpoint = objectMapper.readValue(json, ServerEndpoint.class);

                // Verify the server is active
                if (endpoint.isActive()) {
                    servers.add(endpoint);
                    log.debug("Discovered active server: {} ({})", serverId, endpoint.getUrl());
                } else {
                    log.debug("Skipping inactive server: {}", serverId);
                }

            } catch (KeeperException.NoNodeException e) {
                log.debug("Server info or status node missing for: {}", serverId);
            } catch (Exception e) {
                log.warn("Failed to parse server info for {}: {}", serverId, e.getMessage());
            }
        }

        return servers;
    }

    /**
     * Fallback to static configuration from agent-config.yaml
     */
    private List<ServerEndpoint> fallbackToStaticConfig() {
        log.info("Using fallback to static configuration from agent-config.yaml");
        List<ServerEndpoint> servers = new ArrayList<>();

        AgentConfig config = configLoader.getConfig();
        if (config.getServer() != null && config.getServer().getEndpoints() != null) {
            for (String endpoint : config.getServer().getEndpoints()) {
                try {
                    // Parse endpoint (may contain port)
                    String host;
                    int port = 8080; // default port

                    if (endpoint.contains(":")) {
                        String[] parts = endpoint.split(":");
                        host = parts[0];
                        port = Integer.parseInt(parts[1]);
                    } else {
                        host = endpoint;
                    }

                    servers.add(ServerEndpoint.builder()
                            .id("static-" + host + "-" + port)
                            .host(host)
                            .port(port)
                            .status("ACTIVE")
                            .build());

                } catch (Exception e) {
                    log.warn("Failed to parse static endpoint: {}", endpoint);
                }
            }
        }

        return servers;
    }

    /**
     * Get list of available servers
     */
    public List<ServerEndpoint> getAvailableServers() {
        List<ServerEndpoint> servers = cachedServers.get();
        if (usingFallback.get()) {
            log.debug("Returning servers from fallback configuration");
        }
        return new ArrayList<>(servers); // Return a copy to prevent external modification
    }

    /**
     * Get a single server URL (simple load balancing)
     * Uses round-robin strategy
     */
    public String getServerUrl() {
        List<ServerEndpoint> servers = cachedServers.get();
        if (servers.isEmpty()) {
            return null;
        }

        // Simple round-robin using system time
        int index = (int) ((System.currentTimeMillis() / 1000) % servers.size());
        return servers.get(index).getUrl();
    }

    /**
     * Check if currently using fallback configuration
     */
    public boolean isUsingFallback() {
        return usingFallback.get();
    }

    /**
     * Manually trigger server list refresh
     */
    public void forceRefresh() {
        log.info("Force refreshing server list...");
        refreshServerList();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Service discovery service shutting down...");
    }
}
