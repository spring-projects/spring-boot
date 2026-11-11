/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.test;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} in a reactive environment configured
 * with a user-defined {@link RestTemplate} that is named {@code testRestTemplate}.
 *
 * @author Madhura Bhave
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.main.web-application-type=reactive", "value=123" })
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class SpringBootTestReactiveWebEnvironmentUserDefinedTestRestTemplateTests
		extends AbstractSpringBootTestEmbeddedReactiveWebEnvironmentTests {

	@Test
	void restTemplateIsUserDefined() {
		assertThat(getContext().getBean("testRestTemplate")).isInstanceOf(RestTemplate.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebFlux
	@RestController
	static class Config extends AbstractConfig {

		@Bean
		RestTemplate testRestTemplate() {
			return new RestTemplate();
		}

	}

}
