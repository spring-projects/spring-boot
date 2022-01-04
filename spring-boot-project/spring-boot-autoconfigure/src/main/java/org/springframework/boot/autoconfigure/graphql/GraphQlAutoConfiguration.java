/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;
import org.springframework.graphql.execution.ExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.MissingSchemaException;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for creating a Spring GraphQL base
 * infrastructure.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class })
@EnableConfigurationProperties(GraphQlProperties.class)
public class GraphQlAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlAutoConfiguration.class);

	private final BatchLoaderRegistry batchLoaderRegistry = new DefaultBatchLoaderRegistry();

	@Bean
	@ConditionalOnMissingBean
	public GraphQlSource graphQlSource(ResourcePatternResolver resourcePatternResolver, GraphQlProperties properties,
			ObjectProvider<DataFetcherExceptionResolver> exceptionResolversProvider,
			ObjectProvider<Instrumentation> instrumentationsProvider,
			ObjectProvider<RuntimeWiringConfigurer> wiringConfigurers,
			ObjectProvider<GraphQlSourceBuilderCustomizer> sourceCustomizers) {

		List<Resource> schemaResources = resolveSchemaResources(resourcePatternResolver,
				properties.getSchema().getLocations(), properties.getSchema().getFileExtensions());
		GraphQlSource.Builder builder = GraphQlSource.builder()
				.schemaResources(schemaResources.toArray(new Resource[0]))
				.exceptionResolvers(exceptionResolversProvider.orderedStream().collect(Collectors.toList()))
				.instrumentation(instrumentationsProvider.orderedStream().collect(Collectors.toList()));
		if (!properties.getSchema().getIntrospection().isEnabled()) {
			builder.configureRuntimeWiring((wiring) -> wiring
					.fieldVisibility(NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY));
		}
		wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring);
		sourceCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		try {
			return builder.build();
		}
		catch (MissingSchemaException exc) {
			throw new InvalidSchemaLocationsException(properties.getSchema().getLocations(), resourcePatternResolver,
					exc);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public BatchLoaderRegistry batchLoaderRegistry() {
		return this.batchLoaderRegistry;
	}

	@Bean
	@ConditionalOnMissingBean
	public GraphQlService graphQlService(GraphQlSource graphQlSource) {
		ExecutionGraphQlService service = new ExecutionGraphQlService(graphQlSource);
		service.addDataLoaderRegistrar(this.batchLoaderRegistry);
		return service;
	}

	@Bean
	@ConditionalOnMissingBean
	public AnnotatedControllerConfigurer annotatedControllerConfigurer() {
		AnnotatedControllerConfigurer annotatedControllerConfigurer = new AnnotatedControllerConfigurer();
		annotatedControllerConfigurer.setConversionService(new DefaultFormattingConversionService());
		return annotatedControllerConfigurer;
	}

	private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String[] schemaLocations,
			String[] fileExtensions) {
		List<Resource> schemaResources = new ArrayList<>();
		for (String location : schemaLocations) {
			for (String extension : fileExtensions) {
				String resourcePattern = location + "*" + extension;
				try {
					schemaResources.addAll(Arrays.asList(resolver.getResources(resourcePattern)));
				}
				catch (IOException ex) {
					logger.debug("Could not resolve schema location: '" + resourcePattern + "'", ex);
				}
			}
		}
		return schemaResources;
	}

}
