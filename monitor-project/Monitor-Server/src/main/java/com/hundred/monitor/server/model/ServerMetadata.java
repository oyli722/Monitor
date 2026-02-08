package com.hundred.monitor.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Server metadata stored in Zookeeper
 * Represents information about a Monitor-Server instance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerMetadata {

    /**
     * Unique identifier for this server instance
     * Format: Monitor-Server-{port}
     */
    private String id;

    /**
     * Host address where the server is running
     */
    private String host;

    /**
     * Port number where the server is listening
     */
    private int port;

    /**
     * Timestamp when this server registered with Zookeeper
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime registeredAt;

    /**
     * Current status of the server
     */
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Get the base URL for this server
     */
    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }
}
