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

	/**
     * Constructs a new PulsarConfiguration object with the provided PulsarProperties.
     * 
     * @param properties the PulsarProperties object containing the configuration properties for Pulsar
     */
    PulsarConfiguration(PulsarProperties properties) {
		this.properties = properties;
		this.propertiesMapper = new PulsarPropertiesMapper(properties);
	}

	/**
     * Creates a new instance of {@link PropertiesPulsarConnectionDetails} if no bean of type {@link PulsarConnectionDetails} is present.
     * 
     * @return the {@link PropertiesPulsarConnectionDetails} instance
     */
    @Bean
	@ConditionalOnMissingBean(PulsarConnectionDetails.class)
	PropertiesPulsarConnectionDetails pulsarConnectionDetails() {
		return new PropertiesPulsarConnectionDetails(this.properties);
	}

	/**
     * Creates a DefaultPulsarClientFactory bean if no other bean of type PulsarClientFactory is present.
     * 
     * @param connectionDetails the PulsarConnectionDetails object containing the connection details for the Pulsar client
     * @param customizersProvider the ObjectProvider for PulsarClientBuilderCustomizer objects
     * @return the DefaultPulsarClientFactory bean
     */
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

	/**
     * Applies the provided list of {@link PulsarClientBuilderCustomizer} to customize the {@link ClientBuilder}.
     * 
     * @param customizers the list of {@link PulsarClientBuilderCustomizer} to apply
     * @param clientBuilder the {@link ClientBuilder} to be customized
     */
    private void applyClientBuilderCustomizers(List<PulsarClientBuilderCustomizer> customizers,
			ClientBuilder clientBuilder) {
		customizers.forEach((customizer) -> customizer.customize(clientBuilder));
	}

	/**
     * Creates a Pulsar client using the provided client factory.
     * 
     * @param clientFactory the Pulsar client factory used to create the client
     * @return the Pulsar client
     * @throws PulsarClientException if an error occurs while creating the client
     */
    @Bean
	@ConditionalOnMissingBean
	PulsarClient pulsarClient(PulsarClientFactory clientFactory) throws PulsarClientException {
		return clientFactory.createClient();
	}

	/**
     * Creates a PulsarAdministration bean if no other bean of the same type is present.
     * 
     * @param connectionDetails the PulsarConnectionDetails object containing the connection details for Pulsar
     * @param pulsarAdminBuilderCustomizers the ObjectProvider for PulsarAdminBuilderCustomizer objects
     * @return a PulsarAdministration object
     */
    @Bean
	@ConditionalOnMissingBean
	PulsarAdministration pulsarAdministration(PulsarConnectionDetails connectionDetails,
			ObjectProvider<PulsarAdminBuilderCustomizer> pulsarAdminBuilderCustomizers) {
		List<PulsarAdminBuilderCustomizer> allCustomizers = new ArrayList<>();
		allCustomizers.add((builder) -> this.propertiesMapper.customizeAdminBuilder(builder, connectionDetails));
		allCustomizers.addAll(pulsarAdminBuilderCustomizers.orderedStream().toList());
		return new PulsarAdministration((adminBuilder) -> applyAdminBuilderCustomizers(allCustomizers, adminBuilder));
	}

	/**
     * Applies the provided list of {@link PulsarAdminBuilderCustomizer} to customize the {@link PulsarAdminBuilder}.
     * 
     * @param customizers the list of {@link PulsarAdminBuilderCustomizer} to apply
     * @param adminBuilder the {@link PulsarAdminBuilder} to customize
     */
    private void applyAdminBuilderCustomizers(List<PulsarAdminBuilderCustomizer> customizers,
			PulsarAdminBuilder adminBuilder) {
		customizers.forEach((customizer) -> customizer.customize(adminBuilder));
	}

	/**
     * Creates a DefaultSchemaResolver bean if no other bean of type SchemaResolver is present.
     * 
     * @param schemaResolverCustomizers ObjectProvider of SchemaResolverCustomizer instances
     * @return DefaultSchemaResolver bean
     */
    @Bean
	@ConditionalOnMissingBean(SchemaResolver.class)
	DefaultSchemaResolver pulsarSchemaResolver(ObjectProvider<SchemaResolverCustomizer<?>> schemaResolverCustomizers) {
		DefaultSchemaResolver schemaResolver = new DefaultSchemaResolver();
		addCustomSchemaMappings(schemaResolver, this.properties.getDefaults().getTypeMappings());
		applySchemaResolverCustomizers(schemaResolverCustomizers.orderedStream().toList(), schemaResolver);
		return schemaResolver;
	}

	/**
     * Adds custom schema mappings to the given schema resolver.
     * 
     * @param schemaResolver The schema resolver to add the custom schema mappings to.
     * @param typeMappings The list of type mappings to add.
     */
    private void addCustomSchemaMappings(DefaultSchemaResolver schemaResolver, List<TypeMapping> typeMappings) {
		if (typeMappings != null) {
			typeMappings.forEach((typeMapping) -> addCustomSchemaMapping(schemaResolver, typeMapping));
		}
	}

	/**
     * Adds a custom schema mapping to the given schema resolver using the provided type mapping.
     * 
     * @param schemaResolver The schema resolver to add the custom schema mapping to.
     * @param typeMapping The type mapping containing the schema information.
     * @throws IllegalArgumentException if the schema cannot be resolved.
     */
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

	/**
     * Applies the customizers to the schema resolver.
     * 
     * @param customizers the list of schema resolver customizers
     * @param schemaResolver the default schema resolver
     */
    @SuppressWarnings("unchecked")
	private void applySchemaResolverCustomizers(List<SchemaResolverCustomizer<?>> customizers,
			DefaultSchemaResolver schemaResolver) {
		LambdaSafe.callbacks(SchemaResolverCustomizer.class, customizers, schemaResolver)
			.invoke((customizer) -> customizer.customize(schemaResolver));
	}

	/**
     * Creates a default topic resolver bean if no other bean of type TopicResolver is present.
     * 
     * @return the default topic resolver bean
     */
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

	/**
     * Adds a custom topic mapping to the given topic resolver.
     * 
     * @param topicResolver The topic resolver to add the custom topic mapping to.
     * @param typeMapping The type mapping containing the topic name and message type.
     */
    private void addCustomTopicMapping(DefaultTopicResolver topicResolver, TypeMapping typeMapping) {
		String topicName = typeMapping.topicName();
		if (topicName != null) {
			topicResolver.addCustomTopicMapping(typeMapping.messageType(), topicName);
		}
	}

	/**
     * Creates and returns an instance of {@link PulsarFunctionAdministration} if no other bean of the same type is present.
     * The creation of this bean is conditional on the property "spring.pulsar.function.enabled" being set to "true".
     * If the property is not present, the bean will be created by default.
     * 
     * @param pulsarAdministration The {@link PulsarAdministration} bean used for Pulsar administration operations.
     * @param pulsarFunctions The {@link PulsarFunction} bean provider.
     * @param pulsarSinks The {@link PulsarSink} bean provider.
     * @param pulsarSources The {@link PulsarSource} bean provider.
     * @return An instance of {@link PulsarFunctionAdministration}.
     */
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
