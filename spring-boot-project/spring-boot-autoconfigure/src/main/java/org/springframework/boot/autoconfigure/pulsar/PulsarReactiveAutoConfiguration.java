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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.annotation.EnableReactivePulsar;
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

	/**
	 * Constructs a new PulsarReactiveAutoConfiguration instance with the specified
	 * PulsarProperties.
	 * @param properties the PulsarProperties to be used for configuration
	 */
	PulsarReactiveAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarReactivePropertiesMapper(properties);
	}

	/**
	 * Creates a ReactivePulsarClient bean if no other bean of the same type is present.
	 * @param pulsarClient the PulsarClient bean to be adapted
	 * @return the created ReactivePulsarClient bean
	 */
	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarClient reactivePulsarClient(PulsarClient pulsarClient) {
		return AdaptedReactivePulsarClientFactory.create(pulsarClient);
	}

	/**
	 * Creates a new instance of {@link CaffeineShadedProducerCacheProvider} if no other
	 * bean of type {@link ProducerCacheProvider} is present. This bean is conditionally
	 * created based on the presence of the class
	 * {@link CaffeineShadedProducerCacheProvider} and the property
	 * "spring.pulsar.producer.cache.enabled" being set to "true" or not present. The
	 * properties for the cache provider are obtained from the
	 * {@link PulsarProperties.Producer.Cache} configuration.
	 * @return the created instance of {@link CaffeineShadedProducerCacheProvider}
	 */
	@Bean
	@ConditionalOnMissingBean(ProducerCacheProvider.class)
	@ConditionalOnClass(CaffeineShadedProducerCacheProvider.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	CaffeineShadedProducerCacheProvider reactivePulsarProducerCacheProvider() {
		PulsarProperties.Producer.Cache properties = this.properties.getProducer().getCache();
		return new CaffeineShadedProducerCacheProvider(properties.getExpireAfterAccess(), Duration.ofMinutes(10),
				properties.getMaximumSize(), properties.getInitialCapacity());
	}

	/**
	 * Creates a ReactiveMessageSenderCache bean if it is missing and the property
	 * "spring.pulsar.producer.cache.enabled" is set to true or is missing.
	 * @param producerCacheProvider the provider for the ProducerCacheProvider bean
	 * @return the ReactiveMessageSenderCache bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	ReactiveMessageSenderCache reactivePulsarMessageSenderCache(
			ObjectProvider<ProducerCacheProvider> producerCacheProvider) {
		return reactivePulsarMessageSenderCache(producerCacheProvider.getIfAvailable());
	}

	/**
	 * Creates a cache for the ReactiveMessageSender using the provided
	 * ProducerCacheProvider. If the producerCacheProvider is not null, it creates a cache
	 * using the provided provider. If the producerCacheProvider is null, it creates a
	 * cache using the default provider.
	 * @param producerCacheProvider the provider for the ProducerCache
	 * @return the ReactiveMessageSenderCache
	 */
	private ReactiveMessageSenderCache reactivePulsarMessageSenderCache(ProducerCacheProvider producerCacheProvider) {
		return (producerCacheProvider != null) ? AdaptedReactivePulsarClientFactory.createCache(producerCacheProvider)
				: AdaptedReactivePulsarClientFactory.createCache();
	}

	/**
	 * Creates a default {@link ReactivePulsarSenderFactory} bean if no other bean of the
	 * same type is present.
	 * @param reactivePulsarClient the {@link ReactivePulsarClient} bean
	 * @param reactiveMessageSenderCache the {@link ReactiveMessageSenderCache} bean
	 * provider
	 * @param topicResolver the {@link TopicResolver} bean
	 * @param customizersProvider the {@link ReactiveMessageSenderBuilderCustomizer} bean
	 * provider
	 * @return the default {@link ReactivePulsarSenderFactory} bean
	 */
	@Bean
	@ConditionalOnMissingBean(ReactivePulsarSenderFactory.class)
	DefaultReactivePulsarSenderFactory<?> reactivePulsarSenderFactory(ReactivePulsarClient reactivePulsarClient,
			ObjectProvider<ReactiveMessageSenderCache> reactiveMessageSenderCache, TopicResolver topicResolver,
			ObjectProvider<ReactiveMessageSenderBuilderCustomizer<?>> customizersProvider) {
		List<ReactiveMessageSenderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageSenderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageSenderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageSenderBuilderCustomizers(customizers, builder));
		return DefaultReactivePulsarSenderFactory.builderFor(reactivePulsarClient)
			.withDefaultConfigCustomizers(lambdaSafeCustomizers)
			.withMessageSenderCache(reactiveMessageSenderCache.getIfAvailable())
			.withTopicResolver(topicResolver)
			.build();
	}

	/**
	 * Applies the customizers to the given ReactiveMessageSenderBuilder.
	 * @param customizers the list of ReactiveMessageSenderBuilderCustomizer instances
	 * @param builder the ReactiveMessageSenderBuilder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyMessageSenderBuilderCustomizers(List<ReactiveMessageSenderBuilderCustomizer<?>> customizers,
			ReactiveMessageSenderBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageSenderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a default ReactivePulsarConsumerFactory bean if no other bean of type
	 * ReactivePulsarConsumerFactory is present.
	 * @param pulsarReactivePulsarClient the ReactivePulsarClient bean
	 * @param customizersProvider the ObjectProvider for
	 * ReactiveMessageConsumerBuilderCustomizer beans
	 * @return the created DefaultReactivePulsarConsumerFactory bean
	 */
	@Bean
	@ConditionalOnMissingBean(ReactivePulsarConsumerFactory.class)
	DefaultReactivePulsarConsumerFactory<?> reactivePulsarConsumerFactory(
			ReactivePulsarClient pulsarReactivePulsarClient,
			ObjectProvider<ReactiveMessageConsumerBuilderCustomizer<?>> customizersProvider) {
		List<ReactiveMessageConsumerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageConsumerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageConsumerBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageConsumerBuilderCustomizers(customizers, builder));
		return new DefaultReactivePulsarConsumerFactory<>(pulsarReactivePulsarClient, lambdaSafeCustomizers);
	}

	/**
	 * Applies the customizers to the given ReactiveMessageConsumerBuilder.
	 * @param customizers the list of ReactiveMessageConsumerBuilderCustomizer instances
	 * @param builder the ReactiveMessageConsumerBuilder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyMessageConsumerBuilderCustomizers(List<ReactiveMessageConsumerBuilderCustomizer<?>> customizers,
			ReactiveMessageConsumerBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageConsumerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a {@link DefaultReactivePulsarListenerContainerFactory} bean if there is no
	 * existing bean with the name "reactivePulsarListenerContainerFactory". This factory
	 * is responsible for creating reactive listener containers for consuming messages
	 * from Apache Pulsar.
	 * @param reactivePulsarConsumerFactory The {@link ReactivePulsarConsumerFactory} used
	 * for creating reactive Pulsar consumers.
	 * @param schemaResolver The {@link SchemaResolver} used for resolving the schema of
	 * the consumed messages.
	 * @param topicResolver The {@link TopicResolver} used for resolving the topic of the
	 * consumed messages.
	 * @return The created {@link DefaultReactivePulsarListenerContainerFactory} bean.
	 */
	@Bean
	@ConditionalOnMissingBean(name = "reactivePulsarListenerContainerFactory")
	DefaultReactivePulsarListenerContainerFactory<?> reactivePulsarListenerContainerFactory(
			ReactivePulsarConsumerFactory<Object> reactivePulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		this.propertiesMapper.customizeContainerProperties(containerProperties);
		return new DefaultReactivePulsarListenerContainerFactory<>(reactivePulsarConsumerFactory, containerProperties);
	}

	/**
	 * Creates a default {@link ReactivePulsarReaderFactory} bean if no other bean of the
	 * same type is present.
	 * @param reactivePulsarClient the {@link ReactivePulsarClient} bean
	 * @param customizersProvider the {@link ObjectProvider} of
	 * {@link ReactiveMessageReaderBuilderCustomizer} beans
	 * @return the created {@link DefaultReactivePulsarReaderFactory} bean
	 */
	@Bean
	@ConditionalOnMissingBean(ReactivePulsarReaderFactory.class)
	DefaultReactivePulsarReaderFactory<?> reactivePulsarReaderFactory(ReactivePulsarClient reactivePulsarClient,
			ObjectProvider<ReactiveMessageReaderBuilderCustomizer<?>> customizersProvider) {
		List<ReactiveMessageReaderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeMessageReaderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReactiveMessageReaderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyMessageReaderBuilderCustomizers(customizers, builder));
		return new DefaultReactivePulsarReaderFactory<>(reactivePulsarClient, lambdaSafeCustomizers);
	}

	/**
	 * Applies the customizers to the given ReactiveMessageReaderBuilder.
	 * @param customizers the list of ReactiveMessageReaderBuilderCustomizer instances
	 * @param builder the ReactiveMessageReaderBuilder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyMessageReaderBuilderCustomizers(List<ReactiveMessageReaderBuilderCustomizer<?>> customizers,
			ReactiveMessageReaderBuilder<?> builder) {
		LambdaSafe.callbacks(ReactiveMessageReaderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a new instance of ReactivePulsarTemplate if no other bean of the same type
	 * is present.
	 * @param reactivePulsarSenderFactory the ReactivePulsarSenderFactory used to create
	 * ReactivePulsarSender instances
	 * @param schemaResolver the SchemaResolver used to resolve schemas for messages
	 * @param topicResolver the TopicResolver used to resolve topics for messages
	 * @return a new instance of ReactivePulsarTemplate
	 */
	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarTemplate<?> pulsarReactiveTemplate(ReactivePulsarSenderFactory<?> reactivePulsarSenderFactory,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return new ReactivePulsarTemplate<>(reactivePulsarSenderFactory, schemaResolver, topicResolver);
	}

	/**
	 * EnableReactivePulsarConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableReactivePulsar
	@ConditionalOnMissingBean(
			name = PulsarAnnotationSupportBeanNames.REACTIVE_PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableReactivePulsarConfiguration {

	}

}
