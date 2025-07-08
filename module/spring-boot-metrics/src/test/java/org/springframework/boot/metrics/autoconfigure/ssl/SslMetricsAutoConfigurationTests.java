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

package org.springframework.boot.metrics.autoconfigure.ssl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslMetricsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class SslMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslMetricsAutoConfiguration.class));

	@Test
	void shouldSupplyMeterBinder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
			.withBean(SimpleMeterRegistry.class)
			.run((context) -> assertThat(context).hasSingleBean(SslMeterBinder.class));
	}

	@Test
	void shouldBackOffIfSslBundlesIsMissing() {
		this.contextRunner.withBean(SimpleMeterRegistry.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class));
	}

	@Test
	void shouldBackOffIfMeterRegistryIsMissing() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class));
	}

}
