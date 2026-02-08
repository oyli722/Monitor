package com.hundred.monitor.server.conf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Zookeeper Curator client configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ZookeeperConfig {

    private final ZookeeperProperties zookeeperProperties;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        log.info("Initializing CuratorFramework client for Zookeeper at: {}",
                zookeeperProperties.getConnectionString());

        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                1000,  // base sleep time (ms)
                3      // max retries
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperProperties.getConnectionString())
                .sessionTimeoutMs(zookeeperProperties.getSessionTimeoutMs())
                .connectionTimeoutMs(zookeeperProperties.getConnectionTimeoutMs())
                .retryPolicy(retryPolicy)
                .namespace("")
                .build();

        client.start();

        try {
            // Block until connection is established or timeout
            client.blockUntilConnected(
                    zookeeperProperties.getConnectionTimeoutMs(),
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
            log.info("Successfully connected to Zookeeper at: {}",
                    zookeeperProperties.getConnectionString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for Zookeeper connection", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        }

        return client;
    }
}
