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

package org.springframework.boot.test.context;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generate.CompilerFiles;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
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

/**
 * Tests for {@link SpringBootContextLoader} when used in AOT mode.
 *
 * @author Phillip Webb
 */
@CompileWithForkedClassLoader
class SpringBootContextLoaderAotTests {

	@Test
	void loadContextForAotProcessingAndAotRuntime() {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		Class<?> testClass = ExampleTest.class;
		generator.processAheadOfTime(Stream.of(testClass));
		TestCompiler.forSystem()
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
			ConfigurableApplicationContext context = (ConfigurableApplicationContext) ((AotContextLoader) mergedConfig
				.getContextLoader()).loadContextForAotRuntime(mergedConfig, contextInitializer);
			assertThat(context).isExactlyInstanceOf(GenericApplicationContext.class);
			String[] beanNames = context.getBeanNamesForType(ExampleBean.class);
			BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanNames[0]);
			assertThat(beanDefinition).isNotExactlyInstanceOf(GenericBeanDefinition.class);
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

	@SpringBootTest(classes = ExampleConfig.class, webEnvironment = WebEnvironment.NONE)
	static class ExampleTest {

	}

	@SpringBootConfiguration
	@Import(ExampleBean.class)
	static class ExampleConfig {

	}

	static class ExampleBean {

	}

}
