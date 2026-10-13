/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureRestTestClient
class SampleActuatorExtensionApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void healthActuatorIsNotExposed() {
		this.restTestClient.get().uri("/actuator/health").exchange().expectStatus().isNotFound();
	}

	@Test
	void healthExtensionWithAuthHeaderIsDenied() {
		this.restTestClient.get().uri("/myextension/health").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void healthExtensionWithAuthHeader() {
		RestTestClient restTestClient = this.restTestClient.mutate()
			.defaultHeader("Authorization", "Bearer secret")
			.build();
		restTestClient.get().uri("/myextension/health").exchange().expectStatus().isOk();
	}

}
