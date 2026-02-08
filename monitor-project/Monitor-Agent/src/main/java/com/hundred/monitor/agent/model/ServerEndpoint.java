package com.hundred.monitor.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Server endpoint discovered from Zookeeper
 * Represents a Monitor-Server instance available for registration and reporting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerEndpoint {

    /**
     * Unique identifier for this server instance
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
    @JsonProperty("registeredAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime registeredAt;

    /**
     * Current status of the server
     */
    private String status;

    /**
     * Get the base URL for this server
     */
    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }

    /**
     * Check if server is active
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return String.format("ServerEndpoint{id=%s, url=%s, status=%s}", id, getUrl(), status);
    }
}
