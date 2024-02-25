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

import java.util.ArrayList;
import java.util.List;

import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.ReaderBuilder;
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarReaderFactory;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Pulsar.
 *
 * @author Chris Bono
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Phillip Webb
 * @author Jonas Geiregat
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass({ PulsarClient.class, PulsarTemplate.class })
@Import(PulsarConfiguration.class)
public class PulsarAutoConfiguration {

	private PulsarProperties properties;

	private PulsarPropertiesMapper propertiesMapper;

	/**
	 * Constructs a new PulsarAutoConfiguration object with the specified
	 * PulsarProperties.
	 * @param properties the PulsarProperties object containing the configuration
	 * properties for Pulsar
	 */
	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarPropertiesMapper(properties);
	}

	/**
	 * Creates a default PulsarProducerFactory bean if no other bean of type
	 * PulsarProducerFactory is present and the property
	 * spring.pulsar.producer.cache.enabled is set to false.
	 * @param pulsarClient the PulsarClient bean
	 * @param topicResolver the TopicResolver bean
	 * @param customizersProvider the ObjectProvider for ProducerBuilderCustomizer beans
	 * @return the DefaultPulsarProducerFactory bean
	 */
	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "false")
	DefaultPulsarProducerFactory<?> pulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver,
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider) {
		List<ProducerBuilderCustomizer<Object>> lambdaSafeCustomizers = lambdaSafeProducerBuilderCustomizers(
				customizersProvider);
		return new DefaultPulsarProducerFactory<>(pulsarClient, this.properties.getProducer().getTopicName(),
				lambdaSafeCustomizers, topicResolver);
	}

	/**
	 * Creates a caching Pulsar producer factory if no other bean of type
	 * {@link PulsarProducerFactory} is present and the property
	 * {@code spring.pulsar.producer.cache.enabled} is set to {@code true} (or not set at
	 * all).
	 *
	 * The caching Pulsar producer factory is responsible for creating and managing Pulsar
	 * producers. It uses a cache to store and reuse producer instances, improving
	 * performance by avoiding the overhead of creating a new producer for each message.
	 *
	 * The cache properties, such as expiration time, maximum size, and initial capacity,
	 * are obtained from the application's configuration file.
	 * @param pulsarClient the Pulsar client used to create producers
	 * @param topicResolver the topic resolver used to resolve topic names
	 * @param customizersProvider a provider for customizers that can customize the
	 * producer builder
	 * @return a caching Pulsar producer factory
	 */
	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	CachingPulsarProducerFactory<?> cachingPulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver,
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider) {
		PulsarProperties.Producer.Cache cacheProperties = this.properties.getProducer().getCache();
		List<ProducerBuilderCustomizer<Object>> lambdaSafeCustomizers = lambdaSafeProducerBuilderCustomizers(
				customizersProvider);
		return new CachingPulsarProducerFactory<>(pulsarClient, this.properties.getProducer().getTopicName(),
				lambdaSafeCustomizers, topicResolver, cacheProperties.getExpireAfterAccess(),
				cacheProperties.getMaximumSize(), cacheProperties.getInitialCapacity());
	}

	/**
	 * Returns a list of lambda-safe producer builder customizers.
	 * @param customizersProvider the provider for producer builder customizers
	 * @return the list of lambda-safe producer builder customizers
	 */
	private List<ProducerBuilderCustomizer<Object>> lambdaSafeProducerBuilderCustomizers(
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider) {
		List<ProducerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeProducerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		return List.of((builder) -> applyProducerBuilderCustomizers(customizers, builder));
	}

	/**
	 * Applies the customizers to the given producer builder.
	 * @param customizers the list of producer builder customizers
	 * @param builder the producer builder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyProducerBuilderCustomizers(List<ProducerBuilderCustomizer<?>> customizers,
			ProducerBuilder<?> builder) {
		LambdaSafe.callbacks(ProducerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a PulsarTemplate bean if there is no existing bean of the same type.
	 * @param pulsarProducerFactory the PulsarProducerFactory used to create the
	 * PulsarTemplate
	 * @param producerInterceptors the list of ProducerInterceptors to be applied to the
	 * PulsarTemplate
	 * @param schemaResolver the SchemaResolver used to resolve schemas for the
	 * PulsarTemplate
	 * @param topicResolver the TopicResolver used to resolve topics for the
	 * PulsarTemplate
	 * @return the created PulsarTemplate bean
	 */
	@Bean
	@ConditionalOnMissingBean
	PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> producerInterceptors, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		return new PulsarTemplate<>(pulsarProducerFactory, producerInterceptors.orderedStream().toList(),
				schemaResolver, topicResolver, this.properties.getTemplate().isObservationsEnabled());
	}

	/**
	 * Creates a default {@link PulsarConsumerFactory} bean if no other bean of the same
	 * type is present.
	 * @param pulsarClient The {@link PulsarClient} instance to be used by the consumer
	 * factory.
	 * @param customizersProvider The provider for {@link ConsumerBuilderCustomizer}
	 * instances.
	 * @return The created {@link DefaultPulsarConsumerFactory} bean.
	 */
	@Bean
	@ConditionalOnMissingBean(PulsarConsumerFactory.class)
	DefaultPulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient,
			ObjectProvider<ConsumerBuilderCustomizer<?>> customizersProvider) {
		List<ConsumerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeConsumerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ConsumerBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyConsumerBuilderCustomizers(customizers, builder));
		return new DefaultPulsarConsumerFactory<>(pulsarClient, lambdaSafeCustomizers);
	}

	/**
	 * Applies the customizers to the given consumer builder.
	 * @param customizers the list of consumer builder customizers
	 * @param builder the consumer builder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyConsumerBuilderCustomizers(List<ConsumerBuilderCustomizer<?>> customizers,
			ConsumerBuilder<?> builder) {
		LambdaSafe.callbacks(ConsumerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a {@link ConcurrentPulsarListenerContainerFactory} bean if there is no
	 * existing bean with the name "pulsarListenerContainerFactory". This factory is used
	 * to create concurrent Pulsar listener containers.
	 *
	 * The factory is conditionally created using the {@link ConditionalOnMissingBean}
	 * annotation, which ensures that it is only created if there is no existing bean with
	 * the specified name.
	 *
	 * The factory requires a {@link PulsarConsumerFactory} bean, a {@link SchemaResolver}
	 * bean, a {@link TopicResolver} bean, and an {@link Environment} bean as
	 * dependencies.
	 *
	 * The factory also creates a {@link PulsarContainerProperties} object and sets the
	 * provided {@link SchemaResolver} and {@link TopicResolver} beans as properties.
	 *
	 * If the environment is configured to use virtual threads, the factory sets a
	 * {@link VirtualThreadTaskExecutor} as the consumer task executor in the container
	 * properties.
	 *
	 * The factory also applies any customizations to the container properties using the
	 * {@link PulsarPropertiesMapper} bean.
	 *
	 * Finally, the factory returns a new instance of
	 * {@link ConcurrentPulsarListenerContainerFactory} with the provided dependencies and
	 * container properties.
	 * @param pulsarConsumerFactory the {@link PulsarConsumerFactory} bean to use for
	 * creating Pulsar consumers
	 * @param schemaResolver the {@link SchemaResolver} bean to use for resolving Pulsar
	 * schemas
	 * @param topicResolver the {@link TopicResolver} bean to use for resolving Pulsar
	 * topics
	 * @param environment the {@link Environment} bean to use for accessing the
	 * application's environment
	 * @return a new instance of {@link ConcurrentPulsarListenerContainerFactory}
	 */
	@Bean
	@ConditionalOnMissingBean(name = "pulsarListenerContainerFactory")
	ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
			PulsarConsumerFactory<Object> pulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver, Environment environment) {
		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		if (Threading.VIRTUAL.isActive(environment)) {
			containerProperties.setConsumerTaskExecutor(new VirtualThreadTaskExecutor());
		}
		this.propertiesMapper.customizeContainerProperties(containerProperties);
		return new ConcurrentPulsarListenerContainerFactory<>(pulsarConsumerFactory, containerProperties);
	}

	/**
	 * Creates a default {@link PulsarReaderFactory} bean if no other bean of type
	 * {@link PulsarReaderFactory} is present.
	 * @param pulsarClient The {@link PulsarClient} instance to be used by the reader
	 * factory.
	 * @param customizersProvider The {@link ObjectProvider} of
	 * {@link ReaderBuilderCustomizer} instances.
	 * @return The created {@link DefaultPulsarReaderFactory} bean.
	 */
	@Bean
	@ConditionalOnMissingBean(PulsarReaderFactory.class)
	DefaultPulsarReaderFactory<?> pulsarReaderFactory(PulsarClient pulsarClient,
			ObjectProvider<ReaderBuilderCustomizer<?>> customizersProvider) {
		List<ReaderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeReaderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReaderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyReaderBuilderCustomizers(customizers, builder));
		return new DefaultPulsarReaderFactory<>(pulsarClient, lambdaSafeCustomizers);
	}

	/**
	 * Applies the customizers to the reader builder.
	 * @param customizers the list of reader builder customizers
	 * @param builder the reader builder to customize
	 */
	@SuppressWarnings("unchecked")
	private void applyReaderBuilderCustomizers(List<ReaderBuilderCustomizer<?>> customizers, ReaderBuilder<?> builder) {
		LambdaSafe.callbacks(ReaderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	/**
	 * Creates a default PulsarReaderContainerFactory bean if no bean with the name
	 * "pulsarReaderContainerFactory" is present.
	 * @param pulsarReaderFactory The PulsarReaderFactory bean.
	 * @param schemaResolver The SchemaResolver bean.
	 * @param environment The Environment bean.
	 * @return The DefaultPulsarReaderContainerFactory bean.
	 */
	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(PulsarReaderFactory<?> pulsarReaderFactory,
			SchemaResolver schemaResolver, Environment environment) {
		PulsarReaderContainerProperties readerContainerProperties = new PulsarReaderContainerProperties();
		readerContainerProperties.setSchemaResolver(schemaResolver);
		if (Threading.VIRTUAL.isActive(environment)) {
			readerContainerProperties.setReaderTaskExecutor(new VirtualThreadTaskExecutor());
		}
		this.propertiesMapper.customizeReaderContainerProperties(readerContainerProperties);
		return new DefaultPulsarReaderContainerFactory<>(pulsarReaderFactory, readerContainerProperties);
	}

	/**
	 * EnablePulsarConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	@ConditionalOnMissingBean(name = { PulsarAnnotationSupportBeanNames.PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
			PulsarAnnotationSupportBeanNames.PULSAR_READER_ANNOTATION_PROCESSOR_BEAN_NAME })
	static class EnablePulsarConfiguration {

	}

}
