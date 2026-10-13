/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(classes = { ShutdownSampleActuatorApplicationTests.SecurityConfiguration.class,
		SampleActuatorApplication.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ShutdownSampleActuatorApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHome() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				this.restTemplate.withBasicAuth("user", "password").getForEntity("/", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = entity.getBody();
		assertThat(body).containsEntry("message", "Hello Phil");
	}

	@Test
	@DirtiesContext
	void testShutdown() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(this.restTemplate.withBasicAuth("user", "password")
			.postForEntity("/actuator/shutdown", null, Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = entity.getBody();
		assertThat(body).isNotNull();
		assertThat(((String) body.get("message"))).contains("Shutting down");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) {
			http.csrf(CsrfConfigurer::disable);
			return http.build();
		}

	}

}
