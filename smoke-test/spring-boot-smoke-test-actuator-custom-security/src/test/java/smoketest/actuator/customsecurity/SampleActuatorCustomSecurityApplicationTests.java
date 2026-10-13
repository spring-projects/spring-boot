/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.customsecurity;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for actuator endpoints with custom security configuration.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
class SampleActuatorCustomSecurityApplicationTests extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void testInsecureApplicationPath() {
		ResponseEntity<Map> entity = restTemplate().getForEntity(getPath() + "/foo", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		Map<String, Object> body = entity.getBody();
		assertThat(body).isNotNull();
		assertThat((String) body.get("message")).contains("Expected exception in controller");
	}

	@Test
	void mvcMatchersCanBeUsedToSecureActuators() {
		ResponseEntity<Object> entity = beansRestTemplate().getForEntity(getActuatorPath() + "/beans", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = beansRestTemplate().getForEntity(getActuatorPath() + "/beans/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Override
	String getPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	String getActuatorPath() {
		return "http://localhost:" + this.port + "/actuator";
	}

	@Override
	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
