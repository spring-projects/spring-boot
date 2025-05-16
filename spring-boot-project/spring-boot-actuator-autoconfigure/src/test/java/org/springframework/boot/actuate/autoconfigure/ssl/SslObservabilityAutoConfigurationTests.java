/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.ssl;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslObservabilityAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class SslObservabilityAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
					SslAutoConfiguration.class, SslObservabilityAutoConfiguration.class));

	private final ApplicationContextRunner contextRunnerWithoutSslBundles = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
				CompositeMeterRegistryAutoConfiguration.class, SslObservabilityAutoConfiguration.class));

	private final ApplicationContextRunner contextRunnerWithoutMeterRegistry = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class, SslObservabilityAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(SslMeterBinder.class).hasSingleBean(SslInfo.class));
	}

	@Test
	void shouldBackOffIfSslBundlesIsMissing() {
		this.contextRunnerWithoutSslBundles
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class).doesNotHaveBean(SslInfo.class));
	}

	@Test
	void shouldBackOffIfMeterRegistryIsMissing() {
		this.contextRunnerWithoutMeterRegistry
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class).doesNotHaveBean(SslInfo.class));
	}

}
