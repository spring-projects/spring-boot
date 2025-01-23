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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.reactive.client.adapter.AdaptedReactivePulsarClientFactory;
import org.apache.pulsar.reactive.client.adapter.ProducerCacheProvider;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderCache;
import org.apache.pulsar.reactive.client.api.ReactivePulsarClient;
import org.apache.pulsar.reactive.client.producercache.CaffeineShadedProducerCacheProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.PulsarTopicBuilder;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.annotation.EnableReactivePulsar;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarSenderFactory.Builder;
import org.springframework.pulsar.reactive.core.ReactiveMessageConsumerBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageReaderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageSenderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarTemplate;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring for Apache Pulsar
 * Reactive.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @since 3.2.0
 */
@AutoConfiguration(after = PulsarAutoConfiguration.class)
@ConditionalOnClass({ PulsarClient.class, ReactivePulsarClient.class, ReactivePulsarTemplate.class })
@Import(PulsarConfiguration.class)
public class PulsarReactiveAutoConfiguration {

	private final PulsarProperties properties;

	private final PulsarReactivePropertiesMapper propertiesMapper;

	PulsarReactiveAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarReactivePropertiesMapper(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarClient reactivePulsarClient(PulsarClient pulsarClient) {
		return AdaptedReactivePulsarClientFactory.create(pulsarClient);
	}

	@Bean
	@ConditionalOnMissingBean(ProducerCacheProvider.class)
	@ConditionalOnClass(CaffeineShadedProducerCacheProvider.class)
	@ConditionalOnBooleanProperty(name = "spring.pulsar.producer.cache.enabled", matchIfMissing = true)
	CaffeineShadedProducerCacheProvider reactivePulsarProducerCacheProvider() {
		PulsarProperties.Producer.Cache properties = this.properties.getProducer().getCache();
		return new CaffeineShadedProducerCacheProvider(properties.getExpireAfterAccess(), Duration.ofMinutes(10),
				properties.getMaximumSize(), properties.getInitialCapacity());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.pulsar.producer.cache.enabled", matchIfMissing = true)
	ReactiveMessageSenderCache reactivePulsarMessageSenderCache(
			ObjectProvider<ProducerCacheProvider> producerCacheProvider) {
		return reactivePulsarMessageSenderCache(producerCacheProvider.getIfAvailable());
	}

	private ReactiveMessageSenderCache reactivePulsarMessageSenderCache(ProducerCacheProvider producerCacheProvider) {
		return (producerCacheProvider != null) ? AdaptedReactivePulsarClientFactory.createCache(producerCacheProvider)
				: AdaptedReactivePulsarClientFactory.createCache();
	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarSenderFactory.class)
	DefaultReactivePulsarSenderFactory<?> reactivePulsarSenderFactory(ReactivePulsarClient reactivePulsarClient,
			ObjectProvider<ReactiveMessageSenderCache> reactiveMessageSenderCache, TopicResolver topicResolver,
			ObjectProvider<ReactiveMessageSenderBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ReactiveMessageSenderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageSenderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageSenderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageSenderBuilderCustomizers(customizers, builder));
		Builder<Object> senderFactoryBuilder = DefaultReactivePulsarSenderFactory.builderFor(reactivePulsarClient)
			.withDefaultConfigCustomizers(lambdaSafeCustomizers)
			.withMessageSenderCache(reactiveMessageSenderCache.getIfAvailable())
			.withTopicResolver(topicResolver);
		topicBuilderProvider.ifAvailable(senderFactoryBuilder::withTopicBuilder);
		return senderFactoryBuilder.build();
	}

	@SuppressWarnings("unchecked")
	private void applyMessageSenderBuilderCustomizers(List<ReactiveMessageSenderBuilderCustomizer<?>> customizers,
			ReactiveMessageSenderBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageSenderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarConsumerFactory.class)
	DefaultReactivePulsarConsumerFactory<?> reactivePulsarConsumerFactory(
			ReactivePulsarClient pulsarReactivePulsarClient,
			ObjectProvider<ReactiveMessageConsumerBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ReactiveMessageConsumerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageConsumerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageConsumerBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageConsumerBuilderCustomizers(customizers, builder));
		DefaultReactivePulsarConsumerFactory<?> consumerFactory = new DefaultReactivePulsarConsumerFactory<>(
				pulsarReactivePulsarClient, lambdaSafeCustomizers);
		topicBuilderProvider.ifAvailable(consumerFactory::setTopicBuilder);
		return consumerFactory;
	}

	@SuppressWarnings("unchecked")
	private void applyMessageConsumerBuilderCustomizers(List<ReactiveMessageConsumerBuilderCustomizer<?>> customizers,
			ReactiveMessageConsumerBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageConsumerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(name = "reactivePulsarListenerContainerFactory")
	DefaultReactivePulsarListenerContainerFactory<?> reactivePulsarListenerContainerFactory(
			ReactivePulsarConsumerFactory<Object> reactivePulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver, PulsarContainerFactoryCustomizers containerFactoryCustomizers) {
		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		this.propertiesMapper.customizeContainerProperties(containerProperties);
		DefaultReactivePulsarListenerContainerFactory<?> containerFactory = new DefaultReactivePulsarListenerContainerFactory<>(
				reactivePulsarConsumerFactory, containerProperties);
		containerFactoryCustomizers.customize(containerFactory);
		return containerFactory;
	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarReaderFactory.class)
	DefaultReactivePulsarReaderFactory<?> reactivePulsarReaderFactory(ReactivePulsarClient reactivePulsarClient,
			ObjectProvider<ReactiveMessageReaderBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ReactiveMessageReaderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageReaderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageReaderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageReaderBuilderCustomizers(customizers, builder));
		DefaultReactivePulsarReaderFactory<?> readerFactory = new DefaultReactivePulsarReaderFactory<>(
				reactivePulsarClient, lambdaSafeCustomizers);
		topicBuilderProvider.ifAvailable(readerFactory::setTopicBuilder);
		return readerFactory;
	}

	@SuppressWarnings("unchecked")
	private void applyMessageReaderBuilderCustomizers(List<ReactiveMessageReaderBuilderCustomizer<?>> customizers,
			ReactiveMessageReaderBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageReaderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarTemplate<?> pulsarReactiveTemplate(ReactivePulsarSenderFactory<?> reactivePulsarSenderFactory,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return new ReactivePulsarTemplate<>(reactivePulsarSenderFactory, schemaResolver, topicResolver);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableReactivePulsar
	@ConditionalOnMissingBean(
			name = PulsarAnnotationSupportBeanNames.REACTIVE_PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableReactivePulsarConfiguration {

	}

}
