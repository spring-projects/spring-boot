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

package org.springframework.boot.testcontainers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImportTestcontainers}.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class ImportTestcontainersTests {

	private final TestGenerationContext generationContext = new TestGenerationContext();

	private AnnotationConfigApplicationContext applicationContext;

	@AfterEach
	void teardown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	void importWithoutValueRegistersBeans() {
		this.applicationContext = new AnnotationConfigApplicationContext(ImportWithoutValue.class);
		String[] beanNames = this.applicationContext.getBeanNamesForType(PostgreSQLContainer.class);
		assertThat(beanNames).hasSize(1);
		assertThat(this.applicationContext.getBean(beanNames[0])).isSameAs(ImportWithoutValue.container);
		TestcontainerBeanDefinition beanDefinition = (TestcontainerBeanDefinition) this.applicationContext
			.getBeanDefinition(beanNames[0]);
		assertThat(beanDefinition.getContainerImageName()).isEqualTo(ImportWithoutValue.container.getDockerImageName());
		assertThat(beanDefinition.getAnnotations().isPresent(ContainerAnnotation.class)).isTrue();
	}

	@Test
	void importWithValueRegistersBeans() {
		this.applicationContext = new AnnotationConfigApplicationContext(ImportWithValue.class);
		String[] beanNames = this.applicationContext.getBeanNamesForType(PostgreSQLContainer.class);
		assertThat(beanNames).hasSize(1);
		assertThat(this.applicationContext.getBean(beanNames[0])).isSameAs(ContainerDefinitions.container);
		TestcontainerBeanDefinition beanDefinition = (TestcontainerBeanDefinition) this.applicationContext
			.getBeanDefinition(beanNames[0]);
		assertThat(beanDefinition.getContainerImageName())
			.isEqualTo(ContainerDefinitions.container.getDockerImageName());
		assertThat(beanDefinition.getAnnotations().isPresent(ContainerAnnotation.class)).isTrue();
	}

	@Test
	void importWhenHasNoContainerFieldsDoesNothing() {
		this.applicationContext = new AnnotationConfigApplicationContext(NoContainers.class);
		String[] beanNames = this.applicationContext.getBeanNamesForType(Container.class);
		assertThat(beanNames).isEmpty();
	}

	@Test
	void importWhenHasNullContainerFieldThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.applicationContext = new AnnotationConfigApplicationContext(NullContainer.class))
			.withMessage("Container field 'container' must not have a null value");
	}

	@Test
	void importWhenHasNonStaticContainerFieldThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(
					() -> this.applicationContext = new AnnotationConfigApplicationContext(NonStaticContainer.class))
			.withMessage("Container field 'container' must be static");
	}

	@Test
	void importWhenHasContainerDefinitionsWithDynamicPropertySource() {
		this.applicationContext = new AnnotationConfigApplicationContext(
				ImportWithoutValueWithDynamicPropertySource.class);
		assertThat(this.applicationContext.getEnvironment().containsProperty("container.port")).isTrue();
	}

	@Test
	void importWhenHasNonStaticDynamicPropertySourceMethod() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.applicationContext = new AnnotationConfigApplicationContext(
					NonStaticDynamicPropertySourceMethod.class))
			.withMessage("@DynamicPropertySource method 'containerProperties' must be static");
	}

	@Test
	void importWhenHasBadArgsDynamicPropertySourceMethod() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.applicationContext = new AnnotationConfigApplicationContext(
					BadArgsDynamicPropertySourceMethod.class))
			.withMessage("@DynamicPropertySource method 'containerProperties' must be static");
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValueAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
		this.applicationContext.register(ImportWithoutValue.class);
		compile((freshContext, compiled) -> {
			PostgreSQLContainer<?> container = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(container).isSameAs(ImportWithoutValue.container);
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithValueAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
		this.applicationContext.register(ImportWithValue.class);
		compile((freshContext, compiled) -> {
			PostgreSQLContainer<?> container = freshContext.getBean(PostgreSQLContainer.class);
			assertThat(container).isSameAs(ContainerDefinitions.container);
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValueWithDynamicPropertySourceAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
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
	void importTestcontainersCustomPostgreSQLContainerDefinitionsAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
		this.applicationContext.register(CustomPostgreSQLContainerDefinitions.class);
		compile((freshContext, compiled) -> {
			CustomPostgreSQLContainer container = freshContext.getBean(CustomPostgreSQLContainer.class);
			assertThat(container).isSameAs(CustomPostgreSQLContainerDefinitions.container);
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void importTestcontainersImportWithoutValueNotAccessibleContainerAndDynamicPropertySourceAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
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
	void importTestcontainersWithNotAccessibleContainerAndDynamicPropertySourceAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
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
	void importTestcontainersMultipleContainersAndDynamicPropertySourcesAotContribution() {
		this.applicationContext = new AnnotationConfigApplicationContext();
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

	@ImportTestcontainers
	static class NoContainers {

	}

	@ImportTestcontainers
	static class NullContainer {

		static PostgreSQLContainer<?> container = null;

	}

	@ImportTestcontainers
	static class NonStaticContainer {

		PostgreSQLContainer<?> container = TestImage.container(PostgreSQLContainer.class);

	}

	interface ContainerDefinitions {

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
	static class NonStaticDynamicPropertySourceMethod {

		@DynamicPropertySource
		void containerProperties(DynamicPropertyRegistry registry) {
		}

	}

	@ImportTestcontainers
	static class BadArgsDynamicPropertySourceMethod {

		@DynamicPropertySource
		void containerProperties() {
		}

	}

	@ImportTestcontainers
	static class CustomPostgreSQLContainerDefinitions {

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
