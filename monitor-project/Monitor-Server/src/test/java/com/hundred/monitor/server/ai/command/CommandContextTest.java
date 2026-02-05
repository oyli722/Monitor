package com.hundred.monitor.server.ai.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandContext 实体类测试
 * 测试命令上下文的各种状态和方法
 */
@DisplayName("命令上下文实体测试")
class CommandContextTest {

    private CommandContext context;

    @BeforeEach
    void setUp() {
        // 创建一个标准的命令上下文用于测试
        context = CommandContext.builder()
                .commandId("test-command-123")
                .aiSessionId("ai-session-456")
                .sshSessionId("ssh-session-789")
                .command("ls -la")
                .output(new StringBuilder())
                .startTime(System.currentTimeMillis())
                .status(CommandStatus.EXECUTING)
                .timeoutMillis(5000)
                .build();
    }

    @Test
    @DisplayName("未超时命令应返回false")
    void testIsTimeout_WhenNotTimeout_ReturnsFalse() {
        // 刚创建的命令，未超时
        assertFalse(context.isTimeout());
    }

    @Test
    @DisplayName("超时命令应返回true")
    void testIsTimeout_WhenTimeout_ReturnsTrue() {
        // 设置开始时间为10秒前
        context.setStartTime(System.currentTimeMillis() - 10000);

        // 超时时间设置为5秒
        context.setTimeoutMillis(5000);

        assertTrue(context.isTimeout());
    }

    @Test
    @DisplayName("刚好在超时边界应返回false")
    void testIsTimeout_AtBoundary_ReturnsFalse() {
        // 设置开始时间为4999毫秒前
        context.setStartTime(System.currentTimeMillis() - 4999);

        // 超时时间设置为5000毫秒
        context.setTimeoutMillis(5000);

        assertFalse(context.isTimeout());
    }

    @Test
    @DisplayName("超过超时边界1毫秒应返回true")
    void testIsTimeout_JustOverBoundary_ReturnsTrue() {
        // 设置开始时间为5001毫秒前
        context.setStartTime(System.currentTimeMillis() - 5001);

        // 超时时间设置为5000毫秒
        context.setTimeoutMillis(5000);

        assertTrue(context.isTimeout());
    }

    @Test
    @DisplayName("获取空输出应返回空字符串")
    void testGetOutput_WhenEmpty_ReturnsEmptyString() {
        // output为空StringBuilder
        context.setOutput(null);

        assertEquals("", context.getOutput());
    }

    @Test
    @DisplayName("获取有内容的输出应返回正确字符串")
    void testGetOutput_WithContent_ReturnsCorrectString() {
        StringBuilder sb = new StringBuilder();
        sb.append("line1\n");
        sb.append("line2\n");
        context.setOutput(sb);

        assertEquals("line1\nline2\n", context.getOutput());
    }

    @Test
    @DisplayName("追加输出应正确添加到StringBuilder")
    void testAppendOutput_ShouldAppendToStringBuilder() {
        context.setOutput(new StringBuilder("initial"));

        context.appendOutput(" + appended");

        assertEquals("initial + appended", context.getOutput());
    }

    @Test
    @DisplayName("多次追加输出应正确累积")
    void testAppendOutput_MultipleTimes_ShouldAccumulate() {
        context.setOutput(new StringBuilder());

        context.appendOutput("first");
        context.appendOutput(" ");
        context.appendOutput("second");
        context.appendOutput(" ");
        context.appendOutput("third");

        assertEquals("first second third", context.getOutput());
    }

    @Test
    @DisplayName("向null output追加应自动初始化")
    void testAppendOutput_WhenOutputIsNull_ShouldInitialize() {
        context.setOutput(null);

        context.appendOutput("test");

        assertEquals("test", context.getOutput());
    }

    @Test
    @DisplayName("获取已执行时长应返回正确值")
    void testGetElapsedTime_ShouldReturnCorrectDuration() {
        long startTime = System.currentTimeMillis() - 1000; // 1秒前
        context.setStartTime(startTime);

        long elapsed = context.getElapsedTime();

        // 允许100毫秒误差
        assertTrue(elapsed >= 1000 && elapsed < 1100,
                "执行时长应在1000-1100毫秒之间，实际: " + elapsed);
    }

    @Test
    @DisplayName("新创建命令的执行时长应接近0")
    void testGetElapsedTime_WhenNew_ShouldBeNearZero() {
        // 刚创建的命令
        long elapsed = context.getElapsedTime();

        // 应该小于100毫秒
        assertTrue(elapsed < 100, "新命令执行时长应接近0，实际: " + elapsed);
    }

    @Test
    @DisplayName("Builder模式应正确创建所有字段")
    void testBuilder_ShouldCreateAllFields() {
        CommandContext built = CommandContext.builder()
                .commandId("cmd-id")
                .aiSessionId("ai-id")
                .sshSessionId("ssh-id")
                .command("test command")
                .output(new StringBuilder("output"))
                .startTime(12345L)
                .status(CommandStatus.COMPLETED)
                .timeoutMillis(3000)
                .build();

        assertEquals("cmd-id", built.getCommandId());
        assertEquals("ai-id", built.getAiSessionId());
        assertEquals("ssh-id", built.getSshSessionId());
        assertEquals("test command", built.getCommand());
        assertEquals("output", built.getOutput());
        assertEquals(12345L, built.getStartTime());
        assertEquals(CommandStatus.COMPLETED, built.getStatus());
        assertEquals(3000, built.getTimeoutMillis());
    }

    @Test
    @DisplayName("无参构造函数应创建默认值对象")
    void testNoArgsConstructor_ShouldCreateDefaultObject() {
        CommandContext emptyContext = new CommandContext();

        assertNull(emptyContext.getCommandId());
        assertNull(emptyContext.getAiSessionId());
        assertNull(emptyContext.getSshSessionId());
        assertNull(emptyContext.getCommand());
        // Lombok @Builder.Default 会初始化为空 StringBuilder
        assertEquals("", emptyContext.getOutput());
        assertEquals(0, emptyContext.getStartTime());
        assertNull(emptyContext.getStatus());
        assertEquals(0, emptyContext.getTimeoutMillis());
    }
}
