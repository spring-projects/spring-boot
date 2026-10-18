/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the starter with Jackson 3 explicitly selected.
 *
 * @author Kristoffer Larsen Hopland
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.jersey.preferred-json-mapper=jackson")
class JerseyJackson3ApplicationTests extends AbstractJerseyApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@Override
	void actuatorStatus() {
		String uri = LocalTestWebServer.obtain(this.applicationContext).uri("/actuator/health");
		assertThat(RestClient.create().get().uri(uri).retrieve().body(String.class)).contains("\"status\":\"UP\"",
				"\"groups\":[\"liveness\",\"readiness\"]");
		assertThat(this.applicationContext.containsBean("jacksonResourceConfigCustomizer")).isTrue();
		assertThat(this.applicationContext.containsBean("jackson2ResourceConfigCustomizer")).isFalse();
	}

}
