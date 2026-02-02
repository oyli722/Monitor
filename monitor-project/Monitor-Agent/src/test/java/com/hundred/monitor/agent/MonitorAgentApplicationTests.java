package com.hundred.monitor.agent;

import com.hundred.monitor.agent.service.CollectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MonitorAgentApplicationTests {

	@Autowired
	CollectService collectService;
	@Test
	void contextLoads() {
	}

	@Test
	void testCollectService(){
		System.out.println(collectService.collectMetrics());
		System.out.println(collectService.collectBasicInfo());
	}

}
