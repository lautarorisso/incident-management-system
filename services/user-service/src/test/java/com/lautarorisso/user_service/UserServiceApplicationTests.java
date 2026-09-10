package com.lautarorisso.user_service;

import com.lautarorisso.user_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests extends AbstractPostgresTestBase {

	@Test
	void contextLoads() {
	}

}
