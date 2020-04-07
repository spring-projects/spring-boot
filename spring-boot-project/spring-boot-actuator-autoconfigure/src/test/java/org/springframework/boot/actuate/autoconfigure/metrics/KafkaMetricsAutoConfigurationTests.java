/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link KafkaMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class KafkaMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(KafkaMetricsAutoConfiguration.class));

	@Test
	void whenThereIsNoProducerFactoryAutoConfigurationBacksOff() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(KafkaClientMetrics.class));
	}

	@Test
	void whenThereIsAnAProducerFactoryKafkaClientMetricsIsConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(KafkaClientMetrics.class));
	}

	@Test
	void allowsCustomKafkaClientMetricsToBeUsed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
				.withUserConfiguration(CustomKafkaClientMetricsConfiguration.class).run((context) -> assertThat(context)
						.hasSingleBean(KafkaClientMetrics.class).hasBean("customKafkaClientMetrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomKafkaClientMetricsConfiguration {

		@Bean
		KafkaClientMetrics customKafkaClientMetrics() {
			return mock(KafkaClientMetrics.class);
		}

	}

}
