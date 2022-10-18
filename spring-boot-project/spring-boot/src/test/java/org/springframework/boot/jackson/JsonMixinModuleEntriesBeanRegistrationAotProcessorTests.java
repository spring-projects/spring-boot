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

package org.springframework.boot.jackson;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.boot.jackson.scan.a.RenameMixInClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JsonMixinModuleEntriesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
@CompileWithForkedClassLoader
class JsonMixinModuleEntriesBeanRegistrationAotProcessorTests {

	private final TestGenerationContext generationContext = new TestGenerationContext();

	private final GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();

	@Test
	void processAheadOfTimeShouldRegisterBindingHintsForMixins() {
		registerEntries(RenameMixInClass.class);
		processAheadOfTime();
		RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
		assertThat(RuntimeHintsPredicates.reflection().onMethod(Name.class, "getName")).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(NameAndAge.class, "getAge")).accepts(runtimeHints);
	}

	@Test
	void processAheadOfTimeWhenPublicClassShouldRegisterClass() {
		registerEntries(RenameMixInClass.class);
		compile((freshContext, compiled) -> {
			assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
			JsonMixinModuleEntries jsonMixinModuleEntries = freshContext.getBean(JsonMixinModuleEntries.class);
			assertThat(jsonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP).containsExactly(
					entry(Name.class, RenameMixInClass.class), entry(NameAndAge.class, RenameMixInClass.class));
		});
	}

	@Test
	void processAheadOfTimeWhenNonAccessibleClassShouldRegisterClassName() {
		Class<?> privateMixinClass = ClassUtils
				.resolveClassName("org.springframework.boot.jackson.scan.e.PrivateMixInClass", null);
		registerEntries(privateMixinClass);
		compile((freshContext, compiled) -> {
			assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
			JsonMixinModuleEntries jsonMixinModuleEntries = freshContext.getBean(JsonMixinModuleEntries.class);
			assertThat(jsonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP).containsExactly(
					entry(Name.class.getName(), privateMixinClass.getName()),
					entry(NameAndAge.class.getName(), privateMixinClass.getName()));
		});
	}

	private ClassName processAheadOfTime() {
		ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(this.applicationContext,
				this.generationContext);
		this.generationContext.writeGeneratedContent();
		return className;
	}

	@SuppressWarnings("unchecked")
	private void compile(BiConsumer<GenericApplicationContext, Compiled> result) {
		ClassName className = processAheadOfTime();
		TestCompiler.forSystem().with(this.generationContext).compile((compiled) -> {
			GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
			ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
					.getInstance(ApplicationContextInitializer.class, className.toString());
			initializer.initialize(freshApplicationContext);
			freshApplicationContext.refresh();
			result.accept(freshApplicationContext, compiled);
		});
	}

	private void registerEntries(Class<?>... basePackageClasses) {
		List<String> packageNames = Arrays.stream(basePackageClasses).map(Class::getPackageName).toList();
		this.applicationContext.registerBeanDefinition("configuration", BeanDefinitionBuilder
				.rootBeanDefinition(TestConfiguration.class).addConstructorArgValue(packageNames).getBeanDefinition());
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		public boolean scanningInvoked;

		private final Collection<String> packageNames;

		TestConfiguration(Collection<String> packageNames) {
			this.packageNames = packageNames;
		}

		@Bean
		JsonMixinModuleEntries jsonMixinModuleEntries(ApplicationContext applicationContext) {
			this.scanningInvoked = true;
			return JsonMixinModuleEntries.scan(applicationContext, this.packageNames);
		}

	}

}
