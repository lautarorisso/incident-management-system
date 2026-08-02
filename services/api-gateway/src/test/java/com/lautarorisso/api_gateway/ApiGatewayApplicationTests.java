package com.lautarorisso.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Import(TestSecurityConfig.class)
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
