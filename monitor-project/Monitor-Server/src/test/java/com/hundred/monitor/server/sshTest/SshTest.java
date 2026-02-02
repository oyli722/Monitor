package com.hundred.monitor.server.sshTest;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SshTest {
    public void loginSsh() throws JSchException, IOException, InterruptedException {
        String ip = "192.168.80.132";
        String username = "hundred";
        String password = "100000100ce.";


        JSch jsch = new JSch();
        Session session = jsch.getSession(username, ip, 22);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(5000);
        session.connect();


        //  创建Shell通道
        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        channel.setPtyType("xterm");
        channel.connect(1000);
        log.info("SSH通道已连接");

        InputStream inputStream = channel.getInputStream();
        InputStream errorStream = channel.getExtInputStream();
        OutputStream outputStream = channel.getOutputStream();

        // 创建读取线程
        ExecutorService service = Executors.newSingleThreadExecutor();
        // 创建写入线程
        ExecutorService out = Executors.newSingleThreadExecutor();
        Thread.sleep(1000);
        service.execute(() -> {
            try {
                read(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        out.execute(() -> {
            try {
                write(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void read(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024*1024];
        int i;
        while ((i = inputStream.read(buffer)) != -1) {
            log.info("{}", new String(buffer, 0, i));
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        outputStream.write("ls\n".getBytes());
        outputStream.flush();
    }
}
