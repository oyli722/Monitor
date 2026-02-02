package com.hundred.monitor.server;

import com.hundred.monitor.server.sshTest.SshTest;
import com.jcraft.jsch.JSchException;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

@SpringBootTest
class MonitorServerApplicationTests {
    @Resource
    SshTest sshTest;

    @Test
    void contextLoads() throws JSchException, IOException, InterruptedException {
        sshTest.loginSsh();
    }

}
