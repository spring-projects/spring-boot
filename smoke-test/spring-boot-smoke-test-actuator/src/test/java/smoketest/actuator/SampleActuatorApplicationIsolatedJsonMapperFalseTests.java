/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for WebMVC actuator when not using an isolated {@link JsonMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.jackson.isolated-json-mapper=false",
				"spring.jackson.mapper.require-setters-for-getters=true" })
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
@AutoConfigureTestRestTemplate
class SampleActuatorApplicationIsolatedJsonMapperFalseTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void bodyIsEmptyDueToMainJsonMapperRequiringSettersForGetters() {
		ResponseEntity<String> entity = this.testRestTemplate.withBasicAuth("user", "password")
			.getForEntity("/actuator/startup", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{}");
	}

}
