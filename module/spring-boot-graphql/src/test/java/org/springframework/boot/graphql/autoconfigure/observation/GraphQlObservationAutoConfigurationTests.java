/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.observation;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.observation.DefaultDataFetcherObservationConvention;
import org.springframework.graphql.observation.DefaultDataLoaderObservationConvention;
import org.springframework.graphql.observation.DefaultExecutionRequestObservationConvention;
import org.springframework.graphql.observation.GraphQlObservationInstrumentation;
import org.springframework.graphql.server.WebGraphQlHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GraphQlObservationAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(TestObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(GraphQlObservationAutoConfiguration.class));

	@Test
	void backsOffWhenObservationRegistryIsMissing() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GraphQlObservationAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GraphQlObservationInstrumentation.class));
	}

	@Test
	void definesInstrumentationWhenObservationRegistryIsPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GraphQlObservationInstrumentation.class));
	}

	@Test
	void instrumentationBacksOffIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(InstrumentationConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(GraphQlObservationInstrumentation.class)
				.hasBean("customInstrumentation"));
	}

	@Test
	void instrumentationUsesCustomConventionsIfAvailable() {
		this.contextRunner.withUserConfiguration(CustomConventionsConfiguration.class).run((context) -> {
			GraphQlObservationInstrumentation instrumentation = context
				.getBean(GraphQlObservationInstrumentation.class);
			assertThat(instrumentation).extracting("requestObservationConvention")
				.isInstanceOf(CustomExecutionRequestObservationConvention.class);
			assertThat(instrumentation).extracting("dataFetcherObservationConvention")
				.isInstanceOf(CustomDataFetcherObservationConvention.class);
			assertThat(instrumentation).extracting("dataLoaderObservationConvention")
				.isInstanceOf(CustomDataLoaderObservationConvention.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class InstrumentationConfiguration {

		@Bean
		GraphQlObservationInstrumentation customInstrumentation(ObservationRegistry registry) {
			return new GraphQlObservationInstrumentation(registry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConventionsConfiguration {

		@Bean
		CustomExecutionRequestObservationConvention customExecutionConvention() {
			return new CustomExecutionRequestObservationConvention();
		}

		@Bean
		CustomDataFetcherObservationConvention customDataFetcherConvention() {
			return new CustomDataFetcherObservationConvention();
		}

		@Bean
		CustomDataLoaderObservationConvention customDataLoaderConvention() {
			return new CustomDataLoaderObservationConvention();
		}

	}

	static class CustomExecutionRequestObservationConvention extends DefaultExecutionRequestObservationConvention {

	}

	static class CustomDataFetcherObservationConvention extends DefaultDataFetcherObservationConvention {

	}

	static class CustomDataLoaderObservationConvention extends DefaultDataLoaderObservationConvention {

	}

	@Configuration(proxyBeanMethods = false)
	static class WebGraphQlConfiguration {

		@Bean
		WebGraphQlHandler webGraphQlHandler() {
			return mock(WebGraphQlHandler.class);
		}

	}

}
