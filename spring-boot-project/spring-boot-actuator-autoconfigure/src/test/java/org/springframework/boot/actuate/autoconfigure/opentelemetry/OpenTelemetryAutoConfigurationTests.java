/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OpenTelemetryAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class));

	@Test
	void isRegisteredInAutoConfigurationImports() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, null).getCandidates())
			.contains(OpenTelemetryAutoConfiguration.class.getName());
	}

	@Test
	void shouldProvideBeans() {
		this.runner.run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetrySdk.class);
			assertThat(context).hasSingleBean(Resource.class);
		});
	}

	@Test
	void shouldBackOffIfOpenTelemetryIsNotOnClasspath() {
		this.runner.withClassLoader(new FilteredClassLoader("io.opentelemetry")).run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetrySdk.class);
			assertThat(context).doesNotHaveBean(Resource.class);
		});
	}

	@Test
	void backsOffOnUserSuppliedBeans() {
		this.runner.withUserConfiguration(UserConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasBean("customOpenTelemetry");
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).hasBean("customResource");
		});
	}

	@Test
	void shouldApplySpringApplicationNameToResource() {
		this.runner.withPropertyValues("spring.application.name=my-application").run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap())
				.contains(entry(ResourceAttributes.SERVICE_NAME, "my-application"));
		});
	}

	@Test
	void shouldFallbackToDefaultApplicationNameIfSpringApplicationNameIsNotSet() {
		this.runner.run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap())
				.contains(entry(ResourceAttributes.SERVICE_NAME, "application"));
		});
	}

	@Test
	void shouldApplyResourceAttributesFromProperties() {
		this.runner.withPropertyValues("management.opentelemetry.resource-attributes.region=us-west").run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap()).contains(entry(AttributeKey.stringKey("region"), "us-west"));
		});
	}

	@Test
	void shouldRegisterSdkTracerProviderIfAvailable() {
		this.runner.withBean(SdkTracerProvider.class, () -> SdkTracerProvider.builder().build()).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getTracerProvider()).isNotNull();
		});
	}

	@Test
	void shouldRegisterContextPropagatorsIfAvailable() {
		this.runner.withBean(ContextPropagators.class, ContextPropagators::noop).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getPropagators()).isNotNull();
		});
	}

	@Test
	void shouldRegisterSdkLoggerProviderIfAvailable() {
		this.runner.withBean(SdkLoggerProvider.class, () -> SdkLoggerProvider.builder().build()).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getLogsBridge()).isNotNull();
		});
	}

	@Test
	void shouldRegisterSdkMeterProviderIfAvailable() {
		this.runner.withBean(SdkMeterProvider.class, () -> SdkMeterProvider.builder().build()).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getMeterProvider()).isNotNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static class UserConfiguration {

		@Bean
		OpenTelemetry customOpenTelemetry() {
			return mock(OpenTelemetry.class);
		}

		@Bean
		Resource customResource() {
			return Resource.getDefault();
		}

	}

}
