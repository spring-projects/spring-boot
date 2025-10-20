/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.pulsar.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.ReaderBuilder;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;
import org.apache.pulsar.common.naming.TopicDomain;
import org.apache.pulsar.common.schema.SchemaType;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.pulsar.autoconfigure.PulsarProperties.Defaults.SchemaInfo;
import org.springframework.boot.pulsar.autoconfigure.PulsarProperties.Defaults.TypeMapping;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.DefaultPulsarClientFactory;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarReaderFactory;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.PulsarClientFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTopicBuilder;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunction;
import org.springframework.pulsar.function.PulsarFunctionAdministration;
import org.springframework.pulsar.function.PulsarSink;
import org.springframework.pulsar.function.PulsarSource;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.pulsar.transaction.PulsarAwareTransactionManager;
import org.springframework.pulsar.transaction.PulsarTransactionManager;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Pulsar.
 *
 * @author Chris Bono
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Phillip Webb
 * @author Jonas Geiregat
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ PulsarClient.class, PulsarTemplate.class })
@EnableConfigurationProperties(PulsarProperties.class)
public final class PulsarAutoConfiguration {

	private final PulsarProperties properties;

	private final PulsarPropertiesMapper propertiesMapper;

	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarPropertiesMapper(properties);
	}

	@Bean
	@ConditionalOnMissingBean(PulsarConnectionDetails.class)
	PropertiesPulsarConnectionDetails pulsarConnectionDetails() {
		return new PropertiesPulsarConnectionDetails(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean(PulsarClientFactory.class)
	DefaultPulsarClientFactory pulsarClientFactory(PulsarConnectionDetails connectionDetails,
			ObjectProvider<PulsarClientBuilderCustomizer> customizersProvider) {
		List<PulsarClientBuilderCustomizer> allCustomizers = new ArrayList<>();
		allCustomizers.add((builder) -> this.propertiesMapper.customizeClientBuilder(builder, connectionDetails));
		allCustomizers.addAll(customizersProvider.orderedStream().toList());
		DefaultPulsarClientFactory clientFactory = new DefaultPulsarClientFactory(
				(clientBuilder) -> applyClientBuilderCustomizers(allCustomizers, clientBuilder));
		return clientFactory;
	}

	private void applyClientBuilderCustomizers(List<PulsarClientBuilderCustomizer> customizers,
			ClientBuilder clientBuilder) {
		customizers.forEach((customizer) -> customizer.customize(clientBuilder));
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarClient pulsarClient(PulsarClientFactory clientFactory) {
		return clientFactory.createClient();
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarAdministration pulsarAdministration(PulsarConnectionDetails connectionDetails,
			ObjectProvider<PulsarAdminBuilderCustomizer> pulsarAdminBuilderCustomizers) {
		List<PulsarAdminBuilderCustomizer> allCustomizers = new ArrayList<>();
		allCustomizers.add((builder) -> this.propertiesMapper.customizeAdminBuilder(builder, connectionDetails));
		allCustomizers.addAll(pulsarAdminBuilderCustomizers.orderedStream().toList());
		return new PulsarAdministration((adminBuilder) -> applyAdminBuilderCustomizers(allCustomizers, adminBuilder));
	}

	private void applyAdminBuilderCustomizers(List<PulsarAdminBuilderCustomizer> customizers,
			PulsarAdminBuilder adminBuilder) {
		customizers.forEach((customizer) -> customizer.customize(adminBuilder));
	}

	@Bean
	@ConditionalOnMissingBean(SchemaResolver.class)
	DefaultSchemaResolver pulsarSchemaResolver(ObjectProvider<SchemaResolverCustomizer<?>> schemaResolverCustomizers) {
		DefaultSchemaResolver schemaResolver = new DefaultSchemaResolver();
		addCustomSchemaMappings(schemaResolver, this.properties.getDefaults().getTypeMappings());
		applySchemaResolverCustomizers(schemaResolverCustomizers.orderedStream().toList(), schemaResolver);
		return schemaResolver;
	}

	private void addCustomSchemaMappings(DefaultSchemaResolver schemaResolver,
			@Nullable List<TypeMapping> typeMappings) {
		if (typeMappings != null) {
			typeMappings.forEach((typeMapping) -> addCustomSchemaMapping(schemaResolver, typeMapping));
		}
	}

	private void addCustomSchemaMapping(DefaultSchemaResolver schemaResolver, TypeMapping typeMapping) {
		SchemaInfo schemaInfo = typeMapping.schemaInfo();
		if (schemaInfo != null) {
			Class<?> messageType = typeMapping.messageType();
			SchemaType schemaType = schemaInfo.schemaType();
			Class<?> messageKeyType = schemaInfo.messageKeyType();
			Schema<?> schema = getSchema(schemaResolver, schemaType, messageType, messageKeyType);
			schemaResolver.addCustomSchemaMapping(typeMapping.messageType(), schema);
		}
	}

	private Schema<Object> getSchema(DefaultSchemaResolver schemaResolver, SchemaType schemaType, Class<?> messageType,
			@Nullable Class<?> messageKeyType) {
		Schema<Object> schema = schemaResolver.resolveSchema(schemaType, messageType, messageKeyType).orElseThrow();
		Assert.state(schema != null, "'schema' must not be null");
		return schema;
	}

	@SuppressWarnings("unchecked")
	private void applySchemaResolverCustomizers(List<SchemaResolverCustomizer<?>> customizers,
			DefaultSchemaResolver schemaResolver) {
		LambdaSafe.callbacks(SchemaResolverCustomizer.class, customizers, schemaResolver)
			.invoke((customizer) -> customizer.customize(schemaResolver));
	}

	@Bean
	@ConditionalOnMissingBean(TopicResolver.class)
	DefaultTopicResolver pulsarTopicResolver() {
		DefaultTopicResolver topicResolver = new DefaultTopicResolver();
		List<TypeMapping> typeMappings = this.properties.getDefaults().getTypeMappings();
		if (typeMappings != null) {
			typeMappings.forEach((typeMapping) -> addCustomTopicMapping(topicResolver, typeMapping));
		}
		return topicResolver;
	}

	private void addCustomTopicMapping(DefaultTopicResolver topicResolver, TypeMapping typeMapping) {
		String topicName = typeMapping.topicName();
		if (topicName != null) {
			topicResolver.addCustomTopicMapping(typeMapping.messageType(), topicName);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.pulsar.function.enabled", matchIfMissing = true)
	PulsarFunctionAdministration pulsarFunctionAdministration(PulsarAdministration pulsarAdministration,
			ObjectProvider<PulsarFunction> pulsarFunctions, ObjectProvider<PulsarSink> pulsarSinks,
			ObjectProvider<PulsarSource> pulsarSources) {
		PulsarProperties.Function properties = this.properties.getFunction();
		return new PulsarFunctionAdministration(pulsarAdministration, pulsarFunctions, pulsarSinks, pulsarSources,
				properties.isFailFast(), properties.isPropagateFailures(), properties.isPropagateStopFailures());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.pulsar.defaults.topic.enabled", matchIfMissing = true)
	PulsarTopicBuilder pulsarTopicBuilder() {
		return new PulsarTopicBuilder(TopicDomain.persistent, this.properties.getDefaults().getTopic().getTenant(),
				this.properties.getDefaults().getTopic().getNamespace());
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarContainerFactoryCustomizers pulsarContainerFactoryCustomizers(
			ObjectProvider<PulsarContainerFactoryCustomizer<?>> customizers) {
		return new PulsarContainerFactoryCustomizers(customizers.orderedStream().toList());
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
	PulsarTransactionManager pulsarTransactionManager(PulsarClient pulsarClient) {
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
