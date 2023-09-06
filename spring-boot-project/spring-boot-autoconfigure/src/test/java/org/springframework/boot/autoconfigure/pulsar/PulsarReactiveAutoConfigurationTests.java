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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.reactive.client.adapter.ProducerCacheProvider;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderCache;
import org.apache.pulsar.reactive.client.api.ReactivePulsarClient;
import org.apache.pulsar.reactive.client.producercache.CaffeineShadedProducerCacheProvider;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.ReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.ReactivePulsarListenerEndpointRegistry;
import org.springframework.pulsar.reactive.config.annotation.ReactivePulsarBootstrapConfiguration;
import org.springframework.pulsar.reactive.config.annotation.ReactivePulsarListenerAnnotationBeanPostProcessor;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactiveMessageConsumerBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageReaderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageSenderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarTemplate;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarReactiveAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @author Phillip Webb
 */
class PulsarReactiveAutoConfigurationTests {

	private static final String INTERNAL_PULSAR_LISTENER_ANNOTATION_PROCESSOR = "org.springframework.pulsar.config.internalReactivePulsarListenerAnnotationProcessor";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PulsarReactiveAutoConfiguration.class))
		.withBean(PulsarClient.class, () -> mock(PulsarClient.class));

	@Test
	void whenPulsarNotOnClasspathAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarReactiveAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(PulsarClient.class))
			.run((context) -> assertThat(context).doesNotHaveBean(PulsarReactiveAutoConfiguration.class));
	}

	@Test
	void whenReactivePulsarNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ReactivePulsarClient.class))
			.run((context) -> assertThat(context).doesNotHaveBean(PulsarReactiveAutoConfiguration.class));
	}

	@Test
	void whenReactiveSpringPulsarNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ReactivePulsarTemplate.class))
			.run((context) -> assertThat(context).doesNotHaveBean(PulsarReactiveAutoConfiguration.class));
	}

	@Test
	void whenCustomPulsarListenerAnnotationProcessorDefinedAutoConfigurationIsSkipped() {
		this.contextRunner.withBean(INTERNAL_PULSAR_LISTENER_ANNOTATION_PROCESSOR, String.class, () -> "bean")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactivePulsarBootstrapConfiguration.class));
	}

	@Test
	void autoConfiguresBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(PulsarConfiguration.class)
			.hasSingleBean(PulsarClient.class)
			.hasSingleBean(PulsarAdministration.class)
			.hasSingleBean(DefaultSchemaResolver.class)
			.hasSingleBean(DefaultTopicResolver.class)
			.hasSingleBean(ReactivePulsarClient.class)
			.hasSingleBean(CaffeineShadedProducerCacheProvider.class)
			.hasSingleBean(ReactiveMessageSenderCache.class)
			.hasSingleBean(DefaultReactivePulsarSenderFactory.class)
			.hasSingleBean(ReactivePulsarTemplate.class)
			.hasSingleBean(DefaultReactivePulsarConsumerFactory.class)
			.hasSingleBean(DefaultReactivePulsarListenerContainerFactory.class)
			.hasSingleBean(ReactivePulsarListenerAnnotationBeanPostProcessor.class)
			.hasSingleBean(ReactivePulsarListenerEndpointRegistry.class));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void injectsExpectedBeansIntoReactivePulsarClient() {
		this.contextRunner.run((context) -> {
			PulsarClient pulsarClient = context.getBean(PulsarClient.class);
			assertThat(context).hasNotFailed()
				.getBean(ReactivePulsarClient.class)
				.extracting("reactivePulsarResourceAdapter")
				.extracting("pulsarClientSupplier", InstanceOfAssertFactories.type(Supplier.class))
				.extracting(Supplier::get)
				.isSameAs(pulsarClient);
		});
	}

	@ParameterizedTest
	@ValueSource(classes = { ReactivePulsarClient.class, ProducerCacheProvider.class, ReactiveMessageSenderCache.class,
			ReactivePulsarSenderFactory.class, ReactivePulsarConsumerFactory.class, ReactivePulsarReaderFactory.class,
			ReactivePulsarTemplate.class })
	<T> void whenHasUserDefinedBeanDoesNotAutoConfigureBean(Class<T> beanClass) {
		T bean = mock(beanClass);
		this.contextRunner.withBean(beanClass.getName(), beanClass, () -> bean)
			.run((context) -> assertThat(context).getBean(beanClass).isSameAs(bean));
	}

	@Nested
	class SenderFactoryTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		void injectsExpectedBeans() {
			ReactivePulsarClient client = mock(ReactivePulsarClient.class);
			ReactiveMessageSenderCache cache = mock(ReactiveMessageSenderCache.class);
			this.contextRunner.withPropertyValues("spring.pulsar.producer.topic-name=test-topic")
				.withBean("customReactivePulsarClient", ReactivePulsarClient.class, () -> client)
				.withBean("customReactiveMessageSenderCache", ReactiveMessageSenderCache.class, () -> cache)
				.run((context) -> {
					DefaultReactivePulsarSenderFactory<?> senderFactory = context
						.getBean(DefaultReactivePulsarSenderFactory.class);
					assertThat(senderFactory)
						.extracting("reactivePulsarClient", InstanceOfAssertFactories.type(ReactivePulsarClient.class))
						.isSameAs(client);
					assertThat(senderFactory)
						.extracting("reactiveMessageSenderCache",
								InstanceOfAssertFactories.type(ReactiveMessageSenderCache.class))
						.isSameAs(cache);
					assertThat(senderFactory)
						.extracting("topicResolver", InstanceOfAssertFactories.type(TopicResolver.class))
						.isSameAs(context.getBean(TopicResolver.class));
				});
		}

		@Test
		void injectsExpectedBeansIntoReactiveMessageSenderCache() {
			ProducerCacheProvider provider = mock(ProducerCacheProvider.class);
			this.contextRunner.withBean("customProducerCacheProvider", ProducerCacheProvider.class, () -> provider)
				.run((context) -> assertThat(context).getBean(ReactiveMessageSenderCache.class)
					.extracting("cacheProvider", InstanceOfAssertFactories.type(ProducerCacheProvider.class))
					.isSameAs(provider));
		}

		@Test
		<T> void whenHasUserDefinedCustomizersAppliesInCorrectOrder() {
			this.contextRunner.withPropertyValues("spring.pulsar.producer.name=fromPropsCustomizer")
				.withUserConfiguration(ReactiveMessageSenderBuilderCustomizerConfig.class)
				.run((context) -> {
					DefaultReactivePulsarSenderFactory<?> producerFactory = context
						.getBean(DefaultReactivePulsarSenderFactory.class);
					Customizers<ReactiveMessageSenderBuilderCustomizer<T>, ReactiveMessageSenderBuilder<T>> customizers = Customizers
						.of(ReactiveMessageSenderBuilder.class, ReactiveMessageSenderBuilderCustomizer::customize);
					assertThat(customizers.fromField(producerFactory, "defaultConfigCustomizers")).callsInOrder(
							ReactiveMessageSenderBuilder::producerName, "fromPropsCustomizer", "fromCustomizer1",
							"fromCustomizer2");
				});
		}

		@TestConfiguration(proxyBeanMethods = false)
		static class ReactiveMessageSenderBuilderCustomizerConfig {

			@Bean
			@Order(200)
			ReactiveMessageSenderBuilderCustomizer<?> customizerFoo() {
				return (builder) -> builder.producerName("fromCustomizer2");
			}

			@Bean
			@Order(100)
			ReactiveMessageSenderBuilderCustomizer<?> customizerBar() {
				return (builder) -> builder.producerName("fromCustomizer1");
			}

		}

	}

	@Nested
	class TemplateTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		@SuppressWarnings("rawtypes")
		void injectsExpectedBeans() {
			ReactivePulsarSenderFactory senderFactory = mock(ReactivePulsarSenderFactory.class);
			SchemaResolver schemaResolver = mock(SchemaResolver.class);
			this.contextRunner
				.withBean("customReactivePulsarSenderFactory", ReactivePulsarSenderFactory.class, () -> senderFactory)
				.withBean("schemaResolver", SchemaResolver.class, () -> schemaResolver)
				.run((context) -> assertThat(context).getBean(ReactivePulsarTemplate.class).satisfies((template) -> {
					assertThat(template).extracting("reactiveMessageSenderFactory").isSameAs(senderFactory);
					assertThat(template).extracting("schemaResolver").isSameAs(schemaResolver);
				}));
		}

	}

	@Nested
	class ConsumerFactoryTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		void injectsExpectedBeans() {
			ReactivePulsarClient client = mock(ReactivePulsarClient.class);
			this.contextRunner.withBean("customReactivePulsarClient", ReactivePulsarClient.class, () -> client)
				.run((context) -> {
					ReactivePulsarConsumerFactory<?> consumerFactory = context
						.getBean(DefaultReactivePulsarConsumerFactory.class);
					assertThat(consumerFactory)
						.extracting("reactivePulsarClient", InstanceOfAssertFactories.type(ReactivePulsarClient.class))
						.isSameAs(client);
				});
		}

		@Test
		<T> void whenHasUserDefinedCustomizersAppliesInCorrectOrder() {
			this.contextRunner.withPropertyValues("spring.pulsar.consumer.name=fromPropsCustomizer")
				.withUserConfiguration(ReactiveMessageConsumerBuilderCustomizerConfig.class)
				.run((context) -> {
					DefaultReactivePulsarConsumerFactory<?> consumerFactory = context
						.getBean(DefaultReactivePulsarConsumerFactory.class);
					Customizers<ReactiveMessageConsumerBuilderCustomizer<T>, ReactiveMessageConsumerBuilder<T>> customizers = Customizers
						.of(ReactiveMessageConsumerBuilder.class, ReactiveMessageConsumerBuilderCustomizer::customize);
					assertThat(customizers.fromField(consumerFactory, "defaultConfigCustomizers")).callsInOrder(
							ReactiveMessageConsumerBuilder::consumerName, "fromPropsCustomizer", "fromCustomizer1",
							"fromCustomizer2");
				});
		}

		@TestConfiguration(proxyBeanMethods = false)
		static class ReactiveMessageConsumerBuilderCustomizerConfig {

			@Bean
			@Order(200)
			ReactiveMessageConsumerBuilderCustomizer<?> customizerFoo() {
				return (builder) -> builder.consumerName("fromCustomizer2");
			}

			@Bean
			@Order(100)
			ReactiveMessageConsumerBuilderCustomizer<?> customizerBar() {
				return (builder) -> builder.consumerName("fromCustomizer1");
			}

		}

	}

	@Nested
	class ListenerTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			ReactivePulsarListenerContainerFactory<?> listenerContainerFactory = mock(
					ReactivePulsarListenerContainerFactory.class);
			this.contextRunner
				.withBean("reactivePulsarListenerContainerFactory", ReactivePulsarListenerContainerFactory.class,
						() -> listenerContainerFactory)
				.run((context) -> assertThat(context).getBean(ReactivePulsarListenerContainerFactory.class)
					.isSameAs(listenerContainerFactory));
		}

		@Test
		void whenHasUserDefinedReactivePulsarListenerAnnotationBeanPostProcessorDoesNotAutoConfigureBean() {
			ReactivePulsarListenerAnnotationBeanPostProcessor<?> listenerAnnotationBeanPostProcessor = mock(
					ReactivePulsarListenerAnnotationBeanPostProcessor.class);
			this.contextRunner.withBean(INTERNAL_PULSAR_LISTENER_ANNOTATION_PROCESSOR,
					ReactivePulsarListenerAnnotationBeanPostProcessor.class, () -> listenerAnnotationBeanPostProcessor)
				.run((context) -> assertThat(context).getBean(ReactivePulsarListenerAnnotationBeanPostProcessor.class)
					.isSameAs(listenerAnnotationBeanPostProcessor));
		}

		@Test
		void whenHasCustomProperties() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.listener.schema-type=avro");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new)).run((context) -> {
				DefaultReactivePulsarListenerContainerFactory<?> factory = context
					.getBean(DefaultReactivePulsarListenerContainerFactory.class);
				assertThat(factory.getContainerProperties().getSchemaType()).isEqualTo(SchemaType.AVRO);
			});
		}

		@Test
		void injectsExpectedBeans() {
			ReactivePulsarConsumerFactory<?> consumerFactory = mock(ReactivePulsarConsumerFactory.class);
			SchemaResolver schemaResolver = mock(SchemaResolver.class);
			this.contextRunner
				.withBean("customReactivePulsarConsumerFactory", ReactivePulsarConsumerFactory.class,
						() -> consumerFactory)
				.withBean("schemaResolver", SchemaResolver.class, () -> schemaResolver)
				.run((context) -> {
					DefaultReactivePulsarListenerContainerFactory<?> containerFactory = context
						.getBean(DefaultReactivePulsarListenerContainerFactory.class);
					assertThat(containerFactory).extracting("consumerFactory").isSameAs(consumerFactory);
					assertThat(containerFactory)
						.extracting(DefaultReactivePulsarListenerContainerFactory::getContainerProperties)
						.extracting(ReactivePulsarContainerProperties::getSchemaResolver)
						.isSameAs(schemaResolver);
				});
		}

	}

	@Nested
	class ReaderFactoryTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		void injectsExpectedBeans() {
			ReactivePulsarClient client = mock(ReactivePulsarClient.class);
			this.contextRunner.withPropertyValues("spring.pulsar.reader.name=test-reader")
				.withBean("customReactivePulsarClient", ReactivePulsarClient.class, () -> client)
				.run((context) -> {
					DefaultReactivePulsarReaderFactory<?> readerFactory = context
						.getBean(DefaultReactivePulsarReaderFactory.class);
					assertThat(readerFactory)
						.extracting("reactivePulsarClient", InstanceOfAssertFactories.type(ReactivePulsarClient.class))
						.isSameAs(client);
				});
		}

		@Test
		<T> void whenHasUserDefinedCustomizersAppliesInCorrectOrder() {
			this.contextRunner.withPropertyValues("spring.pulsar.reader.name=fromPropsCustomizer")
				.withUserConfiguration(ReactiveMessageReaderBuilderCustomizerConfig.class)
				.run((context) -> {
					DefaultReactivePulsarReaderFactory<?> readerFactory = context
						.getBean(DefaultReactivePulsarReaderFactory.class);
					Customizers<ReactiveMessageReaderBuilderCustomizer<T>, ReactiveMessageReaderBuilder<T>> customizers = Customizers
						.of(ReactiveMessageReaderBuilder.class, ReactiveMessageReaderBuilderCustomizer::customize);
					assertThat(customizers.fromField(readerFactory, "defaultConfigCustomizers")).callsInOrder(
							ReactiveMessageReaderBuilder::readerName, "fromPropsCustomizer", "fromCustomizer1",
							"fromCustomizer2");
				});
		}

		@TestConfiguration(proxyBeanMethods = false)
		static class ReactiveMessageReaderBuilderCustomizerConfig {

			@Bean
			@Order(200)
			ReactiveMessageReaderBuilderCustomizer<?> customizerFoo() {
				return (builder) -> builder.readerName("fromCustomizer2");
			}

			@Bean
			@Order(100)
			ReactiveMessageReaderBuilderCustomizer<?> customizerBar() {
				return (builder) -> builder.readerName("fromCustomizer1");
			}

		}

	}

	@Nested
	class SenderCacheAutoConfigurationTests {

		private final ApplicationContextRunner contextRunner = PulsarReactiveAutoConfigurationTests.this.contextRunner;

		@Test
		void whenNoPropertiesEnablesCaching() {
			this.contextRunner.run(this::assertCaffeineProducerCacheProvider);
		}

		@Test
		void whenCachingEnabledEnablesCaching() {
			this.contextRunner.withPropertyValues("spring.pulsar.producer.cache.enabled=true")
				.run(this::assertCaffeineProducerCacheProvider);
		}

		@Test
		void whenCachingDisabledDoesNotEnableCaching() {
			this.contextRunner.withPropertyValues("spring.pulsar.producer.cache.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(ProducerCacheProvider.class)
					.doesNotHaveBean(ReactiveMessageSenderCache.class));
		}

		@Test
		void whenCachingEnabledAndCaffeineNotOnClasspathStillUsesCaffeine() {
			// The reactive client shades Caffeine - it should still be used
			this.contextRunner.withClassLoader(new FilteredClassLoader(Caffeine.class))
				.withPropertyValues("spring.pulsar.producer.cache.enabled=true")
				.run(this::assertCaffeineProducerCacheProvider);
		}

		@Test
		void whenCachingEnabledAndNoCacheProviderAvailable() {
			// The reactive client uses a shaded caffeine cache provider as its internal
			// cache
			this.contextRunner.withClassLoader(new FilteredClassLoader(CaffeineShadedProducerCacheProvider.class))
				.withPropertyValues("spring.pulsar.producer.cache.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean(ProducerCacheProvider.class)
					.getBean(ReactiveMessageSenderCache.class)
					.extracting("cacheProvider")
					.isExactlyInstanceOf(CaffeineShadedProducerCacheProvider.class));
		}

		@Test
		void whenCustomCachingPropertiesCreatesConfiguredBean() {
			this.contextRunner
				.withPropertyValues("spring.pulsar.producer.cache.expire-after-access=100s",
						"spring.pulsar.producer.cache.maximum-size=5150",
						"spring.pulsar.producer.cache.initial-capacity=200")
				.run((context) -> assertCaffeineProducerCacheProvider(context).extracting("cache.cache")
					.hasFieldOrPropertyWithValue("expiresAfterAccessNanos", Duration.ofSeconds(100).toNanos())
					.hasFieldOrPropertyWithValue("maximum", 5150L));
		}

		private AbstractObjectAssert<?, ProducerCacheProvider> assertCaffeineProducerCacheProvider(
				AssertableApplicationContext context) {
			return assertThat(context).hasSingleBean(ReactiveMessageSenderCache.class)
				.getBean(ProducerCacheProvider.class)
				.isExactlyInstanceOf(CaffeineShadedProducerCacheProvider.class);
		}

	}

}
