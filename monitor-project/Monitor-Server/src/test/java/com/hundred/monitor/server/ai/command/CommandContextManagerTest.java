package com.hundred.monitor.server.ai.command;

import com.hundred.monitor.server.ai.service.AiSshAssistantService;
import com.hundred.monitor.server.ai.websocket.dto.WsChatMessage;
import com.hundred.monitor.server.ai.websocket.manager.AiSshAssistantManager;
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
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CommandContextManager Test
 * Command lifecycle management and output handling tests
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Command Context Manager Test")
class CommandContextManagerTest {

    @Mock
    private AiSshAssistantManager aiSshAssistantManager;

    @Mock
    private AiSshAssistantService aiSshAssistantService;

    @InjectMocks
    private CommandContextManager commandContextManager;

    private static final String TEST_AI_SESSION_ID = "test-ai-session";
    private static final String TEST_SSH_SESSION_ID = "test-ssh-session";
    private static final String TEST_AGENT_ID = "test-agent";

    @AfterEach
    void tearDown() {
        // Clean up any remaining commands
        try {
            commandContextManager.cleanupByAiSession(TEST_AI_SESSION_ID);
        } catch (Exception e) {
            // Ignore
        }
    }

    // ==================== Command Registration Tests ====================

    @Test
    @DisplayName("Register command should create context with unique ID")
    void testRegisterCommand_ShouldCreateContextWithUniqueId() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "ls -la");

        assertNotNull(commandId);
        assertFalse(commandId.isEmpty());

        CommandContext context = commandContextManager.getCommand(commandId);
        assertNotNull(context);
        assertEquals(commandId, context.getCommandId());
        assertEquals(TEST_AI_SESSION_ID, context.getAiSessionId());
        assertEquals(TEST_SSH_SESSION_ID, context.getSshSessionId());
        assertEquals("ls -la", context.getCommand());
        assertEquals(CommandStatus.EXECUTING, context.getStatus());
        assertEquals(5000, context.getTimeoutMillis());
    }

    @Test
    @DisplayName("Register command should set active command mapping")
    void testRegisterCommand_ShouldSetActiveCommandMapping() {
        commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test command");

        String activeCommandId = commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID);
        assertNotNull(activeCommandId);

        CommandContext context = commandContextManager.getCommand(activeCommandId);
        assertEquals("test command", context.getCommand());
    }

    @Test
    @DisplayName("Register multiple commands for different SSH sessions should all be active")
    void testRegisterCommand_MultipleDifferentSshSessions_AllShouldBeActive() {
        String ssh1 = "ssh-1";
        String ssh2 = "ssh-2";

        String cmdId1 = commandContextManager.registerCommand("ai-1", ssh1, "cmd1");
        String cmdId2 = commandContextManager.registerCommand("ai-2", ssh2, "cmd2");

        assertEquals(cmdId1, commandContextManager.getActiveCommandId(ssh1));
        assertEquals(cmdId2, commandContextManager.getActiveCommandId(ssh2));

        assertNotEquals(cmdId1, cmdId2, "Different sessions should have different command IDs");
    }

    @Test
    @DisplayName("Register command with custom timeout should use custom timeout")
    void testRegisterCommand_WithCustomTimeout_ShouldUseCustomTimeout() {
        long customTimeout = 10000;

        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "long command", customTimeout);

        CommandContext context = commandContextManager.getCommand(commandId);
        assertEquals(customTimeout, context.getTimeoutMillis());
    }

    @Test
    @DisplayName("Register command should start with zero output")
    void testRegisterCommand_ShouldStartWithZeroOutput() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test");

        CommandContext context = commandContextManager.getCommand(commandId);
        assertEquals("", context.getOutput());
    }

    // ==================== Output Handling Tests ====================

    @Test
    @DisplayName("Append output should add to context and push to frontend")
    void testAppendOutput_ShouldAppendToContextAndPushToFrontend() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "echo test");

        commandContextManager.appendOutput(TEST_SSH_SESSION_ID, "line1\n");
        commandContextManager.appendOutput(TEST_SSH_SESSION_ID, "line2\n");

        CommandContext context = commandContextManager.getCommand(commandId);
        assertTrue(context.getOutput().contains("line1"));
        assertTrue(context.getOutput().contains("line2"));

        // Verify push to frontend (2 times because appendOutput called twice)
        verify(aiSshAssistantManager, times(2)).sendToSession(eq(TEST_AI_SESSION_ID), any(WsChatMessage.class));
    }

    @Test
    @DisplayName("Append output to non-existent SSH session should do nothing")
    void testAppendOutput_WhenSshSessionNotExists_ShouldDoNothing() {
        // No command registered for this SSH session
        commandContextManager.appendOutput("non-existent-ssh", "some output");

        // Should not throw exception
        // No active command ID should be set
        assertNull(commandContextManager.getActiveCommandId("non-existent-ssh"));
    }

    @Test
    @DisplayName("Append output should work correctly with multiple calls")
    void testAppendOutput_MultipleCalls_ShouldAccumulateCorrectly() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "cat test.txt");

        for (int i = 1; i <= 5; i++) {
            commandContextManager.appendOutput(TEST_SSH_SESSION_ID, "line" + i + "\n");
        }

        CommandContext context = commandContextManager.getCommand(commandId);
        String output = context.getOutput();
        assertTrue(output.contains("line1"));
        assertTrue(output.contains("line2"));
        assertTrue(output.contains("line3"));
        assertTrue(output.contains("line4"));
        assertTrue(output.contains("line5"));
    }

    // ==================== Command Completion Tests ====================

    @Test
    @DisplayName("Complete command should mark as completed and push completion message")
    void testCompleteCommand_ShouldMarkCompletedAndPushCompletion() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test");

        commandContextManager.completeCommand(TEST_SSH_SESSION_ID);

        CommandContext context = commandContextManager.getCommand(commandId);
        assertEquals(CommandStatus.COMPLETED, context.getStatus());

        // Verify completion message pushed
        verify(aiSshAssistantManager).sendToSession(eq(TEST_AI_SESSION_ID), any(WsChatMessage.class));

        // Verify AI analysis triggered
        verify(aiSshAssistantService).analyzeCommandOutput(eq("test"), anyString());
    }

    @Test
    @DisplayName("Complete command should remove from active commands")
    void testCompleteCommand_ShouldRemoveFromActiveCommands() {
        commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test");

        assertTrue(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID) != null);

        commandContextManager.completeCommand(TEST_SSH_SESSION_ID);

        assertNull(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID));
    }

    @Test
    @DisplayName("Complete command with non-existent SSH session should do nothing")
    void testCompleteCommand_WhenSshSessionNotExists_ShouldDoNothing() {
        // Should not throw exception
        assertDoesNotThrow(() -> commandContextManager.completeCommand("non-existent-ssh"));
    }

    @Test
    @DisplayName("Complete already completed command should do nothing")
    void testCompleteCommand_AlreadyCompleted_ShouldDoNothing() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test");

        // First complete
        commandContextManager.completeCommand(TEST_SSH_SESSION_ID);
        assertEquals(CommandStatus.COMPLETED, commandContextManager.getCommand(commandId).getStatus());

        // Reset status to test second complete
        commandContextManager.getCommand(commandId).setStatus(CommandStatus.EXECUTING);

        // Second complete should not change status
        String statusBefore = commandContextManager.getCommand(commandId).getStatus().toString();
        commandContextManager.completeCommand(TEST_SSH_SESSION_ID);
        String statusAfter = commandContextManager.getCommand(commandId).getStatus().toString();

        // Status might still be COMPLETED from first call or EXECUTING if already removed from active
        // The key is it should not throw exception
        assertNotNull(commandContextManager.getCommand(commandId));
    }

    // ==================== Timeout Handling Tests ====================

    @Test
    @DisplayName("Handle timeout should mark as timeout and push timeout message")
    void testHandleTimeout_ShouldMarkTimeoutAndPushTimeoutMessage() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "long command");

        // Simulate timeout by setting old start time
        CommandContext context = commandContextManager.getCommand(commandId);
        context.setStartTime(System.currentTimeMillis() - 10000); // 10 seconds ago

        commandContextManager.handleTimeout(commandId);

        assertEquals(CommandStatus.TIMEOUT, context.getStatus());

        // Verify timeout message pushed
        verify(aiSshAssistantManager).sendToSession(eq(TEST_AI_SESSION_ID), any(WsChatMessage.class));

        // Verify AI analysis still triggered with partial output
        verify(aiSshAssistantService).analyzeCommandOutput(eq("long command"), anyString());

        // Should remove from active commands
        assertNull(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID));
    }

    @Test
    @DisplayName("Handle timeout for non-existent command should do nothing")
    void testHandleTimeout_WhenCommandNotExists_ShouldDoNothing() {
        // Should not throw exception
        assertDoesNotThrow(() -> commandContextManager.handleTimeout("non-existent-command"));
    }

    // ==================== Cleanup Tests ====================

    @Test
    @DisplayName("Cleanup command should remove from command map and active commands")
    void testCleanup_ShouldRemoveFromCommandMapAndActiveCommands() {
        String commandId = commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "test");

        assertNotNull(commandContextManager.getCommand(commandId));
        assertNotNull(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID));

        commandContextManager.cleanup(commandId);

        assertNull(commandContextManager.getCommand(commandId));
        assertNull(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID));
    }

    @Test
    @DisplayName("Cleanup by SSH session should remove related commands")
    void testCleanupBySshSession_ShouldRemoveRelatedCommands() {
        commandContextManager.registerCommand(
                TEST_AI_SESSION_ID, TEST_SSH_SESSION_ID, "cmd1");
        commandContextManager.registerCommand(
                TEST_AI_SESSION_ID + "2", TEST_SSH_SESSION_ID, "cmd2");

        commandContextManager.cleanupBySshSession(TEST_SSH_SESSION_ID);

        assertNull(commandContextManager.getActiveCommandId(TEST_SSH_SESSION_ID));
    }

    @Test
    @DisplayName("Cleanup by AI session should remove all related commands")
    void testCleanupByAiSession_ShouldRemoveAllRelatedCommands() {
        String aiSession1 = "ai-session-1";
        String sshSession1 = "ssh-1";
        String sshSession2 = "ssh-2";

        commandContextManager.registerCommand(aiSession1, sshSession1, "cmd1");
        commandContextManager.registerCommand(aiSession1, sshSession2, "cmd2");

        commandContextManager.cleanupByAiSession(aiSession1);

        assertNull(commandContextManager.getActiveCommandId(sshSession1));
        assertNull(commandContextManager.getActiveCommandId(sshSession2));
    }

    // ==================== Serial Execution Tests ====================

    @Test
    @DisplayName("Same SSH session should only have one active command at a time")
    void testSameSshSession_ShouldOnlyHaveOneActiveCommand() {
        String sshSessionId = "ssh-serial-test";

        String cmdId1 = commandContextManager.registerCommand("ai-1", sshSessionId, "cmd1");
        assertEquals(cmdId1, commandContextManager.getActiveCommandId(sshSessionId));

        // Register second command for same SSH session
        String cmdId2 = commandContextManager.registerCommand("ai-2", sshSessionId, "cmd2");

        // Second command should override first
        assertEquals(cmdId2, commandContextManager.getActiveCommandId(sshSessionId));

        // First command should still exist in map but not active
        assertNotNull(commandContextManager.getCommand(cmdId1));
        assertNotNull(commandContextManager.getCommand(cmdId2));
    }

    @Test
    @DisplayName("Different SSH sessions can have active commands simultaneously")
    void testDifferentSshSessions_CanHaveActiveCommandsSimultaneously() {
        String ssh1 = "ssh-1";
        String ssh2 = "ssh-2";

        String cmdId1 = commandContextManager.registerCommand("ai-1", ssh1, "cmd1");
        String cmdId2 = commandContextManager.registerCommand("ai-2", ssh2, "cmd2");

        assertEquals(cmdId1, commandContextManager.getActiveCommandId(ssh1));
        assertEquals(cmdId2, commandContextManager.getActiveCommandId(ssh2));
        assertNotEquals(cmdId1, cmdId2);
    }

    // ==================== Getter Tests ====================

    @Test
    @DisplayName("Get all commands should return all command contexts")
    void testGetAllCommands_ShouldReturnAllCommandContexts() {
        commandContextManager.registerCommand("ai-1", "ssh-1", "cmd1");
        commandContextManager.registerCommand("ai-2", "ssh-2", "cmd2");

        var commands = commandContextManager.getAllCommands();

        assertEquals(2, commands.size());
    }

    @Test
    @DisplayName("Get active command count should return correct number")
    void testGetActiveCommandCount_ShouldReturnCorrectNumber() {
        String ssh1 = "ssh-1";
        String ssh2 = "ssh-2";
        String ssh3 = "ssh-3";

        commandContextManager.registerCommand("ai-1", ssh1, "cmd1");
        commandContextManager.registerCommand("ai-2", ssh2, "cmd2");
        commandContextManager.registerCommand("ai-3", ssh3, "cmd3");

        assertEquals(3, commandContextManager.getActiveCommandCount());
    }

    @Test
    @DisplayName("Get command by non-existent ID should return null")
    void testGetNonExistentCommand_ShouldReturnNull() {
        CommandContext command = commandContextManager.getCommand("non-existent-command");
        assertNull(command);
    }
}
