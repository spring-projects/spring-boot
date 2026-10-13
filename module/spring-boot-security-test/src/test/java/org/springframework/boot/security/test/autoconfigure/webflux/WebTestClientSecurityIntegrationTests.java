/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webflux;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webtestclient.autoconfigure.WebTestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for mocked-infrastructure-backed WebTestClient security.
 *
 * @author Andy Wilkinson
 */
class WebTestClientSecurityIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(WebTestClientAutoConfiguration.class, SecurityWebTestClientAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplySpringSecurityConfigurer() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			WebTestClient.Builder builder = (WebTestClient.Builder) ReflectionTestUtils.getField(webTestClient,
					"builder");
			assertThat(builder).isNotNull();
			WebHttpHandlerBuilder httpHandlerBuilder = (WebHttpHandlerBuilder) ReflectionTestUtils.getField(builder,
					"httpHandlerBuilder");
			assertThat(httpHandlerBuilder).isNotNull();
			List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(httpHandlerBuilder, "filters");
			assertThat(filters).isNotNull();
			assertThat(filters.get(0).getClass().getName()).isEqualTo(
					"org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers$MutatorFilter");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}

	}

}
