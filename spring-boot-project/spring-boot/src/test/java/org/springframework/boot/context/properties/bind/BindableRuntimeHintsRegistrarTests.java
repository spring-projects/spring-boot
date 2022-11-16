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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.context.properties.BoundConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindableRuntimeHintsRegistrar}.
 *
 * @author Tumit Watcharapol
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class BindableRuntimeHintsRegistrarTests {

	@Test
	void registerHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		Class<?>[] types = { BoundConfigurationProperties.class, ConfigurationPropertiesBean.class };
		BindableRuntimeHintsRegistrar registrar = new BindableRuntimeHintsRegistrar(types);
		registrar.registerHints(runtimeHints);
		for (Class<?> type : types) {
			assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
		}
	}

	@Test
	void registerHintsWithIterable() {
		RuntimeHints runtimeHints = new RuntimeHints();
		List<Class<?>> types = Arrays.asList(BoundConfigurationProperties.class, ConfigurationPropertiesBean.class);
		BindableRuntimeHintsRegistrar registrar = BindableRuntimeHintsRegistrar.forTypes(types);
		registrar.registerHints(runtimeHints);
		for (Class<?> type : types) {
			assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
		}
	}

	@Test
	void registerHintsWhenNoClasses() {
		RuntimeHints runtimeHints = new RuntimeHints();
		BindableRuntimeHintsRegistrar registrar = new BindableRuntimeHintsRegistrar();
		registrar.registerHints(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(BoundConfigurationProperties.class))
				.rejects(runtimeHints);
	}

	@Test
	void registerHintsViaForType() {
		RuntimeHints runtimeHints = new RuntimeHints();
		Class<?>[] types = { BoundConfigurationProperties.class, ConfigurationPropertiesBean.class };
		BindableRuntimeHintsRegistrar registrar = BindableRuntimeHintsRegistrar.forTypes(types);
		registrar.registerHints(runtimeHints);
		for (Class<?> type : types) {
			assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
		}
	}

	@Test
	void registerHintsWhenJavaBean() {
		RuntimeHints runtimeHints = registerHints(JavaBean.class);
		assertThat(runtimeHints.reflection().getTypeHint(JavaBean.class)).satisfies(javaBeanBinding(JavaBean.class));
	}

	@Test
	void registerHintsWhenJavaBeanWithSeveralConstructors() throws NoSuchMethodException {
		RuntimeHints runtimeHints = registerHints(WithSeveralConstructors.class);
		assertThat(runtimeHints.reflection().getTypeHint(WithSeveralConstructors.class)).satisfies(
				javaBeanBinding(WithSeveralConstructors.class, WithSeveralConstructors.class.getDeclaredConstructor()));
	}

	@Test
	void registerHintsWhenJavaBeanWithMapOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithMap.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(WithMap.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(2);
	}

	@Test
	void registerHintsWhenJavaBeanWithListOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(WithList.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(2);
	}

	@Test
	void registerHintsWhenJavaBeanWitArrayOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithArray.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(WithArray.class));
		assertThat(typeHints).anySatisfy(javaBeanBinding(Address.class));
		assertThat(typeHints).hasSize(2);
	}

	@Test
	void registerHintsWhenJavaBeanWithListOfJavaType() {
		RuntimeHints runtimeHints = registerHints(WithSimpleList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(javaBeanBinding(WithSimpleList.class));
		assertThat(typeHints).hasSize(1);
	}

	@Test
	void registerHintsWhenValueObject() {
		RuntimeHints runtimeHints = registerHints(Immutable.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints)
				.anySatisfy(valueObjectBinding(Immutable.class, Immutable.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).hasSize(1);
	}

	@Test
	void registerHintsWhenValueObjectWithSpecificConstructor() throws NoSuchMethodException {
		RuntimeHints runtimeHints = registerHints(ImmutableWithSeveralConstructors.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(valueObjectBinding(ImmutableWithSeveralConstructors.class,
				ImmutableWithSeveralConstructors.class.getDeclaredConstructor(String.class)));
		assertThat(typeHints).hasSize(1);
	}

	@Test
	void registerHintsWhenValueObjectWithSeveralLayersOfPojo() {
		RuntimeHints runtimeHints = registerHints(ImmutableWithList.class);
		List<TypeHint> typeHints = runtimeHints.reflection().typeHints().toList();
		assertThat(typeHints).anySatisfy(
				valueObjectBinding(ImmutableWithList.class, ImmutableWithList.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).anySatisfy(valueObjectBinding(Person.class, Person.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).anySatisfy(valueObjectBinding(Address.class, Address.class.getDeclaredConstructors()[0]));
		assertThat(typeHints).hasSize(3);
	}

	@Test
	void registerHintsWhenHasNestedTypeNotUsedIsIgnored() {
		RuntimeHints runtimeHints = registerHints(WithNested.class);
		assertThat(runtimeHints.reflection().getTypeHint(WithNested.class))
				.satisfies(javaBeanBinding(WithNested.class));
	}

	@Test
	void registerHintsWhenWhenHasNestedExternalType() {
		RuntimeHints runtimeHints = registerHints(WithExternalNested.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(WithExternalNested.class))
				.anySatisfy(javaBeanBinding(SampleType.class)).anySatisfy(javaBeanBinding(SampleType.Nested.class))
				.hasSize(3);
	}

	@Test
	void registerHintsWhenWhenHasRecursiveType() {
		RuntimeHints runtimeHints = registerHints(WithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(WithRecursive.class))
				.anySatisfy(javaBeanBinding(Recursive.class)).hasSize(2);
	}

	@Test
	void registerHintsWhenValueObjectWithRecursiveType() {
		RuntimeHints runtimeHints = registerHints(ImmutableWithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints())
				.anySatisfy(valueObjectBinding(ImmutableWithRecursive.class,
						ImmutableWithRecursive.class.getDeclaredConstructors()[0]))
				.anySatisfy(valueObjectBinding(ImmutableRecursive.class,
						ImmutableRecursive.class.getDeclaredConstructors()[0]))
				.hasSize(2);
	}

	@Test
	void registerHintsWhenHasWellKnownTypes() {
		RuntimeHints runtimeHints = registerHints(WithWellKnownTypes.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(WithWellKnownTypes.class))
				.hasSize(1);
	}

	@Test
	void registerHintsWhenHasCrossReference() {
		RuntimeHints runtimeHints = registerHints(WithCrossReference.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(WithCrossReference.class))
				.anySatisfy(javaBeanBinding(CrossReferenceA.class)).anySatisfy(javaBeanBinding(CrossReferenceB.class))
				.hasSize(3);
	}

	@Test
	void pregisterHintsWhenHasUnresolvedGeneric() {
		RuntimeHints runtimeHints = registerHints(WithGeneric.class);
		assertThat(runtimeHints.reflection().typeHints()).anySatisfy(javaBeanBinding(WithGeneric.class))
				.anySatisfy(javaBeanBinding(GenericObject.class));
	}

	@Test
	void registerHintsWhenHasNestedGenerics() {
		RuntimeHints runtimeHints = registerHints(NestedGenerics.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.Nested.class)).accepts(runtimeHints);
	}

	@Test
	void registerHintsWhenHasMultipleNestedClasses() {
		RuntimeHints runtimeHints = registerHints(TripleNested.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.Nested.class))
				.accepts(runtimeHints);
	}

	private Consumer<TypeHint> javaBeanBinding(Class<?> type) {
		return javaBeanBinding(type, type.getDeclaredConstructors()[0]);
	}

	private Consumer<TypeHint> javaBeanBinding(Class<?> type, Constructor<?> constructor) {
		return (entry) -> {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
			assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).allMatch((t) -> t.getName().startsWith("set") || t.getName().startsWith("get")
					|| t.getName().startsWith("is"));
		};
	}

	private Consumer<TypeHint> valueObjectBinding(Class<?> type, Constructor<?> constructor) {
		return (entry) -> {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
			assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).isEmpty();
		};
	}

	private Consumer<ExecutableHint> match(Constructor<?> constructor) {
		return (executableHint) -> {
			assertThat(executableHint.getName()).isEqualTo("<init>");
			assertThat(Arrays.stream(constructor.getParameterTypes()).map(TypeReference::of).toList())
					.isEqualTo(executableHint.getParameterTypes());
		};
	}

	private RuntimeHints registerHints(Class<?>... types) {
		RuntimeHints hints = new RuntimeHints();
		BindableRuntimeHintsRegistrar.forTypes(types).registerHints(hints);
		return hints;
	}

	public static class JavaBean {

	}

	public static class WithSeveralConstructors {

		WithSeveralConstructors() {
		}

		WithSeveralConstructors(String ignored) {
		}

	}

	public static class WithMap {

		public Map<String, Address> getAddresses() {
			return Collections.emptyMap();
		}

	}

	public static class WithList {

		public List<Address> getAllAddresses() {
			return Collections.emptyList();
		}

	}

	public static class WithSimpleList {

		public List<String> getNames() {
			return Collections.emptyList();
		}

	}

	public static class WithArray {

		public Address[] getAllAddresses() {
			return new Address[0];
		}

	}

	public static class Immutable {

		@SuppressWarnings("unused")
		private final String name;

		Immutable(String name) {
			this.name = name;
		}

	}

	public static class ImmutableWithSeveralConstructors {

		@SuppressWarnings("unused")
		private final String name;

		@ConstructorBinding
		ImmutableWithSeveralConstructors(String name) {
			this.name = name;
		}

		ImmutableWithSeveralConstructors() {
			this("test");
		}

	}

	public static class ImmutableWithList {

		@SuppressWarnings("unused")
		private final List<Person> family;

		ImmutableWithList(List<Person> family) {
			this.family = family;
		}

	}

	public static class WithNested {

		static class OneLevelDown {

		}

	}

	public static class WithExternalNested {

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

	public static class WithRecursive {

		@NestedConfigurationProperty
		private Recursive recursive;

		public Recursive getRecursive() {
			return this.recursive;
		}

		public void setRecursive(Recursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class ImmutableWithRecursive {

		@NestedConfigurationProperty
		private ImmutableRecursive recursive;

		ImmutableWithRecursive(ImmutableRecursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class WithWellKnownTypes implements ApplicationContextAware, EnvironmentAware {

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

	public static class WithCrossReference {

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

	public static class WithGeneric {

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
