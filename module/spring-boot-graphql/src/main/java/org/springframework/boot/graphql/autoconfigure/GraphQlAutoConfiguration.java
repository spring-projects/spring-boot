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

package org.springframework.boot.graphql.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.introspection.Introspection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.HandlerMethodArgumentResolver;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.data.pagination.ConnectionFieldTypeVisitor;
import org.springframework.graphql.data.pagination.CursorEncoder;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.graphql.data.pagination.EncodingCursorStrategy;
import org.springframework.graphql.data.query.ScrollPositionCursorStrategy;
import org.springframework.graphql.data.query.SliceConnectionAdapter;
import org.springframework.graphql.data.query.WindowConnectionAdapter;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.ConnectionTypeDefinitionConfigurer;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SubscriptionExceptionResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for creating a Spring GraphQL base
 * infrastructure.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class })
@ConditionalOnGraphQlSchema
@EnableConfigurationProperties(GraphQlProperties.class)
@ImportRuntimeHints(GraphQlAutoConfiguration.GraphQlResourcesRuntimeHints.class)
public final class GraphQlAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	GraphQlSource graphQlSource(ResourcePatternResolver resourcePatternResolver, GraphQlProperties properties,
			ObjectProvider<DataFetcherExceptionResolver> exceptionResolvers,
			ObjectProvider<SubscriptionExceptionResolver> subscriptionExceptionResolvers,
			ObjectProvider<Instrumentation> instrumentations, ObjectProvider<RuntimeWiringConfigurer> wiringConfigurers,
			ObjectProvider<GraphQlSourceBuilderCustomizer> sourceCustomizers) {

		String[] schemaLocations = properties.getSchema().getLocations();
		List<Resource> schemaResources = new ArrayList<>();
		schemaResources.addAll(resolveSchemaResources(resourcePatternResolver, schemaLocations,
				properties.getSchema().getFileExtensions()));
		schemaResources.addAll(Arrays.asList(properties.getSchema().getAdditionalFiles()));

		GraphQlSource.SchemaResourceBuilder builder = GraphQlSource.schemaResourceBuilder()
			.schemaResources(schemaResources.toArray(new Resource[0]))
			.exceptionResolvers(exceptionResolvers.orderedStream().toList())
			.subscriptionExceptionResolvers(subscriptionExceptionResolvers.orderedStream().toList())
			.instrumentation(instrumentations.orderedStream().toList());
		if (properties.getSchema().getInspection().isEnabled()) {
			builder.inspectSchemaMappings(logger::info);
		}
		if (!properties.getSchema().getIntrospection().isEnabled()) {
			Introspection.enabledJvmWide(false);
		}
		builder.configureTypeDefinitions(new ConnectionTypeDefinitionConfigurer());
		wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring);
		sourceCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String[] locations,
			String[] extensions) {
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			for (String extension : extensions) {
				resources.addAll(resolveSchemaResources(resolver, location + "*" + extension));
			}
		}
		return resources;
	}

	private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String pattern) {
		try {
			return Arrays.asList(resolver.getResources(pattern));
		}
		catch (IOException ex) {
			logger.debug(LogMessage.format("Could not resolve schema location: '%s'", pattern), ex);
			return Collections.emptyList();
		}
	}

	@Bean
	@ConditionalOnMissingBean
	BatchLoaderRegistry batchLoaderRegistry() {
		return new DefaultBatchLoaderRegistry();
	}

	@Bean
	@ConditionalOnMissingBean
	ExecutionGraphQlService executionGraphQlService(GraphQlSource graphQlSource,
			BatchLoaderRegistry batchLoaderRegistry) {
		DefaultExecutionGraphQlService service = new DefaultExecutionGraphQlService(graphQlSource);
		service.addDataLoaderRegistrar(batchLoaderRegistry);
		return service;
	}

	@Bean
	@ConditionalOnMissingBean
	AnnotatedControllerConfigurer annotatedControllerConfigurer(
			@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) ObjectProvider<Executor> executorProvider,
			ObjectProvider<HandlerMethodArgumentResolver> argumentResolvers) {
		AnnotatedControllerConfigurer controllerConfigurer = new AnnotatedControllerConfigurer();
		controllerConfigurer
			.configureBinder((options) -> options.conversionService(ApplicationConversionService.getSharedInstance()));
		executorProvider.ifAvailable(controllerConfigurer::setExecutor);
		argumentResolvers.orderedStream().forEach(controllerConfigurer::addCustomArgumentResolver);
		return controllerConfigurer;
	}

	@Bean
	DataFetcherExceptionResolver annotatedControllerConfigurerDataFetcherExceptionResolver(
			AnnotatedControllerConfigurer annotatedControllerConfigurer) {
		return annotatedControllerConfigurer.getExceptionResolver();
	}

	@ConditionalOnClass(ScrollPosition.class)
	@Configuration(proxyBeanMethods = false)
	static class GraphQlDataAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		EncodingCursorStrategy<ScrollPosition> cursorStrategy() {
			return CursorStrategy.withEncoder(new ScrollPositionCursorStrategy(), CursorEncoder.base64());
		}

		@Bean
		@SuppressWarnings("unchecked")
		GraphQlSourceBuilderCustomizer cursorStrategyCustomizer(CursorStrategy<?> cursorStrategy) {
			if (cursorStrategy.supports(ScrollPosition.class)) {
				CursorStrategy<ScrollPosition> scrollCursorStrategy = (CursorStrategy<ScrollPosition>) cursorStrategy;
				ConnectionFieldTypeVisitor connectionFieldTypeVisitor = ConnectionFieldTypeVisitor
					.create(List.of(new WindowConnectionAdapter(scrollCursorStrategy),
							new SliceConnectionAdapter(scrollCursorStrategy)));
				return (builder) -> builder.typeVisitors(List.of(connectionFieldTypeVisitor));
			}
			return (builder) -> {
			};
		}

	}

	static class GraphQlResourcesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerPattern("graphql/**/*.graphqls").registerPattern("graphql/**/*.gqls");
		}

	}

}
