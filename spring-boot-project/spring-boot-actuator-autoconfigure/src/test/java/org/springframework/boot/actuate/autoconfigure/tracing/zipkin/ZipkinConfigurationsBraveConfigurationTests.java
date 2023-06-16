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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import brave.handler.SpanHandler;
import org.junit.jupiter.api.Test;
import zipkin2.Span;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.BraveConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BraveConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurationsBraveConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BraveConfiguration.class));

	private final ApplicationContextRunner tracingDisabledContextRunner = this.contextRunner
		.withPropertyValues("management.tracing.enabled=false");

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(ReporterConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ZipkinSpanHandler.class));
	}

	@Test
	void shouldNotSupplySpanHandlerIfReporterIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanHandler.class));
	}

	@Test
	void shouldNotSupplyIfZipkinReporterBraveIsNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("zipkin2.reporter.brave"))
			.withUserConfiguration(ReporterConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanHandler.class));

	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(ReporterConfiguration.class, CustomConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("customZipkinSpanHandler");
				assertThat(context).hasSingleBean(ZipkinSpanHandler.class);
			});
	}

	@Test
	void shouldSupplyZipkinSpanHandlerWithCustomSpanHandler() {
		this.contextRunner.withUserConfiguration(ReporterConfiguration.class, CustomSpanHandlerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("customSpanHandler");
				assertThat(context).hasSingleBean(ZipkinSpanHandler.class);
			});
	}

	@Test
	void shouldNotSupplyZipkinSpanHandlerIfTracingIsDisabled() {
		this.tracingDisabledContextRunner.withUserConfiguration(ReporterConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanHandler.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static class ReporterConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		Reporter<Span> reporter() {
			return mock(Reporter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		ZipkinSpanHandler customZipkinSpanHandler() {
			return (ZipkinSpanHandler) ZipkinSpanHandler.create(mock(Reporter.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomSpanHandlerConfiguration {

		@Bean
		SpanHandler customSpanHandler() {
			return mock(SpanHandler.class);
		}

	}

}
