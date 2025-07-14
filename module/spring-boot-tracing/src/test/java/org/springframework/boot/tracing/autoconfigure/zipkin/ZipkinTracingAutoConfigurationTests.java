/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.tracing.autoconfigure.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesEncoder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipkinTracingAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ZipkinTracingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ZipkinTracingAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfInfrastructureIsNotAvailable() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(BytesEncoder.class)
			.doesNotHaveBean(SpanExporter.class)
			.doesNotHaveBean(ZipkinSpanExporter.class));
	}

	@Test
	void shouldSupplyBeansIfInfrastructureIsAvailable() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ZipkinAutoConfiguration.class)).run((context) -> {
			assertThat(context).hasSingleBean(SpanExporter.class);
			assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
			.withConfiguration(AutoConfigurations.of(ZipkinAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(SpanExporter.class);
				assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class);
			});
	}

}
