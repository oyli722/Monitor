package com.hundred.monitor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.conf.ZookeeperProperties;
import com.hundred.monitor.server.model.ServerMetadata;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for registering Monitor-Server with Zookeeper on startup
 * Creates ephemeral nodes for automatic health detection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerRegistrationService implements ApplicationRunner {

    private final CuratorFramework curatorFramework;
    private final ZookeeperProperties zookeeperProperties;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private int serverPort;

    private String registrationPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // Ensure base path exists
            ensurePathExists(zookeeperProperties.getBasePath());

            // Ensure servers path exists
            ensurePathExists(zookeeperProperties.getServersPath());

            // Generate or use provided server ID
            String serverId = zookeeperProperties.getServerId();
            if (serverId == null || serverId.isEmpty()) {
                serverId = "Monitor-Server-" + serverPort;
            }

            // Get host address
            String host = InetAddress.getLocalHost().getHostAddress();

            // Create server metadata
            ServerMetadata metadata = ServerMetadata.builder()
                    .id(serverId)
                    .host(host)
                    .port(serverPort)
                    .registeredAt(LocalDateTime.now())
                    .status("ACTIVE")
                    .build();

            // Create node structure for this server
            // Note: Ephemeral nodes cannot have children, so we use:
            //   - Parent node: PERSISTENT (holds server metadata)
            //   - Status node: EPHEMERAL (indicates server is alive)
            String serverPath = zookeeperProperties.getServersPath() + "/" + serverId;
            String infoPath = serverPath + "/info";
            String statusPath = serverPath + "/status";

            // Create parent PERSISTENT node for server metadata
            try {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(serverPath);
                log.info("Created persistent node: {}", serverPath);
            } catch (KeeperException.NodeExistsException e) {
                log.debug("Server node already exists: {}", serverPath);
                // Node exists, continue to update children
            }

            // Write metadata to info child node (PERSISTENT)
            String metadataJson = objectMapper.writeValueAsString(metadata);
            try {
                curatorFramework.create()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(infoPath, metadataJson.getBytes());
                log.info("Created metadata node: {}", infoPath);
            } catch (KeeperException.NodeExistsException e) {
                // Update existing metadata
                curatorFramework.setData().forPath(infoPath, metadataJson.getBytes());
                log.info("Updated metadata node: {}", infoPath);
            }

            // Create status EPHEMERAL node for health detection
            // This node will be automatically deleted when session expires
            try {
                String statusData = LocalDateTime.now().toString();
                curatorFramework.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(statusPath, statusData.getBytes());
                log.info("Created ephemeral status node: {}", statusPath);
            } catch (KeeperException.NodeExistsException e) {
                log.debug("Status node already exists: {}", statusPath);
                // Update status
                String statusData = LocalDateTime.now().toString();
                curatorFramework.setData().forPath(statusPath, statusData.getBytes());
            }

            registrationPath = serverPath;
            log.info("Successfully registered server with Zookeeper: id={}, host={}, port={}, path={}",
                    serverId, host, serverPort, serverPath);
            log.info("Server metadata: {}", metadataJson);

        } catch (Exception e) {
            log.error("Failed to register server with Zookeeper", e);
            // Don't throw exception to allow server to start even if ZK registration fails
            log.warn("Server will continue to run despite Zookeeper registration failure");
        }
    }

    @PreDestroy
    public void cleanup() {
        // Clean up server registration
        // Note: The status ephemeral node will be automatically deleted when session closes
        // We only need to clean up the persistent parent node if needed
        if (registrationPath != null && curatorFramework != null) {
            try {
                if (curatorFramework.getZookeeperClient().isConnected()) {
                    // Delete the entire server node (including all children)
                    curatorFramework.delete().deletingChildrenIfNeeded().forPath(registrationPath);
                    log.info("Cleaned up Zookeeper registration: {}", registrationPath);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup Zookeeper registration: {}", registrationPath, e);
                // This is expected if session already expired or node was already deleted
            }
        }
    }

    private void ensurePathExists(String path) throws Exception {
        try {
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath(path);
            log.info("Created Zookeeper path: {}", path);
        } catch (KeeperException.NodeExistsException e) {
            log.debug("Zookeeper path already exists: {}", path);
            // Path already exists, which is fine
        }
    }
}
