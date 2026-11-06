package mogni.system_backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SystemBackendApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	/** Los tests nunca usan el perfil "local" ni su base compartida: siempre una base embebida en esta máquina. */
	@Test
	void los_tests_usan_la_base_embebida_y_no_la_compartida() {
		assertFalse(List.of(environment.getActiveProfiles()).contains("local"));
		String url = environment.getProperty("spring.datasource.url");
		assertTrue(url.startsWith("jdbc:postgresql://localhost:"), url);
	}
}
