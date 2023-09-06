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

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.SchemaInfo;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.TypeMapping;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.core.DefaultPulsarClientFactory;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.PulsarClientFactory;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunction;
import org.springframework.pulsar.function.PulsarFunctionAdministration;
import org.springframework.pulsar.function.PulsarSink;
import org.springframework.pulsar.function.PulsarSource;

/**
 * Common configuration used by both {@link PulsarAutoConfiguration} and
 * {@link PulsarReactiveAutoConfiguration}. A separate configuration class is used so that
 * {@link PulsarAutoConfiguration} can be excluded for reactive only application.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PulsarProperties.class)
class PulsarConfiguration {

	private final PulsarProperties properties;

	private final PulsarPropertiesMapper propertiesMapper;

	PulsarConfiguration(PulsarProperties properties) {
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
	PulsarClient pulsarClient(PulsarClientFactory clientFactory) throws PulsarClientException {
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

	private void addCustomSchemaMappings(DefaultSchemaResolver schemaResolver, List<TypeMapping> typeMappings) {
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
			Schema<?> schema = schemaResolver.resolveSchema(schemaType, messageType, messageKeyType).orElseThrow();
			schemaResolver.addCustomSchemaMapping(typeMapping.messageType(), schema);
		}
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
	@ConditionalOnProperty(name = "spring.pulsar.function.enabled", havingValue = "true", matchIfMissing = true)
	PulsarFunctionAdministration pulsarFunctionAdministration(PulsarAdministration pulsarAdministration,
			ObjectProvider<PulsarFunction> pulsarFunctions, ObjectProvider<PulsarSink> pulsarSinks,
			ObjectProvider<PulsarSource> pulsarSources) {
		PulsarProperties.Function properties = this.properties.getFunction();
		return new PulsarFunctionAdministration(pulsarAdministration, pulsarFunctions, pulsarSinks, pulsarSources,
				properties.isFailFast(), properties.isPropagateFailures(), properties.isPropagateStopFailures());
	}

}
