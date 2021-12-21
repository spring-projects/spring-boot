/*
 * Copyright 2012-2021 the original author or authors.
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

import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.MissingSchemaException;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GraphQlAutoConfiguration}.
 */
class GraphQlAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GraphQlAutoConfiguration.class));

	@Test
	void shouldContributeDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GraphQlSource.class);
			assertThat(context).hasSingleBean(BatchLoaderRegistry.class);
			assertThat(context).hasSingleBean(GraphQlService.class);
			assertThat(context).hasSingleBean(AnnotatedControllerConfigurer.class);
		});
	}

	@Test
	void schemaShouldScanNestedFolders() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GraphQlSource.class);
			GraphQlSource graphQlSource = context.getBean(GraphQlSource.class);
			GraphQLSchema schema = graphQlSource.schema();
			assertThat(schema.getObjectType("Book")).isNotNull();
		});
	}

	@Test
	void shouldFailWhenSchemaFileIsMissing() {
		this.contextRunner.withPropertyValues("spring.graphql.schema.locations:classpath:missing/").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().getRootCause().isInstanceOf(MissingSchemaException.class);
		});
	}

	@Test
	void shouldUseProgrammaticallyDefinedBuilder() {
		this.contextRunner.withUserConfiguration(CustomGraphQlBuilderConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customGraphQlSourceBuilder");
			assertThat(context).hasSingleBean(GraphQlSource.Builder.class);
		});
	}

	@Test
	void shouldScanLocationsWithCustomExtension() {
		this.contextRunner.withPropertyValues("spring.graphql.schema.file-extensions:.graphqls,.custom")
				.run((context) -> {
					assertThat(context).hasSingleBean(GraphQlSource.class);
					GraphQlSource graphQlSource = context.getBean(GraphQlSource.class);
					GraphQLSchema schema = graphQlSource.schema();
					assertThat(schema.getObjectType("Book")).isNotNull();
					assertThat(schema.getObjectType("Person")).isNotNull();
				});
	}

	@Test
	void shouldBackOffWithCustomGraphQlSource() {
		this.contextRunner.withUserConfiguration(CustomGraphQlSourceConfiguration.class).run((context) -> {
			assertThat(context).getBeanNames(GraphQlSource.class).containsOnly("customGraphQlSource");
			assertThat(context).hasSingleBean(GraphQlProperties.class);
		});
	}

	@Test
	void shouldConfigureDataFetcherExceptionResolvers() {
		this.contextRunner.withUserConfiguration(DataFetcherExceptionResolverConfiguration.class).run((context) -> {
			GraphQlSource graphQlSource = context.getBean(GraphQlSource.class);
			GraphQL graphQL = graphQlSource.graphQl();
			assertThat(graphQL.getQueryStrategy()).extracting("dataFetcherExceptionHandler")
					.satisfies((exceptionHandler) -> assertThat(exceptionHandler.getClass().getName())
							.endsWith("ExceptionResolversExceptionHandler"));
		});
	}

	@Test
	void shouldConfigureInstrumentation() {
		this.contextRunner.withUserConfiguration(InstrumentationConfiguration.class).run((context) -> {
			GraphQlSource graphQlSource = context.getBean(GraphQlSource.class);
			Instrumentation customInstrumentation = context.getBean("customInstrumentation", Instrumentation.class);
			GraphQL graphQL = graphQlSource.graphQl();
			assertThat(graphQL).extracting("instrumentation").isInstanceOf(ChainedInstrumentation.class)
					.extracting("instrumentations", InstanceOfAssertFactories.iterable(Instrumentation.class))
					.contains(customInstrumentation);
		});
	}

	@Test
	void shouldApplyRuntimeWiringConfigurers() {
		this.contextRunner.withUserConfiguration(RuntimeWiringConfigurerConfiguration.class).run((context) -> {
			RuntimeWiringConfigurerConfiguration.CustomRuntimeWiringConfigurer configurer = context
					.getBean(RuntimeWiringConfigurerConfiguration.CustomRuntimeWiringConfigurer.class);
			assertThat(configurer.applied).isTrue();
		});
	}

	@Test
	void shouldApplyGraphQlSourceBuilderCustomizer() {
		this.contextRunner.withUserConfiguration(GraphQlSourceBuilderCustomizerConfiguration.class).run((context) -> {
			GraphQlSourceBuilderCustomizerConfiguration.CustomGraphQlSourceBuilderCustomizer customizer = context
					.getBean(GraphQlSourceBuilderCustomizerConfiguration.CustomGraphQlSourceBuilderCustomizer.class);
			assertThat(customizer.applied).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGraphQlBuilderConfiguration {

		@Bean
		GraphQlSource.Builder customGraphQlSourceBuilder() {
			return GraphQlSource.builder().schemaResources(new ClassPathResource("graphql/schema.graphqls"),
					new ClassPathResource("graphql/types/book.graphqls"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGraphQlSourceConfiguration {

		@Bean
		GraphQlSource customGraphQlSource() {
			return mock(GraphQlSource.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetcherExceptionResolverConfiguration {

		@Bean
		DataFetcherExceptionResolver customDataFetcherExceptionResolver() {
			return mock(DataFetcherExceptionResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class InstrumentationConfiguration {

		@Bean
		Instrumentation customInstrumentation() {
			return mock(Instrumentation.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RuntimeWiringConfigurerConfiguration {

		@Bean
		CustomRuntimeWiringConfigurer customRuntimeWiringConfigurer() {
			return new CustomRuntimeWiringConfigurer();
		}

		public static class CustomRuntimeWiringConfigurer implements RuntimeWiringConfigurer {

			public boolean applied = false;

			@Override
			public void configure(RuntimeWiring.Builder builder) {
				this.applied = true;
			}

		}

	}

	static class GraphQlSourceBuilderCustomizerConfiguration {

		@Bean
		CustomGraphQlSourceBuilderCustomizer customGraphQlSourceBuilderCustomizer() {
			return new CustomGraphQlSourceBuilderCustomizer();
		}

		public static class CustomGraphQlSourceBuilderCustomizer implements GraphQlSourceBuilderCustomizer {

			public boolean applied = false;

			@Override
			public void customize(GraphQlSource.Builder builder) {
				this.applied = true;
			}

		}

	}

}
