/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureTestRestTemplate
class SampleActuatorExtensionApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Test
	@SuppressWarnings("rawtypes")
	void healthActuatorIsNotExposed() {
		ResponseEntity<Map> entity = this.restTemplate.getForEntity("/actuator/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void healthExtensionWithAuthHeaderIsDenied() {
		ResponseEntity<Map> entity = this.restTemplate.getForEntity("/myextension/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void healthExtensionWithAuthHeader() {
		TestRestTemplate restTemplate = new TestRestTemplate(
				this.restTemplateBuilder.defaultHeader("Authorization", "Bearer secret"));
		LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(this.applicationContext);
		restTemplate.setUriTemplateHandler(localTestWebServer.uriBuilderFactory());
		ResponseEntity<Map> entity = restTemplate.getForEntity("/myextension/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
