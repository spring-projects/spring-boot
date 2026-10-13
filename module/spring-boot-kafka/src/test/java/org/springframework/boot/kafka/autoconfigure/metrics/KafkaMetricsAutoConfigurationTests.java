/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure.metrics;

import java.util.regex.Pattern;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
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
		this.contextRunner.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
			.run((context) -> {
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
			.withPropertyValues("spring.application.name=my-test-app")
			.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> {
				StreamsBuilderFactoryBean streamsBuilderFactoryBean = context.getBean(StreamsBuilderFactoryBean.class);
				assertThat(streamsBuilderFactoryBean.getListeners()).hasSize(1)
					.hasOnlyElementsOfTypes(KafkaStreamsMicrometerListener.class);
			});
	}

	@Test
	void whenKafkaStreamsIsEnabledAndThereIsNoMeterRegistryThenListenerCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
			.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.application.name=my-test-app")
			.run((context) -> {
				StreamsBuilderFactoryBean streamsBuilderFactoryBean = context.getBean(StreamsBuilderFactoryBean.class);
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
