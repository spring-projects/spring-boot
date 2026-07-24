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

package org.springframework.boot.testcontainers.service.connection;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generate.CompilerFiles;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.test.context.aot.AotTestContextInitializers;
import org.springframework.test.context.aot.TestContextAotGenerator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnection} when used in AOT mode.
 *
 * @author Goutam Adwant
 */
@CompileWithForkedClassLoader
class ServiceConnectionAotTests {

	@Test
	void serviceConnectionOnBeanMethodIsAvailableAtAotRuntime() {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		Class<?> testClass = ExampleTest.class;
		generator.processAheadOfTime(Stream.of(testClass));
		TestCompiler.forSystem()
			.withCompilerOptions("-Xlint:deprecation,removal", "-Werror")
			.with(CompilerFiles.from(generatedFiles))
			.compile(ThrowingConsumer.of((compiled) -> assertCompiledTest(testClass)));
	}

	private void assertCompiledTest(Class<?> testClass) throws Exception {
		try {
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			resetAotClasses();
			AotTestContextInitializers aotContextInitializers = new AotTestContextInitializers();
			TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
			MergedContextConfiguration mergedConfig = testContextBootstrapper.buildMergedContextConfiguration();
			ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer = aotContextInitializers
				.getContextInitializer(testClass);
			assertThat(contextInitializer).isNotNull();
			try (ConfigurableApplicationContext context = (ConfigurableApplicationContext) ((AotContextLoader) mergedConfig
				.getContextLoader()).loadContextForAotRuntime(mergedConfig, contextInitializer)) {
				assertThat(context.getBeansOfType(DatabaseConnectionDetails.class)).hasSize(1);
				ContainerConnectionDetailsFactory.ContainerConnectionDetails<?> connectionDetails = (ContainerConnectionDetailsFactory.ContainerConnectionDetails<?>) context
					.getBean(DatabaseConnectionDetails.class);
				assertThat(connectionDetails.hasAnnotation(Ssl.class)).isTrue();
			}
		}
		finally {
			System.clearProperty(AotDetector.AOT_ENABLED);
			resetAotClasses();
		}
	}

	private void resetAotClasses() {
		reset("org.springframework.test.context.aot.AotTestAttributesFactory");
		reset("org.springframework.test.context.aot.AotTestContextInitializersFactory");
	}

	private void reset(String className) {
		Class<?> targetClass = ClassUtils.resolveClassName(className, null);
		ReflectionTestUtils.invokeMethod(targetClass, "reset");
	}

	@SpringBootTest(classes = ContainerConfiguration.class, webEnvironment = WebEnvironment.NONE)
	static class ExampleTest {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
	static class ContainerConfiguration {

		@Bean
		@ServiceConnection
		@Ssl
		PostgreSQLContainer postgresContainer() {
			return mock();
		}

	}

}
