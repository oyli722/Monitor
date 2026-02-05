package com.hundred.monitor.server.ai.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SshSessionContext ThreadLocal上下文测试
 * 测试ThreadLocal变量的设置、获取和清理
 */
@DisplayName("SSH会话ThreadLocal上下文测试")
class SshSessionContextTest {

    @BeforeEach
    void setUp() {
        // 每个测试前确保上下文是干净的
        SshSessionContext.clear();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理上下文，避免影响其他测试
        SshSessionContext.clear();
    }

    @Test
    @DisplayName("设置aiSessionId后应能正确获取")
    void testSetAiSessionId_ShouldBeRetrievable() {
        String aiSessionId = "test-ai-session-123";

        SshSessionContext.setAiSessionId(aiSessionId);

        assertEquals(aiSessionId, SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("多次设置aiSessionId应使用最新值")
    void testSetAiSessionId_MultipleTimes_ShouldUseLatestValue() {
        SshSessionContext.setAiSessionId("first-ai-session");
        SshSessionContext.setAiSessionId("second-ai-session");
        SshSessionContext.setAiSessionId("third-ai-session");

        assertEquals("third-ai-session", SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("未设置aiSessionId时获取应返回null")
    void testGetAiSessionId_WhenNotSet_ShouldReturnNull() {
        // 未调用 setAiSessionId()
        String aiSessionId = SshSessionContext.getAiSessionId();

        assertNull(aiSessionId);
    }

    @Test
    @DisplayName("设置null作为aiSessionId应正常工作")
    void testSetAiSessionId_WithNull_ShouldWork() {
        SshSessionContext.setAiSessionId("some-value");
        SshSessionContext.setAiSessionId(null);

        assertNull(SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("clear()应清理aiSessionId")
    void testClear_ShouldClearAiSessionId() {
        SshSessionContext.setAiSessionId("test-ai-session");

        SshSessionContext.clear();

        assertNull(SshSessionContext.getAiSessionId(),
                "clear()后aiSessionId应为null");
    }

    @Test
    @DisplayName("clear()应清理所有ThreadLocal变量")
    void testClear_ShouldClearAllThreadLocalVariables() {
        // 设置所有ThreadLocal变量
        SshSessionContext.setSshSessionId("test-ssh-session");
        SshSessionContext.setAgentId("test-agent-id");
        SshSessionContext.setAiSessionId("test-ai-session");

        // 执行清理
        SshSessionContext.clear();

        // 验证所有变量都被清理
        assertNull(SshSessionContext.getSshSessionId(),
                "clear()后sshSessionId应为null");
        assertNull(SshSessionContext.getAgentId(),
                "clear()后agentId应为null");
        assertNull(SshSessionContext.getAiSessionId(),
                "clear()后aiSessionId应为null");
    }

    @Test
    @DisplayName("多个ThreadLocal变量应互不干扰")
    void testMultipleThreadLocalVariables_ShouldBeIndependent() {
        String sshSessionId = "ssh-123";
        String agentId = "agent-456";
        String aiSessionId = "ai-789";

        SshSessionContext.setSshSessionId(sshSessionId);
        SshSessionContext.setAgentId(agentId);
        SshSessionContext.setAiSessionId(aiSessionId);

        // 验证三个变量都能正确获取
        assertEquals(sshSessionId, SshSessionContext.getSshSessionId());
        assertEquals(agentId, SshSessionContext.getAgentId());
        assertEquals(aiSessionId, SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("清理后重新设置应正常工作")
    void testClearAndSetAgain_ShouldWork() {
        // 第一次设置
        SshSessionContext.setAiSessionId("first-ai-session");
        assertEquals("first-ai-session", SshSessionContext.getAiSessionId());

        // 清理
        SshSessionContext.clear();
        assertNull(SshSessionContext.getAiSessionId());

        // 重新设置
        SshSessionContext.setAiSessionId("second-ai-session");
        assertEquals("second-ai-session", SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("在清理前只清理部分变量应不影响其他变量")
    void testPartialClear_ShouldNotAffectOtherVariables() {
        SshSessionContext.setSshSessionId("ssh-session");
        SshSessionContext.setAiSessionId("ai-session");

        // 手动设置aiSessionId为null（模拟部分清理）
        SshSessionContext.setAiSessionId(null);

        // sshSessionId应该仍然存在
        assertEquals("ssh-session", SshSessionContext.getSshSessionId());
        assertNull(SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("连续调用clear()应该是安全的")
    void testMultipleClearCalls_ShouldBeSafe() {
        SshSessionContext.setAiSessionId("test-session");

        // 连续调用多次clear()
        SshSessionContext.clear();
        SshSessionContext.clear();
        SshSessionContext.clear();

        // 不应该抛出异常
        assertNull(SshSessionContext.getAiSessionId());
    }

    @Test
    @DisplayName("验证原有字段sshSessionId和agentId仍然正常工作")
    void testExistingFields_ShouldStillWork() {
        String sshSessionId = "ssh-session-123";
        String agentId = "agent-456";

        SshSessionContext.setSshSessionId(sshSessionId);
        SshSessionContext.setAgentId(agentId);

        assertEquals(sshSessionId, SshSessionContext.getSshSessionId());
        assertEquals(agentId, SshSessionContext.getAgentId());

        SshSessionContext.clear();

        assertNull(SshSessionContext.getSshSessionId());
        assertNull(SshSessionContext.getAgentId());
    }
}
