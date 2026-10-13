/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} (i.e. full integration
 * test).
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(properties = { "spring.main.web-application-type=reactive", "debug=true" },
		classes = ExampleWebTestClientApplication.class)
@AutoConfigureWebTestClient
@EnableWebFlux
@Import({ ExampleController1.class, ExampleController2.class })
class WebTestClientSpringBootTestIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldFindController1() {
		this.webClient.get().uri("/one").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("one");
	}

	@Test
	void shouldFindController2() {
		this.webClient.get().uri("/two").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("two");
	}

	@Test
	void shouldHaveRealService() {
		assertThat(this.applicationContext.getBeansOfType(ExampleRealService.class)).hasSize(1);
	}

}
