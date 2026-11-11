/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.test;

import org.junit.jupiter.api.Test;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} configured with
 * {@link WebEnvironment#RANDOM_PORT}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "value=123" })
@SuppressWarnings("removal")
class SpringBootTestWebEnvironmentRandomPortTests extends AbstractSpringBootTestWebServerWebEnvironmentTests {

	@Test
	void testRestTemplateShouldUseBuilder() {
		assertThat(getRestTemplate().getRestTemplate().getMessageConverters())
			.hasAtLeastOneElementOfType(MyConverter.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@RestController
	static class Config extends AbstractConfig {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().additionalMessageConverters(new MyConverter());
		}

	}

	static class MyConverter extends StringHttpMessageConverter {

	}

}
