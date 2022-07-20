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

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

	private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

	@Test
	void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(new AotFactoriesLoader(new DefaultListableBeanFactory())
				.load(BeanFactoryInitializationAotProcessor.class))
						.anyMatch(ConfigurationPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void processNoMatchesReturnsNullContribution() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(String.class));
		assertThat(this.processor.processAheadOfTime(beanFactory)).isNull();
	}

	@Test
	void processManuallyRegisteredSingleton() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("test", new SampleProperties());
		RuntimeHints runtimeHints = process(beanFactory);
		assertThat(runtimeHints.reflection().getTypeHint(SampleProperties.class))
				.satisfies(javaBeanBinding(SampleProperties.class));
	}

	@Test
	void registerConfigurationPropertiesAnnotation() {
		RuntimeHints runtimeHints = process(SampleProperties.class);
		assertThat(runtimeHints.reflection().getTypeHint(ConfigurationProperties.class)).satisfies(
				(hint) -> assertThat(hint.getMemberCategories()).contains(MemberCategory.INVOKE_DECLARED_METHODS));
	}

	@Test
	void processValueObjectConfigurationPropertiesWithSpecificConstructor() throws NoSuchMethodException {
		RuntimeHints runtimeHints = process(SampleImmutablePropertiesWithSeveralConstructors.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(valueObjectBinding(SampleImmutablePropertiesWithSeveralConstructors.class,
				SampleImmutablePropertiesWithSeveralConstructors.class.getDeclaredConstructor(String.class)));
		assertThat(typeHints).hasSize(2);
	}

	@Test
	void processConfigurationPropertiesWithNestedTypeNotUsedIsIgnored() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithNested.class);
		assertThat(runtimeHints.reflection().getTypeHint(SamplePropertiesWithNested.class))
				.satisfies(javaBeanBinding(SamplePropertiesWithNested.class));
	}

	@Test
	void processValueObjectConfigurationPropertiesWithRecursiveType() {
		RuntimeHints runtimeHints = process(SampleImmutablePropertiesWithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(valueObjectBinding(SampleImmutablePropertiesWithRecursive.class,
						SampleImmutablePropertiesWithRecursive.class.getDeclaredConstructors()[0]))
				.anySatisfy(valueObjectBinding(ImmutableRecursive.class,
						ImmutableRecursive.class.getDeclaredConstructors()[0]))
				.hasSize(3);
	}

	@Test
	void processConfigurationPropertiesWithCrossReference() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithCrossReference.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(javaBeanBinding(SamplePropertiesWithCrossReference.class))
				.anySatisfy(javaBeanBinding(CrossReferenceA.class)).anySatisfy(javaBeanBinding(CrossReferenceB.class))
				.hasSize(4);
	}

	@Test
	void processConfigurationPropertiesWithUnresolvedGeneric() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithGeneric.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(SamplePropertiesWithGeneric.class))
				.anySatisfy(javaBeanBinding(GenericObject.class));
	}

	private Consumer<TypeHint> javaBeanBinding(Class<?> type) {
		return javaBeanBinding(type, type.getDeclaredConstructors()[0]);
	}

	private Consumer<TypeHint> javaBeanBinding(Class<?> type, Constructor<?> constructor) {
		return (entry) -> {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
			assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
			assertThat(entry.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS);
		};
	}

	private Consumer<TypeHint> valueObjectBinding(Class<?> type, Constructor<?> constructor) {
		return (entry) -> {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
			assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
			assertThat(entry.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS);
		};
	}

	private Consumer<ExecutableHint> match(Constructor<?> constructor) {
		return (executableHint) -> {
			assertThat(executableHint.getName()).isEqualTo("<init>");
			assertThat(Arrays.stream(constructor.getParameterTypes()).map(TypeReference::of).toList())
					.isEqualTo(executableHint.getParameterTypes());
		};
	}

	private RuntimeHints process(Class<?>... types) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (Class<?> type : types) {
			beanFactory.registerBeanDefinition(type.getName(), new RootBeanDefinition(type));
		}
		return process(beanFactory);
	}

	private RuntimeHints process(ConfigurableListableBeanFactory beanFactory) {
		BeanFactoryInitializationAotContribution contribution = this.processor.processAheadOfTime(beanFactory);
		assertThat(contribution).isNotNull();
		GenerationContext generationContext = new DefaultGenerationContext(new ClassNameGenerator(Object.class),
				new InMemoryGeneratedFiles());
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
		return generationContext.getRuntimeHints();
	}

	@ConfigurationProperties("test")
	static class SampleProperties {

	}

	@ConfigurationProperties
	public static class SampleImmutablePropertiesWithSeveralConstructors {

		@SuppressWarnings("unused")
		private final String name;

		@ConstructorBinding
		SampleImmutablePropertiesWithSeveralConstructors(String name) {
			this.name = name;
		}

		SampleImmutablePropertiesWithSeveralConstructors() {
			this("test");
		}

	}

	@ConfigurationProperties("nested")
	public static class SamplePropertiesWithNested {

		static class OneLevelDown {

		}

	}

	@ConfigurationProperties
	public static class SampleImmutablePropertiesWithRecursive {

		@NestedConfigurationProperty
		private ImmutableRecursive recursive;

		SampleImmutablePropertiesWithRecursive(ImmutableRecursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class ImmutableRecursive {

		@SuppressWarnings("unused")
		private ImmutableRecursive recursive;

		ImmutableRecursive(ImmutableRecursive recursive) {
			this.recursive = recursive;
		}

	}

	@ConfigurationProperties("crossreference")
	public static class SamplePropertiesWithCrossReference {

		@NestedConfigurationProperty
		private CrossReferenceA crossReferenceA;

		public void setCrossReferenceA(CrossReferenceA crossReferenceA) {
			this.crossReferenceA = crossReferenceA;
		}

		public CrossReferenceA getCrossReferenceA() {
			return this.crossReferenceA;
		}

	}

	public static class CrossReferenceA {

		@NestedConfigurationProperty
		private CrossReferenceB crossReferenceB;

		public void setCrossReferenceB(CrossReferenceB crossReferenceB) {
			this.crossReferenceB = crossReferenceB;
		}

		public CrossReferenceB getCrossReferenceB() {
			return this.crossReferenceB;
		}

	}

	public static class CrossReferenceB {

		private CrossReferenceA crossReferenceA;

		public void setCrossReferenceA(CrossReferenceA crossReferenceA) {
			this.crossReferenceA = crossReferenceA;
		}

		public CrossReferenceA getCrossReferenceA() {
			return this.crossReferenceA;
		}

	}

	@ConfigurationProperties(prefix = "generic")
	public static class SamplePropertiesWithGeneric {

		@NestedConfigurationProperty
		private GenericObject<?> generic;

		public GenericObject<?> getGeneric() {
			return this.generic;
		}

	}

	public static final class GenericObject<T> {

		private final T value;

		GenericObject(T value) {
			this.value = value;
		}

		public T getValue() {
			return this.value;
		}

	}

}
