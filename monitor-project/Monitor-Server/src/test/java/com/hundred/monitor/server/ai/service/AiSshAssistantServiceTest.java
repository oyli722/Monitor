package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.ai.entity.Assistant;
import com.hundred.monitor.server.ai.entity.SshAssistantMessage;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.entity.SystemPrompt;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.service.AgentService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AiSshAssistantService Test
 * AI SSH assistant service layer functionality tests
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI SSH Assistant Service Test")
class AiSshAssistantServiceTest {

    @Mock
    private TerminalChatRedisUtils aiSshRedisUtils;

    @Mock
    private OpenAiChatModel ollamaChatModel;

    @Mock
    private OpenAiChatModel glmChatModel;

    @Mock
    private AgentService agentService;

    @Mock
    private Assistant defaultAssistant;

    @Mock
    private Assistant ollamaAssistant;

    @Mock
    private Assistant glmAssistant;

    private AiSshAssistantService service;

    private static final String TEST_AI_SESSION_ID = "test-ai-session";
    private static final String TEST_SSH_SESSION_ID = "test-ssh-session";
    private static final String TEST_AGENT_ID = "test-agent-123";

    @BeforeEach
    void setUp() {
        // Create service instance manually
        service = new AiSshAssistantService();

        // Manually inject mocks using ReflectionTestUtils
        ReflectionTestUtils.setField(service, "aiSshRedisUtils", aiSshRedisUtils);
        ReflectionTestUtils.setField(service, "ollamaChatModel", ollamaChatModel);
        ReflectionTestUtils.setField(service, "glmChatModel", glmChatModel);
        ReflectionTestUtils.setField(service, "agentService", agentService);
        ReflectionTestUtils.setField(service, "defaultAssistant", defaultAssistant);
        ReflectionTestUtils.setField(service, "ollamaAssistant", ollamaAssistant);
        ReflectionTestUtils.setField(service, "glmAssistant", glmAssistant);

        // Set default values for @Value fields
        ReflectionTestUtils.setField(service, "defaultModelName", "ollama");
        ReflectionTestUtils.setField(service, "useAssistant", true);

        // Initialize SystemPrompt (static initialization may not work in tests)
        try {
            // Ensure SystemPrompt is loaded
            SystemPrompt.getSshAssistantPrompt();
        } catch (Exception ignored) {
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up ThreadLocal context
        SshSessionContext.clear();
    }

    // ==================== sendMessage Tests ====================

    @Test
    @DisplayName("sendMessage should throw exception when binding does not exist")
    void testSendMessage_WhenBindingNotExists_ShouldThrowException() {
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.sendMessage(TEST_AI_SESSION_ID, "Hello")
        );

        assertTrue(exception.getMessage().contains("AI会话不存在或已过期"));
    }

    @Test
    @DisplayName("sendMessage should save user message and AI reply")
    void testSendMessage_ShouldSaveUserAndAiMessages() {
        // Setup binding
        SshSessionBinding binding = createTestBinding();
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(binding);

        // Setup agent
        Agent agent = createTestAgent();
        when(agentService.getAgentById(TEST_AGENT_ID)).thenReturn(agent);
        System.out.println(ollamaAssistant.chat("Hello"));
        // Setup AI response
        when(ollamaAssistant.chat(anyString(), anyString())).thenReturn("AI reply");

        // Execute
        String result = service.sendMessage(TEST_AI_SESSION_ID, "Hello AI");

        // Verify
        assertEquals("AI reply", result);

        // Verify messages saved (called twice: user message + AI reply)
        verify(aiSshRedisUtils, times(2)).addMessage(eq(TEST_AI_SESSION_ID), any(SshAssistantMessage.class));
    }

    @Test
    @DisplayName("sendMessage should set and clear ThreadLocal context")
    void testSendMessage_ShouldSetAndClearThreadLocalContext() {
        SshSessionBinding binding = createTestBinding();
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(binding);
        when(agentService.getAgentById(TEST_AGENT_ID)).thenReturn(createTestAgent());
        when(ollamaAssistant.chat(anyString(), anyString())).thenReturn("Reply");

        service.sendMessage(TEST_AI_SESSION_ID, "Test");

        // After completion, ThreadLocal should be cleared
        assertNull(SshSessionContext.getSshSessionId());
        assertNull(SshSessionContext.getAgentId());
        assertNull(SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("sendMessage should set ThreadLocal context during execution")
    void testSendMessage_ShouldSetContextDuringExecution() {
        SshSessionBinding binding = createTestBinding();
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(binding);
        when(agentService.getAgentById(TEST_AGENT_ID)).thenReturn(createTestAgent());

        // Make assistant callback check ThreadLocal
        doAnswer(invocation -> {
            // Verify ThreadLocal is set during AI call
            assertEquals(TEST_SSH_SESSION_ID, SshSessionContext.getSshSessionId());
            assertEquals(TEST_AGENT_ID, SshSessionContext.getAgentId());
            assertEquals(TEST_AI_SESSION_ID, SshSessionContext.getAiSessionId());
            return "Reply";
        }).when(ollamaAssistant).chat(anyString(), anyString());

        service.sendMessage(TEST_AI_SESSION_ID, "Test");
    }

    @Test
    @DisplayName("sendMessage should handle AI exception gracefully")
    void testSendMessage_WhenAiFails_ShouldReturnErrorMessage() {
        SshSessionBinding binding = createTestBinding();
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(binding);
        when(agentService.getAgentById(TEST_AGENT_ID)).thenReturn(createTestAgent());
        when(ollamaAssistant.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        String result = service.sendMessage(TEST_AI_SESSION_ID, "Test");

        assertTrue(result.contains("AI服务暂时不可用"));
        assertTrue(result.contains("AI service unavailable"));
    }

    // ==================== getMessages Tests ====================

    @Test
    @DisplayName("getMessages should return messages from Redis")
    void testGetMessages_ShouldReturnMessagesFromRedis() {
        List<SshAssistantMessage> expectedMessages = List.of(
                SshAssistantMessage.builder().role("user").content("Hello").build(),
                SshAssistantMessage.builder().role("assistant").content("Hi").build()
        );

        when(aiSshRedisUtils.getMessages(TEST_AI_SESSION_ID)).thenReturn(expectedMessages);

        List<SshAssistantMessage> result = service.getMessages(TEST_AI_SESSION_ID);
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getContent());
        assertEquals("Hi", result.get(1).getContent());
    }

    @Test
    @DisplayName("getMessages should return empty list when no messages")
    void testGetMessages_WhenNoMessages_ShouldReturnEmptyList() {
        when(aiSshRedisUtils.getMessages(TEST_AI_SESSION_ID)).thenReturn(List.of());

        List<SshAssistantMessage> result = service.getMessages(TEST_AI_SESSION_ID);

        assertTrue(result.isEmpty());
    }

    // ==================== clearMessages Tests ====================

    @Test
    @DisplayName("clearMessages should clear messages in Redis")
    void testClearMessages_ShouldClearMessages() {
        service.clearMessages(TEST_AI_SESSION_ID);

        verify(aiSshRedisUtils).clearMessages(TEST_AI_SESSION_ID);
    }

    // ==================== analyzeCommandOutput Tests ====================
    // Note: analyzeCommandOutput uses ChatLanguageModel with complex response chain
    // These tests focus on error handling behavior

    @Test
    @DisplayName("analyzeCommandOutput should handle exceptions gracefully")
    void testAnalyzeCommandOutput_WhenModelFails_ShouldReturnError() {
        String command = "ls -la";
        String output = "some output";

        ReflectionTestUtils.setField(service, "useAssistant", false);

        String result = service.analyzeCommandOutput(command, output);

        // When model fails, should return error message
        assertTrue(result.contains("命令已执行，但分析失败"));
    }

    // ==================== getBinding Tests ====================

    @Test
    @DisplayName("getBinding should return binding from Redis")
    void testGetBinding_ShouldReturnBinding() {
        SshSessionBinding expectedBinding = createTestBinding();
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(expectedBinding);

        SshSessionBinding result = service.getBinding(TEST_AI_SESSION_ID);

        assertEquals(expectedBinding, result);
    }

    @Test
    @DisplayName("getBinding should return null when not exists")
    void testGetBinding_WhenNotExists_ShouldReturnNull() {
        when(aiSshRedisUtils.getBinding(TEST_AI_SESSION_ID)).thenReturn(null);

        SshSessionBinding result = service.getBinding(TEST_AI_SESSION_ID);

        assertNull(result);
    }

    // ==================== sessionExists Tests ====================

    @Test
    @DisplayName("sessionExists should return true when binding exists")
    void testSessionExists_WhenBindingExists_ShouldReturnTrue() {
        when(aiSshRedisUtils.bindingExists(TEST_AI_SESSION_ID)).thenReturn(true);

        boolean result = service.sessionExists(TEST_AI_SESSION_ID);

        assertTrue(result);
    }

    @Test
    @DisplayName("sessionExists should return false when binding not exists")
    void testSessionExists_WhenBindingNotExists_ShouldReturnFalse() {
        when(aiSshRedisUtils.bindingExists(TEST_AI_SESSION_ID)).thenReturn(false);

        boolean result = service.sessionExists(TEST_AI_SESSION_ID);

        assertFalse(result);
    }

    // ==================== Helper Methods ====================

    private SshSessionBinding createTestBinding() {
        return SshSessionBinding.builder()
                .aiSessionId(TEST_AI_SESSION_ID)
                .sshSessionId(TEST_SSH_SESSION_ID)
                .agentId(TEST_AGENT_ID)
                .build();
    }

    private Agent createTestAgent() {
        Agent agent = new Agent();
        agent.setAgentId(TEST_AGENT_ID);
        agent.setHostname("test-host");
        agent.setIp("192.168.1.100");
        return agent;
    }
}
