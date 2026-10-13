/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJackson2MixedApplication}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleJackson2MixedApplicationTests {

	@Autowired
	RestTestClient rest;

	@Test
	void jackson2IsUsedForHttpMessageConversion() {
		this.rest.get().uri("/names").exchange().expectBody().json("[\"JACKSON2:Spring:Boot\"]");
	}

	@Test
	void jackson2IsUsedForActuatorEndpoints() {
		this.rest.get().uri("/actuator/names").exchange().expectBody().json("[\"JACKSON2:Spring:Boot\"]");
	}

	@Test
	@SuppressWarnings("unchecked")
	void configPropsShouldReturnOk() {
		this.rest.get()
			.uri("/actuator/configprops")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("contexts"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void actuatorLinksShouldReturnOk() {
		this.rest.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("_links"));
	}

}
