/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} timeout property
 * binding.
 *
 * @author Jay Choi
 * @author Andy Wilkinson
 */
@SpringBootTest(properties = { "spring.main.web-application-type=reactive" },
		classes = ExampleWebTestClientApplication.class)
class WebTestClientTimeoutSpringBootTestIntegrationTests {

	@Nested
	@EnableWebFlux
	@AutoConfigureWebTestClient
	@TestPropertySource(properties = "spring.test.webtestclient.timeout=30s")
	class TimeoutFromProperty {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(30));
		}

	}

	@Nested
	@EnableWebFlux
	@AutoConfigureWebTestClient(timeout = "25s")
	class TimeoutFromAnnotation {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(25));
		}

	}

	@Nested
	@EnableWebFlux
	@AutoConfigureWebTestClient
	class DefaultTimeout {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(5));
		}

	}

}
