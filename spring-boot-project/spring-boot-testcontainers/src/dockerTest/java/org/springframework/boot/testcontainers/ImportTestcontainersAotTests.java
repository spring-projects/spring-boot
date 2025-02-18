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

package org.springframework.boot.testcontainers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AoT Tests for {@link ImportTestcontainers}.
 *
 * @author Dmytro Nosan
 */
@DisabledIfDockerUnavailable
class ImportTestcontainersAotTests {

	private final TestGenerationContext generationContext = new TestGenerationContext();

	private final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

	@AfterEach
	void teardown() {
		this.applicationContext.close();
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValue() {
		this.applicationContext.register(ImportWithoutValue.class);
		compile((freshContext, compiled) -> {
			PostgreSQLContainer<?> container = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(container).isSameAs(ImportWithoutValue.container);
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValueWithDynamicPropertySource() {
		this.applicationContext.register(ImportWithoutValueWithDynamicPropertySource.class);
		compile((freshContext, compiled) -> {
			PostgreSQLContainer<?> container = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(container).isSameAs(ImportWithoutValueWithDynamicPropertySource.container);
			assertThat(freshContext.getEnvironment().getProperty("container.port", Integer.class))
				.isEqualTo(ImportWithoutValueWithDynamicPropertySource.container.getFirstMappedPort());
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersCustomPostgreSQLContainerDefinitions() {
		this.applicationContext.register(CustomPostgresqlContainerDefinitions.class);
		compile((freshContext, compiled) -> {
			CustomPostgreSQLContainer container = freshContext.getBean(CustomPostgreSQLContainer.class);
			assertThat(container).isSameAs(CustomPostgresqlContainerDefinitions.container);
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValueNotAccessibleContainerAndDynamicPropertySource() {
		this.applicationContext.register(ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.class);
		compile((freshContext, compiled) -> {
			MongoDBContainer container = freshContext.getBean(MongoDBContainer.class);
			assertThat(container).isSameAs(ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.container);
			assertThat(freshContext.getEnvironment().getProperty("mongo.port", Integer.class)).isEqualTo(
					ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.container.getFirstMappedPort());
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersWithNotAccessibleContainerAndDynamicPropertySource() {
		this.applicationContext.register(ImportWithValueAndDynamicPropertySource.class);
		compile((freshContext, compiled) -> {
			PostgreSQLContainer<?> container = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(container).isSameAs(ContainerDefinitionsWithDynamicPropertySource.container);
			assertThat(freshContext.getEnvironment().getProperty("postgres.port", Integer.class))
				.isEqualTo(ContainerDefinitionsWithDynamicPropertySource.container.getFirstMappedPort());
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersMultipleContainersAndDynamicPropertySources() {
		this.applicationContext.register(ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.class);
		this.applicationContext.register(ImportWithValueAndDynamicPropertySource.class);
		compile((freshContext, compiled) -> {
			MongoDBContainer mongo = freshContext.getBean(MongoDBContainer.class);
			PostgreSQLContainer<?> postgres = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(mongo).isSameAs(ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.container);
			assertThat(postgres).isSameAs(ContainerDefinitionsWithDynamicPropertySource.container);
			ConfigurableEnvironment environment = freshContext.getEnvironment();
			assertThat(environment.getProperty("postgres.port", Integer.class))
				.isEqualTo(ContainerDefinitionsWithDynamicPropertySource.container.getFirstMappedPort());
			assertThat(environment.getProperty("mongo.port", Integer.class)).isEqualTo(
					ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource.container.getFirstMappedPort());
		});
	}

	@SuppressWarnings("unchecked")
	private void compile(BiConsumer<GenericApplicationContext, Compiled> result) {
		ClassName className = processAheadOfTime();
		TestCompiler.forSystem().with(this.generationContext).compile((compiled) -> {
			try (GenericApplicationContext context = new GenericApplicationContext()) {
				new TestcontainersLifecycleApplicationContextInitializer().initialize(context);
				ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
					.getInstance(ApplicationContextInitializer.class, className.toString());
				initializer.initialize(context);
				context.refresh();
				result.accept(context, compiled);
			}
		});
	}

	private ClassName processAheadOfTime() {
		ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(this.applicationContext,
				this.generationContext);
		this.generationContext.writeGeneratedContent();
		return className;
	}

	@ImportTestcontainers
	static class ImportWithoutValue {

		@ContainerAnnotation
		static PostgreSQLContainer<?> container = TestImage.container(PostgreSQLContainer.class);

	}

	@ImportTestcontainers(ContainerDefinitions.class)
	static class ImportWithValue {

	}

	static class ContainerDefinitions {

		@ContainerAnnotation
		PostgreSQLContainer<?> container = TestImage.container(PostgreSQLContainer.class);

	}

	private interface ContainerDefinitionsWithDynamicPropertySource {

		@ContainerAnnotation
		PostgreSQLContainer<?> container = TestImage.container(PostgreSQLContainer.class);

		@DynamicPropertySource
		static void containerProperties(DynamicPropertyRegistry registry) {
			registry.add("postgres.port", container::getFirstMappedPort);
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerAnnotation {

	}

	@ImportTestcontainers
	static class ImportWithoutValueWithDynamicPropertySource {

		static PostgreSQLContainer<?> container = TestImage.container(PostgreSQLContainer.class);

		@DynamicPropertySource
		static void containerProperties(DynamicPropertyRegistry registry) {
			registry.add("container.port", container::getFirstMappedPort);
		}

	}

	@ImportTestcontainers
	static class CustomPostgresqlContainerDefinitions {

		private static final CustomPostgreSQLContainer container = new CustomPostgreSQLContainer();

	}

	static class CustomPostgreSQLContainer extends PostgreSQLContainer<CustomPostgreSQLContainer> {

		CustomPostgreSQLContainer() {
			super("postgres:14");
		}

	}

	@ImportTestcontainers
	static class ImportWithoutValueNotAccessibleContainerAndDynamicPropertySource {

		private static final MongoDBContainer container = TestImage.container(MongoDBContainer.class);

		@DynamicPropertySource
		private static void containerProperties(DynamicPropertyRegistry registry) {
			registry.add("mongo.port", container::getFirstMappedPort);
		}

	}

	@ImportTestcontainers(ContainerDefinitionsWithDynamicPropertySource.class)
	static class ImportWithValueAndDynamicPropertySource {

	}

}
