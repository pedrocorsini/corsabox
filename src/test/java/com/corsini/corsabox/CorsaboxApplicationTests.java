package com.corsini.corsabox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:corsabox-test;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.docker.compose.enabled=false"
})
class CorsaboxApplicationTests {

	@Test
	void contextLoads() {
	}

}
