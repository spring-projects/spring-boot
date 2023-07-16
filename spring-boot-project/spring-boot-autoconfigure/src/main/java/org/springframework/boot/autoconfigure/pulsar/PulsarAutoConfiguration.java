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

import java.util.Optional;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarClientFactory;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarReaderFactory;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunction;
import org.springframework.pulsar.function.PulsarFunctionAdministration;
import org.springframework.pulsar.function.PulsarSink;
import org.springframework.pulsar.function.PulsarSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Pulsar.
 *
 * @author Soby Chacko
 * @author Chris Bono
 * @author Alexander Preuß
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass(PulsarTemplate.class)
@EnableConfigurationProperties(PulsarProperties.class)
@Import({ PulsarAnnotationDrivenConfiguration.class })
public class PulsarAutoConfiguration {

	private final PulsarProperties properties;

	public PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarClientBuilderConfigurer pulsarClientBuilderConfigurer(PulsarProperties pulsarProperties,
			ObjectProvider<PulsarClientBuilderCustomizer> customizers) {
		return new PulsarClientBuilderConfigurer(pulsarProperties, customizers.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarClient pulsarClient(PulsarClientBuilderConfigurer configurer) {
		var clientFactory = new DefaultPulsarClientFactory(configurer::configure);
		try {
			return clientFactory.createClient();
		}
		catch (PulsarClientException ex) {
			throw new IllegalArgumentException("Failed to create client: " + ex.getMessage(), ex);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "false")
	public PulsarProducerFactory<?> pulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver) {
		return new DefaultPulsarProducerFactory<>(pulsarClient, this.properties.getProducer().getTopicName(),
				this.properties.getProducer().toProducerBuilderCustomizer(), topicResolver);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	public PulsarProducerFactory<?> cachingPulsarProducerFactory(PulsarClient pulsarClient,
			TopicResolver topicResolver) {
		return new CachingPulsarProducerFactory<>(pulsarClient, this.properties.getProducer().getTopicName(),
				this.properties.getProducer().toProducerBuilderCustomizer(), topicResolver,
				this.properties.getProducer().getCache().getExpireAfterAccess(),
				this.properties.getProducer().getCache().getMaximumSize(),
				this.properties.getProducer().getCache().getInitialCapacity());
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> interceptorsProvider, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		return new PulsarTemplate<>(pulsarProducerFactory, interceptorsProvider.orderedStream().toList(),
				schemaResolver, topicResolver, this.properties.getTemplate().isObservationsEnabled());
	}

	@Bean
	@ConditionalOnMissingBean(SchemaResolver.class)
	public DefaultSchemaResolver schemaResolver(PulsarProperties pulsarProperties,
			Optional<SchemaResolverCustomizer<DefaultSchemaResolver>> schemaResolverCustomizer) {
		var schemaResolver = new DefaultSchemaResolver();
		if (pulsarProperties.getDefaults().getTypeMappings() != null) {
			pulsarProperties.getDefaults()
				.getTypeMappings()
				.stream()
				.filter((tm) -> tm.schemaInfo() != null)
				.forEach((tm) -> {
					var schema = schemaResolver
						.resolveSchema(tm.schemaInfo().schemaType(), tm.messageType(), tm.schemaInfo().messageKeyType())
						.orElseThrow();
					schemaResolver.addCustomSchemaMapping(tm.messageType(), schema);
				});
		}
		schemaResolverCustomizer.ifPresent((customizer) -> customizer.customize(schemaResolver));
		return schemaResolver;
	}

	@Bean
	@ConditionalOnMissingBean(TopicResolver.class)
	public DefaultTopicResolver topicResolver(PulsarProperties pulsarProperties) {
		var topicResolver = new DefaultTopicResolver();
		if (pulsarProperties.getDefaults().getTypeMappings() != null) {
			pulsarProperties.getDefaults()
				.getTypeMappings()
				.stream()
				.filter((tm) -> tm.topicName() != null)
				.forEach((tm) -> topicResolver.addCustomTopicMapping(tm.messageType(), tm.topicName()));
		}
		return topicResolver;
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarConsumerFactory<>(pulsarClient,
				this.properties.getConsumer().toConsumerBuilderCustomizer());
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarAdministration pulsarAdministration() {
		return new PulsarAdministration(this.properties.getAdministration().toPulsarAdminBuilderCustomizer());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.function.enabled", havingValue = "true", matchIfMissing = true)
	public PulsarFunctionAdministration pulsarFunctionAdministration(PulsarAdministration pulsarAdministration,
			ObjectProvider<PulsarFunction> pulsarFunctions, ObjectProvider<PulsarSink> pulsarSinks,
			ObjectProvider<PulsarSource> pulsarSources) {
		return new PulsarFunctionAdministration(pulsarAdministration, pulsarFunctions, pulsarSinks, pulsarSources,
				this.properties.getFunction().getFailFast(), this.properties.getFunction().getPropagateFailures(),
				this.properties.getFunction().getPropagateStopFailures());
	}

	@Bean
	@ConditionalOnMissingBean
	public PulsarReaderFactory<?> pulsarReaderFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarReaderFactory<>(pulsarClient, this.properties.getReader().toReaderBuilderCustomizer());
	}

}
