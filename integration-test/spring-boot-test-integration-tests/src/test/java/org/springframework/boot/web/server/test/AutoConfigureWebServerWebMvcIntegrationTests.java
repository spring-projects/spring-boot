/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.test;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.test.AutoConfigureWebServerWebMvcIntegrationTests.TestController;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureWebServer @AutoConfigureWebServer} and Spring
 * MVC.
 *
 * @author Phillip Webb
 */
@SpringBootTest(classes = TestController.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServer
@AutoConfigureWebMvc
@AutoConfigureRestTestClient
class AutoConfigureWebServerWebMvcIntegrationTests {

	@Test
	void embeddedWebServerWithSpringMvcIsAvailable(@Autowired RestTestClient restClient) {
		assertThat(RestTestClientResponse.from(restClient.get().uri("/hello").exchange())).hasStatusOk()
			.bodyText()
			.isEqualTo("hello");
	}

	@RestController
	static class TestController {

		@RequestMapping("/hello")
		String hello() {
			return "hello";
		}

	}

}
