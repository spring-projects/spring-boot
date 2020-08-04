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

import java.util.regex.Pattern;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.streams.KafkaStreamsMicrometerListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class KafkaMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(KafkaMetricsAutoConfiguration.class));

	@Test
	void whenThereIsAMeterRegistryThenMetricsListenersAreAdded() {
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class)).run((context) -> {
					assertThat(((DefaultKafkaProducerFactory<?, ?>) context.getBean(DefaultKafkaProducerFactory.class))
							.getListeners()).hasSize(1).hasOnlyElementsOfTypes(MicrometerProducerListener.class);
					assertThat(((DefaultKafkaConsumerFactory<?, ?>) context.getBean(DefaultKafkaConsumerFactory.class))
							.getListeners()).hasSize(1).hasOnlyElementsOfTypes(MicrometerConsumerListener.class);
				});
	}

	@Test
	void whenThereIsNoMeterRegistryThenListenerCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class)).run((context) -> {
			assertThat(((DefaultKafkaProducerFactory<?, ?>) context.getBean(DefaultKafkaProducerFactory.class))
					.getListeners()).isEmpty();
			assertThat(((DefaultKafkaConsumerFactory<?, ?>) context.getBean(DefaultKafkaConsumerFactory.class))
					.getListeners()).isEmpty();
		});
	}

	@Test
	void whenKafkaStreamsIsEnabledAndThereIsAMeterRegistryThenMetricsListenersAreAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
				.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
				.withPropertyValues("spring.application.name=my-test-app").with(MetricsRun.simple()).run((context) -> {
					StreamsBuilderFactoryBean streamsBuilderFactoryBean = context
							.getBean(StreamsBuilderFactoryBean.class);
					assertThat(streamsBuilderFactoryBean.getListeners()).hasSize(1)
							.hasOnlyElementsOfTypes(KafkaStreamsMicrometerListener.class);
				});
	}

	@Test
	void whenKafkaStreamsIsEnabledAndThereIsNoMeterRegistryThenListenerCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
				.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
				.withPropertyValues("spring.application.name=my-test-app").run((context) -> {
					StreamsBuilderFactoryBean streamsBuilderFactoryBean = context
							.getBean(StreamsBuilderFactoryBean.class);
					assertThat(streamsBuilderFactoryBean.getListeners()).isEmpty();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableKafkaStreams
	static class EnableKafkaStreamsConfiguration {

		@Bean
		KTable<?, ?> table(StreamsBuilder builder) {
			KStream<Object, Object> stream = builder.stream(Pattern.compile("test"));
			return stream.groupByKey().count(Materialized.as("store"));
		}

	}

}
