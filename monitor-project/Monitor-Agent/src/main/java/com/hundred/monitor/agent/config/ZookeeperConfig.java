package com.hundred.monitor.agent.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Zookeeper Curator client configuration for service discovery
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zookeeper.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ZookeeperConfig {

    private final DiscoveryProperties discoveryProperties;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        log.info("Initializing CuratorFramework client for Zookeeper service discovery at: {}",
                discoveryProperties.getConnectionString());

        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                1000,  // base sleep time (ms)
                3      // max retries
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(discoveryProperties.getConnectionString())
                .sessionTimeoutMs(discoveryProperties.getSessionTimeoutMs())
                .connectionTimeoutMs(discoveryProperties.getConnectionTimeoutMs())
                .retryPolicy(retryPolicy)
                .namespace("")
                .build();

        client.start();

        try {
            // Block until connection is established or timeout
            client.blockUntilConnected(
                    discoveryProperties.getConnectionTimeoutMs(),
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
            log.info("Successfully connected to Zookeeper for service discovery");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for Zookeeper connection", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        }

        return client;
    }
}
