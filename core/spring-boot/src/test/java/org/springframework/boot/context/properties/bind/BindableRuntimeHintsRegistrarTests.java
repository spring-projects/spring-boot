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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.context.properties.BoundConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrarTests.BaseProperties.InheritedNested;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrarTests.ComplexNestedProperties.ListenerRetry;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrarTests.ComplexNestedProperties.Retry;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrarTests.ComplexNestedProperties.Simple;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
		BindableRuntimeHintsRegistrar registrar = new BindableRuntimeHintsRegistrar(new Class<?>[0]);
		registrar.registerHints(runtimeHints);
		assertThat(runtimeHints.reflection().typeHints()).isEmpty();
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
		assertThat(runtimeHints.reflection().typeHints()).singleElement().satisfies(javaBeanBinding(JavaBean.class));
	}

	@Test
	void registerHintsWhenJavaBeanWithSeveralConstructors() throws NoSuchMethodException {
		RuntimeHints runtimeHints = registerHints(WithSeveralConstructors.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(javaBeanBinding(WithSeveralConstructors.class)
				.constructor(WithSeveralConstructors.class.getDeclaredConstructor()));
	}

	@Test
	void registerHintsWhenJavaBeanWithMapOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithMap.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(javaBeanBinding(WithMap.class).methods("getAddresses"))
			.anySatisfy(javaBeanBinding(Address.class));
	}

	@Test
	void registerHintsWhenJavaBeanWithListOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithList.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(javaBeanBinding(WithList.class).methods("getAllAddresses"))
			.anySatisfy(javaBeanBinding(Address.class));
	}

	@Test
	void registerHintsWhenJavaBeanWitArrayOfPojo() {
		RuntimeHints runtimeHints = registerHints(WithArray.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(javaBeanBinding(WithArray.class).methods("getAllAddresses"))
			.anySatisfy(javaBeanBinding(Address.class));
	}

	@Test
	void registerHintsWhenJavaBeanWithListOfJavaType() {
		RuntimeHints runtimeHints = registerHints(WithSimpleList.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(javaBeanBinding(WithSimpleList.class).methods("getNames"));
	}

	@Test
	void registerHintsWhenValueObject() {
		RuntimeHints runtimeHints = registerHints(Immutable.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(valueObjectBinding(Immutable.class));
	}

	@Test
	void registerHintsWhenValueObjectWithSpecificConstructor() throws NoSuchMethodException {
		RuntimeHints runtimeHints = registerHints(ImmutableWithSeveralConstructors.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(valueObjectBinding(ImmutableWithSeveralConstructors.class,
					ImmutableWithSeveralConstructors.class.getDeclaredConstructor(String.class)));
	}

	@Test
	void registerHintsWhenValueObjectWithSeveralLayersOfPojo() {
		RuntimeHints runtimeHints = registerHints(ImmutableWithList.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
			.anySatisfy(valueObjectBinding(ImmutableWithList.class))
			.anySatisfy(valueObjectBinding(Person.class))
			.anySatisfy(valueObjectBinding(Address.class));
	}

	@Test
	void registerHintsWhenHasNestedTypeNotUsedIsIgnored() {
		RuntimeHints runtimeHints = registerHints(WithNested.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement().satisfies(javaBeanBinding(WithNested.class));
	}

	@Test
	void registerHintsWhenWhenHasNestedExternalType() {
		RuntimeHints runtimeHints = registerHints(WithExternalNested.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
			.anySatisfy(javaBeanBinding(WithExternalNested.class)
				.methods("getName", "setName", "getSampleType", "setSampleType")
				.fields("name", "sampleType"))
			.anySatisfy(javaBeanBinding(SampleType.class).methods("getNested").fields("nested"))
			.anySatisfy(javaBeanBinding(SampleType.Nested.class));
	}

	@Test
	void registerHintsWhenHasRecursiveType() {
		RuntimeHints runtimeHints = registerHints(WithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(
					javaBeanBinding(WithRecursive.class).methods("getRecursive", "setRecursive").fields("recursive"))
			.anySatisfy(javaBeanBinding(Recursive.class).methods("getRecursive", "setRecursive").fields("recursive"));
	}

	@Test
	void registerHintsWhenValueObjectWithRecursiveType() {
		RuntimeHints runtimeHints = registerHints(ImmutableWithRecursive.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(valueObjectBinding(ImmutableWithRecursive.class))
			.anySatisfy(valueObjectBinding(ImmutableRecursive.class));
	}

	@Test
	void registerHintsWhenHasWellKnownTypes() {
		RuntimeHints runtimeHints = registerHints(WithWellKnownTypes.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(javaBeanBinding(WithWellKnownTypes.class)
				.methods("getApplicationContext", "setApplicationContext", "getEnvironment", "setEnvironment")
				.fields("applicationContext", "environment"));
	}

	@Test
	void registerHintsWhenHasCrossReference() {
		RuntimeHints runtimeHints = registerHints(WithCrossReference.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
			.anySatisfy(javaBeanBinding(WithCrossReference.class).methods("getCrossReferenceA", "setCrossReferenceA")
				.fields("crossReferenceA"))
			.anySatisfy(javaBeanBinding(CrossReferenceA.class).methods("getCrossReferenceB", "setCrossReferenceB")
				.fields("crossReferenceB"))
			.anySatisfy(javaBeanBinding(CrossReferenceB.class).methods("getCrossReferenceA", "setCrossReferenceA")
				.fields("crossReferenceA"));
	}

	@Test
	void registerHintsWhenHasUnresolvedGeneric() {
		RuntimeHints runtimeHints = registerHints(WithGeneric.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
			.anySatisfy(javaBeanBinding(WithGeneric.class).methods("getGeneric").fields("generic"))
			.anySatisfy(javaBeanBinding(GenericObject.class));
	}

	@Test
	void registerHintsWhenHasNestedGenerics() {
		RuntimeHints runtimeHints = registerHints(NestedGenerics.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(2);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.Nested.class)).accepts(runtimeHints);
	}

	@Test
	void registerHintsWhenHasMultipleNestedClasses() {
		RuntimeHints runtimeHints = registerHints(TripleNested.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(3);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.class)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.Nested.class))
			.accepts(runtimeHints);
	}

	@Test
	void registerHintsWhenHasPackagePrivateGettersAndSetters() {
		RuntimeHints runtimeHints = registerHints(PackagePrivateGettersAndSetters.class);
		assertThat(runtimeHints.reflection().typeHints()).singleElement()
			.satisfies(javaBeanBinding(PackagePrivateGettersAndSetters.class)
				.methods("getAlpha", "setAlpha", "getBravo", "setBravo")
				.fields("alpha", "bravo"));
	}

	@Test
	void registerHintsWhenHasInheritedNestedProperties() {
		RuntimeHints runtimeHints = registerHints(ExtendingProperties.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(3);
		assertThat(runtimeHints.reflection().getTypeHint(BaseProperties.class)).satisfies((entry) -> {
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).extracting(ExecutableHint::getName)
				.containsExactlyInAnyOrder("getInheritedNested", "setInheritedNested");
		});
		assertThat(runtimeHints.reflection().getTypeHint(ExtendingProperties.class))
			.satisfies(javaBeanBinding(ExtendingProperties.class).methods("getBravo", "setBravo").fields("bravo"));
		assertThat(runtimeHints.reflection().getTypeHint(InheritedNested.class))
			.satisfies(javaBeanBinding(InheritedNested.class).methods("getAlpha", "setAlpha").fields("alpha"));
	}

	@Test
	void registerHintsWhenHasComplexNestedProperties() {
		RuntimeHints runtimeHints = registerHints(ComplexNestedProperties.class);
		assertThat(runtimeHints.reflection().typeHints()).hasSize(4);
		assertThat(runtimeHints.reflection().getTypeHint(Retry.class)).satisfies((entry) -> {
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).extracting(ExecutableHint::getName)
				.containsExactlyInAnyOrder("getCount", "setCount");
		});
		assertThat(runtimeHints.reflection().getTypeHint(ListenerRetry.class))
			.satisfies(javaBeanBinding(ListenerRetry.class).methods("isStateless", "setStateless").fields("stateless"));
		assertThat(runtimeHints.reflection().getTypeHint(Simple.class))
			.satisfies(javaBeanBinding(Simple.class).methods("getRetry").fields("retry"));
		assertThat(runtimeHints.reflection().getTypeHint(ComplexNestedProperties.class))
			.satisfies(javaBeanBinding(ComplexNestedProperties.class).methods("getSimple").fields("simple"));
	}

	@Test
	void registerHintsDoesNotThrowWhenParameterInformationForConstructorBindingIsNotAvailable()
			throws NoSuchMethodException, SecurityException {
		Constructor<?> constructor = PoolProperties.InterceptorProperty.class.getConstructor(String.class,
				String.class);
		@Nullable String[] parameterNames = new StandardReflectionParameterNameDiscoverer().getParameterNames(constructor);
		assertThat(parameterNames).isNull();
		assertThatNoException().isThrownBy(() -> registerHints(PoolProperties.class));
	}

	private JavaBeanBinding javaBeanBinding(Class<?> type) {
		return new JavaBeanBinding(type);
	}

	private Consumer<TypeHint> valueObjectBinding(Class<?> type) {
		return valueObjectBinding(type, type.getDeclaredConstructors()[0]);
	}

	private Consumer<TypeHint> valueObjectBinding(Class<?> type, Constructor<?> constructor) {
		return (entry) -> {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
			assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).isEmpty();
		};
	}

	private static Consumer<ExecutableHint> match(Constructor<?> constructor) {
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

		private @Nullable String name;

		@NestedConfigurationProperty
		private @Nullable SampleType sampleType;

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		public @Nullable SampleType getSampleType() {
			return this.sampleType;
		}

		public void setSampleType(@Nullable SampleType sampleType) {
			this.sampleType = sampleType;
		}

	}

	public static class WithRecursive {

		@NestedConfigurationProperty
		private @Nullable Recursive recursive;

		public @Nullable Recursive getRecursive() {
			return this.recursive;
		}

		public void setRecursive(@Nullable Recursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class ImmutableWithRecursive {

		@NestedConfigurationProperty
		private final @Nullable ImmutableRecursive recursive;

		ImmutableWithRecursive(@Nullable ImmutableRecursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class WithWellKnownTypes implements ApplicationContextAware, EnvironmentAware {

		@SuppressWarnings("NullAway.Init")
		private ApplicationContext applicationContext;

		@SuppressWarnings("NullAway.Init")
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

	public static class PackagePrivateGettersAndSetters {

		private @Nullable String alpha;

		private @Nullable Map<String, String> bravo;

		@Nullable String getAlpha() {
			return this.alpha;
		}

		void setAlpha(@Nullable String alpha) {
			this.alpha = alpha;
		}

		@Nullable Map<String, String> getBravo() {
			return this.bravo;
		}

		void setBravo(@Nullable Map<String, String> bravo) {
			this.bravo = bravo;
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

		private @Nullable Recursive recursive;

		public @Nullable Recursive getRecursive() {
			return this.recursive;
		}

		public void setRecursive(@Nullable Recursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class ImmutableRecursive {

		@SuppressWarnings("unused")
		private final @Nullable ImmutableRecursive recursive;

		ImmutableRecursive(@Nullable ImmutableRecursive recursive) {
			this.recursive = recursive;
		}

	}

	public static class WithCrossReference {

		@NestedConfigurationProperty
		private @Nullable CrossReferenceA crossReferenceA;

		public void setCrossReferenceA(@Nullable CrossReferenceA crossReferenceA) {
			this.crossReferenceA = crossReferenceA;
		}

		public @Nullable CrossReferenceA getCrossReferenceA() {
			return this.crossReferenceA;
		}

	}

	public static class CrossReferenceA {

		@NestedConfigurationProperty
		private @Nullable CrossReferenceB crossReferenceB;

		public void setCrossReferenceB(@Nullable CrossReferenceB crossReferenceB) {
			this.crossReferenceB = crossReferenceB;
		}

		public @Nullable CrossReferenceB getCrossReferenceB() {
			return this.crossReferenceB;
		}

	}

	public static class CrossReferenceB {

		private @Nullable CrossReferenceA crossReferenceA;

		public void setCrossReferenceA(@Nullable CrossReferenceA crossReferenceA) {
			this.crossReferenceA = crossReferenceA;
		}

		public @Nullable CrossReferenceA getCrossReferenceA() {
			return this.crossReferenceA;
		}

	}

	public static class WithGeneric {

		@NestedConfigurationProperty
		private @Nullable GenericObject<?> generic;

		public @Nullable GenericObject<?> getGeneric() {
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

			private @Nullable String field;

			public @Nullable String getField() {
				return this.field;
			}

			public void setField(@Nullable String field) {
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

				private @Nullable String field;

				public @Nullable String getField() {
					return this.field;
				}

				public void setField(@Nullable String field) {
					this.field = field;
				}

			}

		}

	}

	public abstract static class BaseProperties {

		private @Nullable InheritedNested inheritedNested;

		public @Nullable InheritedNested getInheritedNested() {
			return this.inheritedNested;
		}

		public void setInheritedNested(@Nullable InheritedNested inheritedNested) {
			this.inheritedNested = inheritedNested;
		}

		public static class InheritedNested {

			private @Nullable String alpha;

			public @Nullable String getAlpha() {
				return this.alpha;
			}

			public void setAlpha(@Nullable String alpha) {
				this.alpha = alpha;
			}

		}

	}

	public static class ExtendingProperties extends BaseProperties {

		private @Nullable String bravo;

		public @Nullable String getBravo() {
			return this.bravo;
		}

		public void setBravo(@Nullable String bravo) {
			this.bravo = bravo;
		}

	}

	public static class ComplexNestedProperties {

		private final Simple simple = new Simple();

		public Simple getSimple() {
			return this.simple;
		}

		public static class Simple {

			private final ListenerRetry retry = new ListenerRetry();

			public ListenerRetry getRetry() {
				return this.retry;
			}

		}

		public abstract static class Retry {

			private int count = 5;

			public int getCount() {
				return this.count;
			}

			public void setCount(int count) {
				this.count = count;
			}

		}

		public static class ListenerRetry extends Retry {

			private boolean stateless;

			public boolean isStateless() {
				return this.stateless;
			}

			public void setStateless(boolean stateless) {
				this.stateless = stateless;
			}

		}

	}

	private static final class JavaBeanBinding implements Consumer<TypeHint> {

		private final Class<?> type;

		private Constructor<?> constructor;

		private List<String> expectedMethods = Collections.emptyList();

		private List<String> expectedFields = Collections.emptyList();

		private JavaBeanBinding(Class<?> type) {
			this.type = type;
			this.constructor = this.type.getDeclaredConstructors()[0];
		}

		@Override
		public void accept(TypeHint entry) {
			assertThat(entry.getType()).isEqualTo(TypeReference.of(this.type));
			assertThat(entry.constructors()).singleElement().satisfies(match(this.constructor));
			assertThat(entry.getMemberCategories()).isEmpty();
			assertThat(entry.methods()).as("Methods requiring reflection")
				.extracting(ExecutableHint::getName)
				.containsExactlyInAnyOrderElementsOf(this.expectedMethods);
			assertThat(entry.fields()).as("Fields requiring reflection")
				.extracting(FieldHint::getName)
				.containsExactlyInAnyOrderElementsOf(this.expectedFields);
		}

		private JavaBeanBinding constructor(Constructor<?> constructor) {
			this.constructor = constructor;
			return this;
		}

		private JavaBeanBinding methods(String... methods) {
			this.expectedMethods = List.of(methods);
			return this;
		}

		private JavaBeanBinding fields(String... fields) {
			this.expectedFields = List.of(fields);
			return this;
		}

	}

}
