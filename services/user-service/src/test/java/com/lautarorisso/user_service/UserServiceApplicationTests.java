package com.lautarorisso.user_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Until persistence adapters (T034) are implemented - JPA repos needed by KeycloakSyncScheduler")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
