package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.command.CommandContextManager;
import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.ai.entity.SshAssistantMessage;
import com.hundred.monitor.server.ai.entity.SshAssistantSessionInfo;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.model.request.SshConnectRequest;
import com.hundred.monitor.server.service.SshService;
import com.hundred.monitor.server.service.impl.SshServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 用于测试AI是否能够连接到主机输出正确的结果
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
public class AiSshTaskExecuteTest {
    @Resource
    TerminalChatRedisUtils terminalChatRedisUtils;
    @Resource
    AiSshAssistantService aiSshAssistantService;
    @Resource
    SshService sshService;
    @Resource
    CommandContextManager commandContextManager;

    @Test
    public void testTaskExecute() throws Exception {
        // 构建请求，进行连接
        SshConnectRequest request = SshConnectRequest.builder()
                .agentId("agent-001")
                .username("hundred")
                .password("100000100ce.")
                .build();

        // 获取sessionId Ai助手通过这个获取命令
        String sessionId = sshService.connect(request);
        // 注入上下文
        SshSessionContext.setSshSessionId(sessionId);

        String userId = "default-user";
        String aiSessionId = UUID.randomUUID().toString();
        // 3. 创建SSH与AI会话的绑定关系
        SshSessionBinding binding = SshSessionBinding.create(
                aiSessionId,
                sessionId,
                request.getAgentId(),
                userId
        );
        terminalChatRedisUtils.saveBinding(binding);

        // 4. 创建会话信息
        SshAssistantSessionInfo sessionInfo = SshAssistantSessionInfo.builder()
                .sessionId(aiSessionId)
                .title("主机助手 - " + request.getAgentId())
                .linkedAgentId(request.getAgentId())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .messageCount(0)
                .build();
        terminalChatRedisUtils.saveSessionInfo(sessionInfo);

        // 5. 向AI发送消息，请求执行ls命令
        System.out.println("\n========== 开始AI命令执行测试 ==========");
        System.out.println("AI会话ID: " + aiSessionId);
        System.out.println("SSH会话ID: " + sessionId);
        System.out.println("=====================================\n");

        // 发送执行命令的请求
        String userMessage = "请执行ls命令并查看当前目录的文件列表";
        System.out.println("用户消息: " + userMessage);

        String aiResponse = aiSshAssistantService.sendMessage(aiSessionId, userMessage);
        System.out.println("\n========== AI回复 ==========");
        System.out.println(aiResponse);
        System.out.println("==============================\n");

        // 6. 模拟SSH命令输出（因为没有WebSocket连接读取真实输出）
        System.out.println("模拟SSH命令输出...");
        Thread.sleep(2000);  // 等待命令注册完成

        // 模拟ls命令的输出
        String mockOutput = "Desktop  Documents  Downloads  Music  Pictures  Public  Templates  Videos\n";
        commandContextManager.appendOutput(sessionId, mockOutput);

        // 模拟命令结束标记
        String endMarker = "\n## COMMAND_END: " + System.currentTimeMillis() + " ##\n";
        commandContextManager.appendOutput(sessionId, endMarker);
        commandContextManager.completeCommand(sessionId);

        // 7. 等待AI分析完成
        System.out.println("等待AI分析完成...");
        Thread.sleep(10000);  // 等待10秒，让AI分析完成

        // 8. 获取消息历史，查看完整的对话
        System.out.println("\n========== 消息历史 ==========");
        var messages = aiSshAssistantService.getMessages(aiSessionId);
        for (SshAssistantMessage msg : messages) {
            System.out.println("[" + msg.getRole() + "] " + msg.getContent());
        }
        System.out.println("==============================\n");

        // 9. 验证AI是否成功执行命令并返回结果
        assertNotNull(aiResponse, "AI回复不应为空");
        assertFalse(aiResponse.isEmpty(), "AI回复不应为空字符串");

        // 获取最终消息历史
        var finalMessages = aiSshAssistantService.getMessages(aiSessionId);

        // 验证是否有AI分析结果（应该有至少3条消息：用户、AI执行命令、AI分析结果）
        assertTrue(finalMessages.size() >= 2, "应该至少有2条消息（用户和AI回复）");

        // 检查最后一条消息是否是AI的分析结果
        SshAssistantMessage lastMessage = finalMessages.get(finalMessages.size() - 1);
        System.out.println("最后一条消息: " + lastMessage.getContent());

        // 验证AI回复中是否包含相关信息
        boolean hasCommandInfo = aiResponse.contains("ls") ||
                aiResponse.contains("执行") ||
                aiResponse.contains("文件") ||
                aiResponse.contains("目录") ||
                aiResponse.contains("command");

        System.out.println("\n========== 测试结果 ==========");
        System.out.println("消息总数: " + finalMessages.size());
        System.out.println("AI回复长度: " + aiResponse.length() + " 字符");
        System.out.println("包含命令相关信息: " + hasCommandInfo);
        System.out.println("最后一条消息包含分析: " + (lastMessage.getRole().equals("assistant") && finalMessages.size() >= 2));
        System.out.println("==============================\n");

        // 清理
        terminalChatRedisUtils.clearMessages(aiSessionId);
        terminalChatRedisUtils.deleteBinding(aiSessionId);
        terminalChatRedisUtils.deleteSessionInfo(aiSessionId);
        System.out.println("测试清理完成");
    }
}
