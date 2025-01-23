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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.pulsar.core.PulsarTopicBuilder;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.pulsar.transaction.PulsarAwareTransactionManager;
import org.springframework.pulsar.transaction.PulsarTransactionManager;

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

	private final PulsarProperties properties;

	private final PulsarPropertiesMapper propertiesMapper;

	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarPropertiesMapper(properties);
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnBooleanProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = false)
	DefaultPulsarProducerFactory<?> pulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver,
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ProducerBuilderCustomizer<Object>> lambdaSafeCustomizers = lambdaSafeProducerBuilderCustomizers(
				customizersProvider);
		DefaultPulsarProducerFactory<?> producerFactory = new DefaultPulsarProducerFactory<>(pulsarClient,
				this.properties.getProducer().getTopicName(), lambdaSafeCustomizers, topicResolver);
		topicBuilderProvider.ifAvailable(producerFactory::setTopicBuilder);
		return producerFactory;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnBooleanProperty(name = "spring.pulsar.producer.cache.enabled", matchIfMissing = true)
	CachingPulsarProducerFactory<?> cachingPulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver,
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		PulsarProperties.Producer.Cache cacheProperties = this.properties.getProducer().getCache();
		List<ProducerBuilderCustomizer<Object>> lambdaSafeCustomizers = lambdaSafeProducerBuilderCustomizers(
				customizersProvider);
		CachingPulsarProducerFactory<?> producerFactory = new CachingPulsarProducerFactory<>(pulsarClient,
				this.properties.getProducer().getTopicName(), lambdaSafeCustomizers, topicResolver,
				cacheProperties.getExpireAfterAccess(), cacheProperties.getMaximumSize(),
				cacheProperties.getInitialCapacity());
		topicBuilderProvider.ifAvailable(producerFactory::setTopicBuilder);
		return producerFactory;
	}

	private List<ProducerBuilderCustomizer<Object>> lambdaSafeProducerBuilderCustomizers(
			ObjectProvider<ProducerBuilderCustomizer<?>> customizersProvider) {
		List<ProducerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeProducerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		return List.of((builder) -> applyProducerBuilderCustomizers(customizers, builder));
	}

	@SuppressWarnings("unchecked")
	private void applyProducerBuilderCustomizers(List<ProducerBuilderCustomizer<?>> customizers,
			ProducerBuilder<?> builder) {
		LambdaSafe.callbacks(ProducerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> producerInterceptors, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		PulsarTemplate<?> template = new PulsarTemplate<>(pulsarProducerFactory,
				producerInterceptors.orderedStream().toList(), schemaResolver, topicResolver,
				this.properties.getTemplate().isObservationsEnabled());
		this.propertiesMapper.customizeTemplate(template);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarConsumerFactory.class)
	DefaultPulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient,
			ObjectProvider<ConsumerBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ConsumerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeConsumerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ConsumerBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyConsumerBuilderCustomizers(customizers, builder));
		DefaultPulsarConsumerFactory<?> consumerFactory = new DefaultPulsarConsumerFactory<>(pulsarClient,
				lambdaSafeCustomizers);
		topicBuilderProvider.ifAvailable(consumerFactory::setTopicBuilder);
		return consumerFactory;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarAwareTransactionManager.class)
	@ConditionalOnBooleanProperty("spring.pulsar.transaction.enabled")
	public PulsarTransactionManager pulsarTransactionManager(PulsarClient pulsarClient) {
		return new PulsarTransactionManager(pulsarClient);
	}

	@SuppressWarnings("unchecked")
	private void applyConsumerBuilderCustomizers(List<ConsumerBuilderCustomizer<?>> customizers,
			ConsumerBuilder<?> builder) {
		LambdaSafe.callbacks(ConsumerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarListenerContainerFactory")
	ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
			PulsarConsumerFactory<Object> pulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver, ObjectProvider<PulsarAwareTransactionManager> pulsarTransactionManager,
			Environment environment, PulsarContainerFactoryCustomizers containerFactoryCustomizers) {
		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		if (Threading.VIRTUAL.isActive(environment)) {
			containerProperties.setConsumerTaskExecutor(new VirtualThreadTaskExecutor("pulsar-consumer-"));
		}
		pulsarTransactionManager.ifUnique(containerProperties.transactions()::setTransactionManager);
		this.propertiesMapper.customizeContainerProperties(containerProperties);
		ConcurrentPulsarListenerContainerFactory<?> containerFactory = new ConcurrentPulsarListenerContainerFactory<>(
				pulsarConsumerFactory, containerProperties);
		containerFactoryCustomizers.customize(containerFactory);
		return containerFactory;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarReaderFactory.class)
	DefaultPulsarReaderFactory<?> pulsarReaderFactory(PulsarClient pulsarClient,
			ObjectProvider<ReaderBuilderCustomizer<?>> customizersProvider,
			ObjectProvider<PulsarTopicBuilder> topicBuilderProvider) {
		List<ReaderBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeReaderBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ReaderBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyReaderBuilderCustomizers(customizers, builder));
		DefaultPulsarReaderFactory<?> readerFactory = new DefaultPulsarReaderFactory<>(pulsarClient,
				lambdaSafeCustomizers);
		topicBuilderProvider.ifAvailable(readerFactory::setTopicBuilder);
		return readerFactory;
	}

	@SuppressWarnings("unchecked")
	private void applyReaderBuilderCustomizers(List<ReaderBuilderCustomizer<?>> customizers, ReaderBuilder<?> builder) {
		LambdaSafe.callbacks(ReaderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(PulsarReaderFactory<?> pulsarReaderFactory,
			SchemaResolver schemaResolver, Environment environment,
			PulsarContainerFactoryCustomizers containerFactoryCustomizers) {
		PulsarReaderContainerProperties readerContainerProperties = new PulsarReaderContainerProperties();
		readerContainerProperties.setSchemaResolver(schemaResolver);
		if (Threading.VIRTUAL.isActive(environment)) {
			readerContainerProperties.setReaderTaskExecutor(new VirtualThreadTaskExecutor("pulsar-reader-"));
		}
		this.propertiesMapper.customizeReaderContainerProperties(readerContainerProperties);
		DefaultPulsarReaderContainerFactory<?> containerFactory = new DefaultPulsarReaderContainerFactory<>(
				pulsarReaderFactory, readerContainerProperties);
		containerFactoryCustomizers.customize(containerFactory);
		return containerFactory;
	}

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	@ConditionalOnMissingBean(name = { PulsarAnnotationSupportBeanNames.PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
			PulsarAnnotationSupportBeanNames.PULSAR_READER_ANNOTATION_PROCESSOR_BEAN_NAME })
	static class EnablePulsarConfiguration {

	}

}
