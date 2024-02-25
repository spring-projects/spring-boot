/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ListableBeanFactory;
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
import org.springframework.graphql.execution.TypeDefinitionConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for creating a Spring GraphQL base
 * infrastructure.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class })
@ConditionalOnGraphQlSchema
@EnableConfigurationProperties(GraphQlProperties.class)
@ImportRuntimeHints(GraphQlAutoConfiguration.GraphQlResourcesRuntimeHints.class)
public class GraphQlAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlAutoConfiguration.class);

	private final ListableBeanFactory beanFactory;

	/**
     * Constructs a new instance of GraphQlAutoConfiguration with the specified beanFactory.
     *
     * @param beanFactory the ListableBeanFactory to be used for dependency injection
     */
    public GraphQlAutoConfiguration(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
     * Creates a bean of type GraphQlSource if no other bean of the same type is present.
     * 
     * This method uses the provided resourcePatternResolver, properties, exceptionResolvers,
     * subscriptionExceptionResolvers, instrumentations, wiringConfigurers, sourceCustomizers,
     * and typeDefinitionConfigurers to configure and build the GraphQlSource bean.
     * 
     * The schemaLocations are resolved using the resourcePatternResolver and the resolved
     * schemaResources are used to build the schema for the GraphQlSource.
     * 
     * The exceptionResolvers, subscriptionExceptionResolvers, and instrumentations are ordered
     * and added to the GraphQlSource builder.
     * 
     * If the inspection is enabled in the properties, the schema mappings are logged.
     * 
     * If the introspection is disabled in the properties, the enableIntrospection method is
     * used to configure the runtime wiring.
     * 
     * The ConnectionTypeDefinitionConfigurer is added to the GraphQlSource builder to configure
     * the type definitions.
     * 
     * The wiringConfigurers and sourceCustomizers are ordered and used to configure the runtime
     * wiring and customize the GraphQlSource builder respectively.
     * 
     * @param resourcePatternResolver The resource pattern resolver used to resolve the schema
     *                                locations.
     * @param properties              The GraphQlProperties used to configure the GraphQlSource.
     * @param exceptionResolvers      The exception resolvers used to handle data fetcher exceptions.
     * @param subscriptionExceptionResolvers The exception resolvers used to handle subscription exceptions.
     * @param instrumentations        The instrumentations used to instrument the GraphQL execution.
     * @param wiringConfigurers       The runtime wiring configurers used to configure the runtime wiring.
     * @param sourceCustomizers       The source customizers used to customize the GraphQlSource builder.
     * @param typeDefinitionConfigurers The type definition configurers used to configure the type definitions.
     * 
     * @return The created GraphQlSource bean.
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphQlSource graphQlSource(ResourcePatternResolver resourcePatternResolver, GraphQlProperties properties,
			ObjectProvider<DataFetcherExceptionResolver> exceptionResolvers,
			ObjectProvider<SubscriptionExceptionResolver> subscriptionExceptionResolvers,
			ObjectProvider<Instrumentation> instrumentations, ObjectProvider<RuntimeWiringConfigurer> wiringConfigurers,
			ObjectProvider<GraphQlSourceBuilderCustomizer> sourceCustomizers,
			ObjectProvider<TypeDefinitionConfigurer> typeDefinitionConfigurers) {
		String[] schemaLocations = properties.getSchema().getLocations();
		Resource[] schemaResources = resolveSchemaResources(resourcePatternResolver, schemaLocations,
				properties.getSchema().getFileExtensions());
		GraphQlSource.SchemaResourceBuilder builder = GraphQlSource.schemaResourceBuilder()
			.schemaResources(schemaResources)
			.exceptionResolvers(exceptionResolvers.orderedStream().toList())
			.subscriptionExceptionResolvers(subscriptionExceptionResolvers.orderedStream().toList())
			.instrumentation(instrumentations.orderedStream().toList());
		if (properties.getSchema().getInspection().isEnabled()) {
			builder.inspectSchemaMappings(logger::info);
		}
		if (!properties.getSchema().getIntrospection().isEnabled()) {
			builder.configureRuntimeWiring(this::enableIntrospection);
		}
		typeDefinitionConfigurers.forEach(builder::configureTypeDefinitions);
		builder.configureTypeDefinitions(new ConnectionTypeDefinitionConfigurer());
		wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring);
		sourceCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
     * Enables introspection for the given GraphQL builder.
     * 
     * @param wiring the GraphQL builder to enable introspection for
     * @return the GraphQL builder with introspection enabled
     */
    private Builder enableIntrospection(Builder wiring) {
		return wiring.fieldVisibility(NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY);
	}

	/**
     * Resolves the schema resources based on the given locations and extensions.
     * 
     * @param resolver the resource pattern resolver
     * @param locations the locations of the schema resources
     * @param extensions the file extensions of the schema resources
     * @return an array of resolved schema resources
     */
    private Resource[] resolveSchemaResources(ResourcePatternResolver resolver, String[] locations,
			String[] extensions) {
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			for (String extension : extensions) {
				resources.addAll(resolveSchemaResources(resolver, location + "*" + extension));
			}
		}
		return resources.toArray(new Resource[0]);
	}

	/**
     * Resolves the schema resources using the given resource pattern resolver and pattern.
     * 
     * @param resolver the resource pattern resolver to use
     * @param pattern the pattern to match the schema resources
     * @return a list of resolved schema resources
     */
    private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String pattern) {
		try {
			return Arrays.asList(resolver.getResources(pattern));
		}
		catch (IOException ex) {
			logger.debug(LogMessage.format("Could not resolve schema location: '%s'", pattern), ex);
			return Collections.emptyList();
		}
	}

	/**
     * Creates a new instance of BatchLoaderRegistry if no other bean of type BatchLoaderRegistry is present in the application context.
     * 
     * @return the BatchLoaderRegistry instance
     */
    @Bean
	@ConditionalOnMissingBean
	public BatchLoaderRegistry batchLoaderRegistry() {
		return new DefaultBatchLoaderRegistry();
	}

	/**
     * Creates an instance of ExecutionGraphQlService if no other bean of the same type is present.
     * 
     * @param graphQlSource The GraphQlSource used by the service.
     * @param batchLoaderRegistry The BatchLoaderRegistry used by the service.
     * @return An instance of ExecutionGraphQlService.
     */
    @Bean
	@ConditionalOnMissingBean
	public ExecutionGraphQlService executionGraphQlService(GraphQlSource graphQlSource,
			BatchLoaderRegistry batchLoaderRegistry) {
		DefaultExecutionGraphQlService service = new DefaultExecutionGraphQlService(graphQlSource);
		service.addDataLoaderRegistrar(batchLoaderRegistry);
		return service;
	}

	/**
     * Creates and configures an instance of {@link AnnotatedControllerConfigurer}.
     * This bean is conditional on the absence of another bean of the same type.
     * 
     * @param executorProvider The provider for the {@link Executor} bean.
     * @return The configured instance of {@link AnnotatedControllerConfigurer}.
     */
    @Bean
	@ConditionalOnMissingBean
	public AnnotatedControllerConfigurer annotatedControllerConfigurer(
			@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) ObjectProvider<Executor> executorProvider) {
		AnnotatedControllerConfigurer controllerConfigurer = new AnnotatedControllerConfigurer();
		controllerConfigurer
			.addFormatterRegistrar((registry) -> ApplicationConversionService.addBeans(registry, this.beanFactory));
		executorProvider.ifAvailable(controllerConfigurer::setExecutor);
		return controllerConfigurer;
	}

	/**
     * Returns the DataFetcherExceptionResolver obtained from the provided AnnotatedControllerConfigurer.
     * 
     * @param annotatedControllerConfigurer the AnnotatedControllerConfigurer used to obtain the DataFetcherExceptionResolver
     * @return the DataFetcherExceptionResolver obtained from the AnnotatedControllerConfigurer
     */
    @Bean
	DataFetcherExceptionResolver annotatedControllerConfigurerDataFetcherExceptionResolver(
			AnnotatedControllerConfigurer annotatedControllerConfigurer) {
		return annotatedControllerConfigurer.getExceptionResolver();
	}

	/**
     * GraphQlDataAutoConfiguration class.
     */
    @ConditionalOnClass(ScrollPosition.class)
	@Configuration(proxyBeanMethods = false)
	static class GraphQlDataAutoConfiguration {

		/**
         * Returns the encoding cursor strategy for the GraphQL data auto-configuration.
         * This method is annotated with @Bean to indicate that it is a bean definition method.
         * It is also annotated with @ConditionalOnMissingBean to specify that this bean should only be created if there is no existing bean of the same type.
         * The encoding cursor strategy is used to determine the cursor strategy for pagination using scroll positions.
         * The cursor strategy is created using the ScrollPositionCursorStrategy and the base64 cursor encoder.
         *
         * @return the encoding cursor strategy for the GraphQL data auto-configuration
         */
        @Bean
		@ConditionalOnMissingBean
		EncodingCursorStrategy<ScrollPosition> cursorStrategy() {
			return CursorStrategy.withEncoder(new ScrollPositionCursorStrategy(), CursorEncoder.base64());
		}

		/**
         * Customizes the GraphQlSourceBuilder with a cursor strategy.
         * 
         * @param cursorStrategy The cursor strategy to be used.
         * @return The customizer for the GraphQlSourceBuilder.
         */
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

	/**
     * GraphQlResourcesRuntimeHints class.
     */
    static class GraphQlResourcesRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers the GraphQL schema files as resources for the runtime hints.
         * 
         * @param hints the runtime hints object
         * @param classLoader the class loader to load the resources
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("graphql/*.graphqls").registerPattern("graphql/*.gqls");
		}

	}

}
