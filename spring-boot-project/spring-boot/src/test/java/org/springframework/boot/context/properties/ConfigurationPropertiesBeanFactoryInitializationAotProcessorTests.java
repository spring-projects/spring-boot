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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

	private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

	@Test
	void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
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
	void processJavaBeanConfigurationProperties() {
		RuntimeHints runtimeHints = process(SampleProperties.class);
		assertThat(runtimeHints.reflection().getTypeHint(SampleProperties.class))
				.satisfies(javaBeanBinding(SampleProperties.class));
	}

	@Test
	void processJavaBeanConfigurationPropertiesWithSeveralConstructors() throws NoSuchMethodException {
		RuntimeHints runtimeHints = process(SamplePropertiesWithSeveralConstructors.class);
		assertThat(runtimeHints.reflection().getTypeHint(SamplePropertiesWithSeveralConstructors.class))
				.satisfies(javaBeanBinding(SamplePropertiesWithSeveralConstructors.class,
						SamplePropertiesWithSeveralConstructors.class.getDeclaredConstructor()));
	}

	@Test
	void processJavaBeanConfigurationPropertiesWithMapOfPojo() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithMap.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(SamplePropertiesWithMap.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(3);
	}

	@Test
	void processJavaBeanConfigurationPropertiesWithListOfPojo() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(SamplePropertiesWithList.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(3);
	}

	@Test
	void processJavaBeanConfigurationPropertiesWitArrayOfPojo() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithArray.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(SamplePropertiesWithArray.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(3);
	}

	@Test
	void processJavaBeanConfigurationPropertiesWithListOfJavaType() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithSimpleList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(SamplePropertiesWithSimpleList.class));
		assertThat(typeHints).hasSize(2);
	}

	@Test
	void processValueObjectConfigurationProperties() {
		RuntimeHints runtimeHints = process(SampleImmutableProperties.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(valueObjectBinding(SampleImmutableProperties.class,
				SampleImmutableProperties.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).hasSize(2);
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
	void processValueObjectConfigurationPropertiesWithSeveralLayersOfPojo() {
		RuntimeHints runtimeHints = process(SampleImmutablePropertiesWithList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(valueObjectBinding(SampleImmutablePropertiesWithList.class,
				SampleImmutablePropertiesWithList.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).anySatisfy(valueObjectBinding(Person.class, Person.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).anySatisfy(valueObjectBinding(Address.class, Address.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).hasSize(4);
	}

	@Test
	void processConfigurationPropertiesWithNestedTypeNotUsedIsIgnored() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithNested.class);
		assertThat(runtimeHints.reflection().getTypeHint(SamplePropertiesWithNested.class))
				.satisfies(javaBeanBinding(SamplePropertiesWithNested.class));
	}

	@Test
	void processConfigurationPropertiesWithNestedExternalType() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithExternalNested.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(javaBeanBinding(SamplePropertiesWithExternalNested.class))
				.anySatisfy(javaBeanBinding(SampleType.class)).anySatisfy(javaBeanBinding(SampleType.Nested.class))
				.hasSize(4);
	}

	@Test
	void processConfigurationPropertiesWithRecursiveType() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(javaBeanBinding(SamplePropertiesWithRecursive.class))
				.anySatisfy(javaBeanBinding(Recursive.class)).hasSize(3);
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
	void processConfigurationPropertiesWithWellKnownTypes() {
		RuntimeHints runtimeHints = process(SamplePropertiesWithWellKnownTypes.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(javaBeanBinding(SamplePropertiesWithWellKnownTypes.class))
				// TODO
				.hasSize(2);
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

	@Test
	void processConfigurationPropertiesWithNestedGenerics() {
		RuntimeHints runtimeHints = process(NestedGenerics.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.Nested.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(runtimeHints);
	}

	@Test
	void processConfigurationPropertiesWithMultipleNestedClasses() {
		RuntimeHints runtimeHints = process(TripleNested.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.Nested.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(runtimeHints);
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

	@ConfigurationProperties("test")
	public static class SamplePropertiesWithSeveralConstructors {

		SamplePropertiesWithSeveralConstructors() {
		}

		SamplePropertiesWithSeveralConstructors(String ignored) {
		}

	}

	@ConfigurationProperties("test")
	public static class SamplePropertiesWithMap {

		public Map<String, Address> getAddresses() {
			return Collections.emptyMap();
		}

	}

	@ConfigurationProperties("test")
	public static class SamplePropertiesWithList {

		public List<Address> getAllAddresses() {
			return Collections.emptyList();
		}

	}

	@ConfigurationProperties("test")
	public static class SamplePropertiesWithSimpleList {

		public List<String> getNames() {
			return Collections.emptyList();
		}

	}

	@ConfigurationProperties("test")
	public static class SamplePropertiesWithArray {

		public Address[] getAllAddresses() {
			return new Address[0];
		}

	}

	@ConfigurationProperties
	public static class SampleImmutableProperties {

		@SuppressWarnings("unused")
		private final String name;

		SampleImmutableProperties(String name) {
			this.name = name;
		}

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

	@ConfigurationProperties
	public static class SampleImmutablePropertiesWithList {

		@SuppressWarnings("unused")
		private final List<Person> family;

		SampleImmutablePropertiesWithList(List<Person> family) {
			this.family = family;
		}

	}

	@ConfigurationProperties("nested")
	public static class SamplePropertiesWithNested {

		static class OneLevelDown {

		}

	}

	@ConfigurationProperties("nested")
	public static class SamplePropertiesWithExternalNested {

		private String name;

		@NestedConfigurationProperty
		private SampleType sampleType;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SampleType getSampleType() {
			return this.sampleType;
		}

		public void setSampleType(SampleType sampleType) {
			this.sampleType = sampleType;
		}

	}

	@ConfigurationProperties("recursive")
	public static class SamplePropertiesWithRecursive {

		@NestedConfigurationProperty
		private Recursive recursive;

		public Recursive getRecursive() {
			return this.recursive;
		}

		public void setRecursive(Recursive recursive) {
			this.recursive = recursive;
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

	@ConfigurationProperties("wellKnownTypes")
	public static class SamplePropertiesWithWellKnownTypes implements ApplicationContextAware, EnvironmentAware {

		private ApplicationContext applicationContext;

		private Environment environment;

		public ApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		public Environment getEnvironment() {
			return this.environment;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

	public static class SampleType {

		private final Nested nested = new Nested();

		public Nested getNested() {
			return this.nested;
		}

		static class Nested {

		}

	}

	public static class Address {

	}

	public static class Person {

		@SuppressWarnings("unused")
		private final String firstName;

		@SuppressWarnings("unused")
		private final String lastName;

		@NestedConfigurationProperty
		private final Address address;

		Person(String firstName, String lastName, Address address) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.address = address;
		}

	}

	public static class Recursive {

		private Recursive recursive;

		public Recursive getRecursive() {
			return this.recursive;
		}

		public void setRecursive(Recursive recursive) {
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

	@ConfigurationProperties(prefix = "nested-generics")
	public static class NestedGenerics {

		private final Map<String, List<Nested>> nested = new HashMap<>();

		public Map<String, List<Nested>> getNested() {
			return this.nested;
		}

		public static class Nested {

			private String field;

			public String getField() {
				return this.field;
			}

			public void setField(String field) {
				this.field = field;
			}

		}

	}

	@ConfigurationProperties(prefix = "triple-nested")
	public static class TripleNested {

		private final DoubleNested doubleNested = new DoubleNested();

		public DoubleNested getDoubleNested() {
			return this.doubleNested;
		}

		public static class DoubleNested {

			private final Nested nested = new Nested();

			public Nested getNested() {
				return this.nested;
			}

			public static class Nested {

				private String field;

				public String getField() {
					return this.field;
				}

				public void setField(String field) {
					this.field = field;
				}

			}

		}

	}

}
