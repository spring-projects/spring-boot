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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceProviderCustomizerBeanRegistrationAotProcessor}.
 *
 * @author Moritz Halbritter
 */
class ResourceProviderCustomizerBeanRegistrationAotProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ResourceProviderCustomizerBeanRegistrationAotProcessor processor = new ResourceProviderCustomizerBeanRegistrationAotProcessor();

	@Test
	void beanRegistrationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
			.anyMatch(ResourceProviderCustomizerBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void shouldIgnoreNonResourceProviderCustomizerBeans() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationAotContribution contribution = this.processor
			.processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(contribution).isNull();
	}

	@Test
	@CompileWithForkedClassLoader
	void shouldReplaceResourceProviderCustomizer() {
		compile(createContext(ResourceProviderCustomizerConfiguration.class), (freshContext) -> {
			freshContext.refresh();
			ResourceProviderCustomizer bean = freshContext.getBean(ResourceProviderCustomizer.class);
			assertThat(bean).isInstanceOf(NativeImageResourceProviderCustomizer.class);
		});
	}

	private GenericApplicationContext createContext(Class<?>... types) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		Arrays.stream(types).forEach((type) -> context.registerBean(type));
		return context;
	}

	@SuppressWarnings("unchecked")
	private void compile(GenericApplicationContext context, Consumer<GenericApplicationContext> freshContext) {
		TestGenerationContext generationContext = new TestGenerationContext(TestTarget.class);
		ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile((compiled) -> {
			GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
			ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
				.getInstance(ApplicationContextInitializer.class, className.toString());
			initializer.initialize(freshApplicationContext);
			freshContext.accept(freshApplicationContext);
		});
	}

	static class TestTarget {

	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceProviderCustomizerConfiguration {

		@Bean
		ResourceProviderCustomizer resourceProviderCustomizer() {
			return new ResourceProviderCustomizer();
		}

	}

}
