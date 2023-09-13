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
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass({ PulsarClient.class, PulsarTemplate.class })
@Import(PulsarConfiguration.class)
public class PulsarAutoConfiguration {

	private PulsarProperties properties;

	private PulsarPropertiesMapper propertiesMapper;

	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarPropertiesMapper(properties);
	}

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
		return new PulsarTemplate<>(pulsarProducerFactory, producerInterceptors.orderedStream().toList(),
				schemaResolver, topicResolver, this.properties.getTemplate().isObservationsEnabled());
	}

	@Bean
	@ConditionalOnMissingBean(PulsarConsumerFactory.class)
	DefaultPulsarConsumerFactory<Object> pulsarConsumerFactory(PulsarClient pulsarClient,
			ObjectProvider<ConsumerBuilderCustomizer<?>> customizersProvider) {
		List<ConsumerBuilderCustomizer<?>> customizers = new ArrayList<>();
		customizers.add(this.propertiesMapper::customizeConsumerBuilder);
		customizers.addAll(customizersProvider.orderedStream().toList());
		List<ConsumerBuilderCustomizer<Object>> lambdaSafeCustomizers = List
			.of((builder) -> applyConsumerBuilderCustomizers(customizers, builder));
		return new DefaultPulsarConsumerFactory<>(pulsarClient, lambdaSafeCustomizers);
	}

	@SuppressWarnings("unchecked")
	private void applyConsumerBuilderCustomizers(List<ConsumerBuilderCustomizer<?>> customizers,
			ConsumerBuilder<?> builder) {
		LambdaSafe.callbacks(ConsumerBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarListenerContainerFactory")
	ConcurrentPulsarListenerContainerFactory<Object> pulsarListenerContainerFactory(
			PulsarConsumerFactory<Object> pulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		this.propertiesMapper.customizeContainerProperties(containerProperties);
		return new ConcurrentPulsarListenerContainerFactory<>(pulsarConsumerFactory, containerProperties);
	}

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

	@SuppressWarnings("unchecked")
	private void applyReaderBuilderCustomizers(List<ReaderBuilderCustomizer<?>> customizers, ReaderBuilder<?> builder) {
		LambdaSafe.callbacks(ReaderBuilderCustomizer.class, customizers, builder)
			.invoke((customizer) -> customizer.customize(builder));
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(PulsarReaderFactory<?> pulsarReaderFactory,
			SchemaResolver schemaResolver) {
		PulsarReaderContainerProperties readerContainerProperties = new PulsarReaderContainerProperties();
		readerContainerProperties.setSchemaResolver(schemaResolver);
		this.propertiesMapper.customizeReaderContainerProperties(readerContainerProperties);
		return new DefaultPulsarReaderContainerFactory<>(pulsarReaderFactory, readerContainerProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	@ConditionalOnMissingBean(name = { PulsarAnnotationSupportBeanNames.PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
			PulsarAnnotationSupportBeanNames.PULSAR_READER_ANNOTATION_PROCESSOR_BEAN_NAME })
	static class EnablePulsarConfiguration {

	}

}
