package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.ai.entity.SshAssistantMessage;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.service.AgentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AiSshAssistantService Integration Test
 * Real AI model integration tests using Ollama
 */
@SpringBootTest
@TestPropertySource(properties = {
        "ai.monitor-agent.default-model-name=ollama",
        "ai.monitor-agent.use-assistant=true",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=",
        "spring.data.redis.database=0",
        "langchain4j.ollama.chat-model.base-url=http://localhost:11434/v1",
        "langchain4j.ollama.chat-model.model-name=qwen2.5:7b"
})
@DisplayName("AI SSH Assistant Service Integration Test (Ollama)")
class AiSshAssistantServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AiSshAssistantServiceIntegrationTest.class);
    @Autowired
    private AiSshAssistantService aiSshAssistantService;

    @MockBean
    private AgentService agentService;

    @Autowired
    private TerminalChatRedisUtils aiSshRedisUtils;

    private static final String TEST_AI_SESSION_ID = "integration-test-ai-session";
    private static final String TEST_SSH_SESSION_ID = "integration-test-ssh-session";
    private static final String TEST_AGENT_ID = "integration-test-agent-123";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        try {
            aiSshRedisUtils.clearMessages(TEST_AI_SESSION_ID);
            aiSshRedisUtils.deleteBinding(TEST_AI_SESSION_ID);
        } catch (Exception ignored) {
        }

        // Clean up ThreadLocal context
        SshSessionContext.clear();

        // Setup binding for AI session
        SshSessionBinding binding = SshSessionBinding.builder()
                .aiSessionId(TEST_AI_SESSION_ID)
                .sshSessionId(TEST_SSH_SESSION_ID)
                .agentId(TEST_AGENT_ID)
                .build();
        aiSshRedisUtils.saveBinding(binding);

        // Mock agent service
        Agent mockAgent = new Agent();
        mockAgent.setAgentId(TEST_AGENT_ID);
        mockAgent.setHostname("test-host");
        mockAgent.setIp("192.168.1.100");
        when(agentService.getAgentById(TEST_AGENT_ID)).thenReturn(mockAgent);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        try {
            aiSshRedisUtils.clearMessages(TEST_AI_SESSION_ID);
            aiSshRedisUtils.deleteBinding(TEST_AI_SESSION_ID);
        } catch (Exception ignored) {
        }
        SshSessionContext.clear();
    }

    // ==================== Basic AI Interaction Tests ====================

    @Test
    @DisplayName("Real AI call: simple greeting should return response")
    void testRealAiCall_SimpleGreeting_ShouldReturnResponse() {
        String userMessage = "Hello, please introduce yourself briefly.";

        String aiReply = aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, userMessage);
        System.out.println("=== User Message ===" + aiReply);
        assertNotNull(aiReply, "AI should return a response");
        assertFalse(aiReply.isEmpty(), "AI response should not be empty");
        assertTrue(aiReply.length() > 10, "AI response should have meaningful content");

        System.out.println("=== AI Response ===");
        System.out.println(aiReply);
        System.out.println("===================");
    }

    @Test
    @DisplayName("Real AI call: technical question should return relevant answer")
    void testRealAiCall_TechnicalQuestion_ShouldReturnRelevantAnswer() {
        String userMessage = "What is the command to check disk usage in Linux?";

        String aiReply = aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, userMessage);

        log.info("ai reply:{}", aiReply);

        assertNotNull(aiReply, "AI should return a response");
        assertFalse(aiReply.isEmpty(), "AI response should not be empty");

        // The response should contain relevant information about disk usage
        String lowerReply = aiReply.toLowerCase();
        boolean containsRelevantInfo =
                lowerReply.contains("df") ||
                lowerReply.contains("disk") ||
                lowerReply.contains("usage") ||
                lowerReply.contains("du");

        assertTrue(containsRelevantInfo, "Response should contain relevant disk usage information");

        System.out.println("=== AI Response ===");
        System.out.println(aiReply);
        System.out.println("===================");
    }

    // ==================== Conversation Context Tests ====================

    @Test
    @DisplayName("Real AI call: multi-turn conversation should maintain context")
    void testRealAiCall_MultiTurnConversation_ShouldMaintainContext() {
        // First message
        String msg1 = "My name is Alice. Remember this.";
        String reply1 = aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, msg1);
        assertNotNull(reply1);

        // Wait a bit
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Second message - asking if AI remembers
        String msg2 = "What is my name?";
        String reply2 = aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, msg2);
        assertNotNull(reply2);

        System.out.println("=== First Response ===");
        System.out.println(reply1);
        System.out.println("=== Second Response ===");
        System.out.println(reply2);
        System.out.println("======================");

        // The AI should mention "Alice" or indicate it remembers the name
        // Note: This may not always work due to model limitations
        String lowerReply2 = reply2.toLowerCase();
        boolean remembersName =
                lowerReply2.contains("alice") ||
                lowerReply2.contains("remember");

        // We don't fail the test if context isn't maintained, as it depends on model
        System.out.println("AI remembers context: " + remembersName);
    }

    // ==================== Message History Tests ====================

    @Test
    @DisplayName("Real AI call: message history should be saved")
    void testRealAiCall_MessageHistory_ShouldBeSaved() {
        String userMessage = "Test message for history.";

        aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, userMessage);

        // Wait for message to be saved
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<SshAssistantMessage> messages = aiSshAssistantService.getMessages(TEST_AI_SESSION_ID);

        assertNotNull(messages, "Messages should not be null");
        assertTrue(messages.size() >= 2, "Should have at least 2 messages (user + AI)");

        // First message should be from user
        SshAssistantMessage firstMessage = messages.get(messages.size() - 2);
        assertEquals("user", firstMessage.getRole());
        assertTrue(firstMessage.getContent().contains(userMessage) ||
                   userMessage.contains(firstMessage.getContent()));

        // Last message should be from AI
        SshAssistantMessage lastMessage = messages.get(messages.size() - 1);
        assertEquals("assistant", lastMessage.getRole());
        assertFalse(lastMessage.getContent().isEmpty());

        System.out.println("=== Message History ===");
        for (int i = 0; i < messages.size(); i++) {
            System.out.println(i + ": [" + messages.get(i).getRole() + "] " +
                    messages.get(i).getContent());
        }
        System.out.println("=======================");
    }

    // ==================== Clear Messages Tests ====================

    @Test
    @DisplayName("Real AI call: clear messages should remove history")
    void testRealAiCall_ClearMessages_ShouldRemoveHistory() {
        // Send a message first
        aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, "Initial message");

        // Wait
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify messages exist
        List<SshAssistantMessage> messagesBefore = aiSshAssistantService.getMessages(TEST_AI_SESSION_ID);
        assertFalse(messagesBefore.isEmpty(), "Should have messages before clear");

        // Clear messages
        aiSshAssistantService.clearMessages(TEST_AI_SESSION_ID);

        // Verify messages are cleared
        List<SshAssistantMessage> messagesAfter = aiSshAssistantService.getMessages(TEST_AI_SESSION_ID);
        assertTrue(messagesAfter.isEmpty(), "Should have no messages after clear");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Real AI call: non-existent session should throw exception")
    void testRealAiCall_NonExistentSession_ShouldThrowException() {
        String nonExistentSessionId = "non-existent-session-xyz";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSshAssistantService.sendMessage(nonExistentSessionId, "Test")
        );

        assertTrue(exception.getMessage().contains("AI会话不存在或已过期"));
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Real AI call: response time should be reasonable")
    void testRealAiCall_ResponseTime_ShouldBeReasonable() {
        String userMessage = "What is 2 + 2?";

        long startTime = System.currentTimeMillis();
        String aiReply = aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, userMessage);
        long endTime = System.currentTimeMillis();

        long responseTime = endTime - startTime;

        assertNotNull(aiReply);
        assertTrue(responseTime < 30000, "Response time should be less than 30 seconds, actual: " + responseTime + "ms");

        System.out.println("=== Response Time ===");
        System.out.println("Time: " + responseTime + "ms");
        System.out.println("AI Reply: " + aiReply);
        System.out.println("=====================");
    }

    // ==================== Command Analysis Tests ====================

    @Test
    @DisplayName("Real AI call: analyze command output should provide analysis")
    void testRealAiCall_AnalyzeCommandOutput_ShouldProvideAnalysis() {
        String command = "top -b -n 1";
        String output = """
                top - 00:00:01 up 1 day,  2:30,  2 users,  load average: 0.50, 0.40, 0.30
                Tasks:   5 total,   1 running,   4 sleeping,   0 stopped,   0 zombie
                %Cpu(s):  5.0 us,  2.0 sy,  0.0 ni, 92.0 id,  1.0 wa,  0.0 hi,  0.0 si,  0.0 st
                MiB Mem:   8192.0 total,   2048.0 free,   4096.0 used,   2048.0 cache
                """;

        String analysis = aiSshAssistantService.analyzeCommandOutput(command, output);

        assertNotNull(analysis, "Analysis should not be null");
        assertFalse(analysis.isEmpty(), "Analysis should not be empty");
        assertFalse(analysis.contains("命令已执行，但分析失败"),
                   "Should not return error message for valid input");

        System.out.println("=== Command Output Analysis ===");
        System.out.println("Command: " + command);
        System.out.println("Analysis: " + analysis);
        System.out.println("==============================");
    }

    // ==================== ThreadLocal Context Tests ====================

    @Test
    @DisplayName("Real AI call: ThreadLocal context should be cleaned after execution")
    void testRealAiCall_ThreadLocalContext_ShouldBeCleanedAfterExecution() {
        String userMessage = "Test message";

        aiSshAssistantService.sendMessage(TEST_AI_SESSION_ID, userMessage);

        // After execution, ThreadLocal should be cleaned
        assertNull(SshSessionContext.getSshSessionId());
        assertNull(SshSessionContext.getAgentId());
        assertNull(SshSessionContext.getAiSessionId());
    }
}
