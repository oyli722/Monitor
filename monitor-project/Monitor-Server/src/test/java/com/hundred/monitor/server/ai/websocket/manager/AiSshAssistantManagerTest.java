package com.hundred.monitor.server.ai.websocket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.ai.websocket.dto.WsChatMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiSshAssistantManager Test
 * WebSocket session manager and message sending functionality tests
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI Assistant WebSocket Session Manager Test")
class AiSshAssistantManagerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiSshAssistantManager manager;

    private WebSocketSession mockSession;

    @BeforeEach
    void setUp() throws IOException {
        mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-ws-session-id");
        when(mockSession.isOpen()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        manager.clearAllSessions();
    }

    // ==================== Session Management Tests ====================

    @Test
    @DisplayName("Add session should store correctly")
    void testAddSession_ShouldStoreSession() {
        String aiSessionId = "test-ai-session";
        manager.addSession(aiSessionId, mockSession);

        assertTrue(manager.hasSession(aiSessionId));
        assertSame(mockSession, manager.getSession(aiSessionId));
    }

    @Test
    @DisplayName("Add session should increase active count")
    void testAddSession_ShouldIncreaseActiveCount() {
        int initialCount = manager.getActiveSessionCount();
        manager.addSession("session-1", mockSession);

        assertEquals(initialCount + 1, manager.getActiveSessionCount());
    }

    @Test
    @DisplayName("Add multiple sessions should store all")
    void testAddMultipleSessions_ShouldStoreAll() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        manager.addSession("ai-1", mockSession);
        manager.addSession("ai-2", session2);

        assertEquals(2, manager.getActiveSessionCount());
        assertTrue(manager.hasSession("ai-1"));
        assertTrue(manager.hasSession("ai-2"));
    }

    @Test
    @DisplayName("Remove session should delete correctly")
    void testRemoveSession_ShouldRemoveSession() {
        String aiSessionId = "test-ai-session";
        manager.addSession(aiSessionId, mockSession);
        manager.removeSession(aiSessionId);

        assertFalse(manager.hasSession(aiSessionId));
        assertNull(manager.getSession(aiSessionId));
    }

    @Test
    @DisplayName("Remove non-existent session should not throw")
    void testRemoveNonExistentSession_ShouldNotThrow() {
        assertDoesNotThrow(() -> manager.removeSession("non-existent-session"));
    }

    @Test
    @DisplayName("Get non-existent session should return null")
    void testGetNonExistentSession_ShouldReturnNull() {
        WebSocketSession session = manager.getSession("non-existent-session");
        assertNull(session);
    }

    @Test
    @DisplayName("Clear all sessions should remove all")
    void testClearAllSessions_ShouldRemoveAll() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        manager.addSession("session-1", mockSession);
        manager.addSession("session-2", session2);
        manager.clearAllSessions();

        assertEquals(0, manager.getActiveSessionCount());
        assertFalse(manager.hasSession("session-1"));
        assertFalse(manager.hasSession("session-2"));
    }

    // ==================== Message Sending Tests ====================

    @Test
    @DisplayName("Send message to existing open session should succeed")
    void testSendToSession_WhenSessionExistsAndOpen_ShouldReturnTrue() throws Exception {
        String aiSessionId = "test-ai-session";
        manager.addSession(aiSessionId, mockSession);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenReturn("{\"type\":\"reply\",\"content\":\"test\"}");

        WsChatMessage message = WsChatMessage.reply("test message");
        boolean result = manager.sendToSession(aiSessionId, message);

        assertTrue(result);
        verify(mockSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Send message to non-existent session should return false")
    void testSendToSession_WhenSessionNotExists_ShouldReturnFalse() throws IOException {
        WsChatMessage message = WsChatMessage.reply("test");
        boolean result = manager.sendToSession("non-existent-session", message);

        assertFalse(result);
        verify(mockSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Send message to closed session should return false and cleanup")
    void testSendToSession_WhenSessionClosed_ShouldReturnFalseAndCleanup() throws Exception {
        String aiSessionId = "test-ai-session";
        when(mockSession.isOpen()).thenReturn(false);
        manager.addSession(aiSessionId, mockSession);

        WsChatMessage message = WsChatMessage.reply("test");
        boolean result = manager.sendToSession(aiSessionId, message);

        assertFalse(result);
        assertFalse(manager.hasSession(aiSessionId));
    }

    @Test
    @DisplayName("JSON serialization failure should return false")
    void testSendToSession_WhenJsonSerializationFails_ShouldReturnFalse() throws Exception {
        String aiSessionId = "test-ai-session";
        manager.addSession(aiSessionId, mockSession);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenThrow(new RuntimeException("JSON error"));

        WsChatMessage message = WsChatMessage.reply("test");
        boolean result = manager.sendToSession(aiSessionId, message);

        assertFalse(result);
    }

    // ==================== sendReply Shortcut Method Tests ====================

    @Test
    @DisplayName("sendReply should construct and send reply message")
    void testSendReply_ShouldSendReplyMessage() throws Exception {
        String aiSessionId = "test-ai-session";
        manager.addSession(aiSessionId, mockSession);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenReturn("{\"type\":\"reply\",\"content\":\"test reply\"}");

        String reply = "This is a test reply";
        boolean result = manager.sendReply(aiSessionId, reply);

        assertTrue(result);

        ArgumentCaptor<WsChatMessage> captor = ArgumentCaptor.forClass(WsChatMessage.class);
        verify(objectMapper).writeValueAsString(captor.capture());

        WsChatMessage capturedMessage = captor.getValue();
        assertEquals("reply", capturedMessage.getType());
        assertEquals("This is a test reply", capturedMessage.getContent());
    }

    // ==================== Broadcast Tests ====================

    @Test
    @DisplayName("Broadcast should send to all sessions")
    void testBroadcast_ShouldSendToAllSessions() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        manager.addSession("ai-1", mockSession);
        manager.addSession("ai-2", session2);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenReturn("{\"type\":\"broadcast\"}");

        WsChatMessage message = WsChatMessage.builder()
                .type("broadcast")
                .content("test broadcast")
                .build();

        int successCount = manager.broadcast(message);

        assertEquals(2, successCount);
        verify(mockSession).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Broadcast exclude should not send to excluded session")
    void testBroadcastExclude_ShouldNotSendToExcludedSession() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        manager.addSession("ai-1", mockSession);
        manager.addSession("ai-2", session2);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenReturn("{\"type\":\"broadcast\"}");

        WsChatMessage message = WsChatMessage.builder()
                .type("broadcast")
                .content("test")
                .build();

        int successCount = manager.broadcastExclude(message, "ai-1");

        assertEquals(1, successCount);
        verify(mockSession, never()).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Broadcast with partial failures should return success count")
    void testBroadcast_WithPartialFailures_ShouldReturnSuccessCount() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        WebSocketSession session3 = mock(WebSocketSession.class);
        when(session3.getId()).thenReturn("ws-session-3");
        when(session3.isOpen()).thenReturn(true);
        doThrow(new IOException("Send failed")).when(session3).sendMessage(any(TextMessage.class));
        manager.addSession("ai-1", mockSession);
        manager.addSession("ai-2", session2);
        manager.addSession("ai-3", session3);

        when(objectMapper.writeValueAsString(any(WsChatMessage.class)))
                .thenReturn("{\"type\":\"broadcast\"}");

        WsChatMessage message = WsChatMessage.builder()
                .type("broadcast")
                .content("test")
                .build();

        int successCount = manager.broadcast(message);

        assertEquals(2, successCount);
    }

    // ==================== Cleanup Tests ====================

    @Test
    @DisplayName("Cleanup closed sessions should remove them")
    void testCleanupClosedSessions_ShouldRemoveClosedSessions() {
        WebSocketSession openSession = mock(WebSocketSession.class);
        when(openSession.getId()).thenReturn("ws-open");
        when(openSession.isOpen()).thenReturn(true);

        WebSocketSession closedSession = mock(WebSocketSession.class);
        when(closedSession.getId()).thenReturn("ws-closed");
        when(closedSession.isOpen()).thenReturn(false);

        manager.addSession("ai-open", openSession);
        manager.addSession("ai-closed", closedSession);

        int cleanedCount = manager.cleanupClosedSessions();

        assertEquals(1, cleanedCount);
        assertTrue(manager.hasSession("ai-open"));
        assertFalse(manager.hasSession("ai-closed"));
    }

    @Test
    @DisplayName("Check session active status should return correct")
    void testIsSessionActive_ShouldReturnCorrectStatus() {
        String aiSessionId = "test-ai-session";

        assertFalse(manager.isSessionActive(aiSessionId));

        manager.addSession(aiSessionId, mockSession);
        assertTrue(manager.isSessionActive(aiSessionId));

        when(mockSession.isOpen()).thenReturn(false);
        assertFalse(manager.isSessionActive(aiSessionId));
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("Get statistics should return correct format")
    void testGetStatistics_ShouldReturnCorrectFormat() {
        manager.addSession("ai-1", mockSession);

        String stats = manager.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.contains("AI助手会话统计"));
        assertTrue(stats.contains("活跃数="));
        assertTrue(stats.contains("1"));
    }

    @Test
    @DisplayName("Get all session IDs should return all IDs")
    void testGetAllSessionIds_ShouldReturnAllIds() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("ws-session-2");
        when(session2.isOpen()).thenReturn(true);

        manager.addSession("ai-1", mockSession);
        manager.addSession("ai-2", session2);

        var sessionIds = manager.getAllSessionIds();

        assertEquals(2, sessionIds.size());
        assertTrue(sessionIds.contains("ai-1"));
        assertTrue(sessionIds.contains("ai-2"));
    }

    @Test
    @DisplayName("Get all sessions should return all sessions")
    void testGetAllSessions_ShouldReturnAllSessions() {
        manager.addSession("ai-1", mockSession);

        var sessions = manager.getAllSessions();

        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(mockSession));
    }
}
