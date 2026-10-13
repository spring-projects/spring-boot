/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure.tester;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GraphQlTesterAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlTesterAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, GraphQlTesterAutoConfiguration.class));

	@Test
	void shouldNotContributeTesterIfGraphQlServiceNotPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(GraphQlTester.class));
	}

	@Test
	void shouldContributeTester() {
		this.contextRunner.withUserConfiguration(CustomGraphQlServiceConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed().hasSingleBean(GraphQlTester.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGraphQlServiceConfiguration {

		@Bean
		ExecutionGraphQlService graphQlService() {
			return mock(ExecutionGraphQlService.class);
		}

	}

}
