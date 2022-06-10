/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipkinAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ZipkinAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(BytesEncoder.class));
	}

	@Test
	void shouldNotSupplyBeansIfZipkinReporterIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("zipkin2.reporter"))
				.run((context) -> assertThat(context).doesNotHaveBean(BytesEncoder.class));
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customBytesEncoder");
			assertThat(context).hasSingleBean(BytesEncoder.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(BytesEncoder.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		BytesEncoder<Span> customBytesEncoder() {
			return SpanBytesEncoder.JSON_V2;
		}

	}

}
