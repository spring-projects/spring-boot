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

import org.junit.jupiter.api.Test;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.ReporterConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReporterConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurationsReporterConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BaseConfiguration.class, ReporterConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Reporter.class));
	}

	@Test
	void shouldNotSupplyReporterIfSenderIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Reporter.class));
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("customReporter");
				assertThat(context).hasSingleBean(Reporter.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	private static class SenderConfiguration {

		@Bean
		Sender sender() {
			return new NoopSender();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		Reporter<Span> customReporter() {
			return mock(Reporter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		BytesEncoder<Span> spanBytesEncoder() {
			return SpanBytesEncoder.JSON_V2;
		}

	}

}
