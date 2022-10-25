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

package org.springframework.boot.configurationprocessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.test.RoundEnvironmentTester;
import org.springframework.boot.configurationprocessor.test.TestableAnnotationProcessor;
import org.springframework.boot.configurationsample.immutable.ImmutableClassConstructorBindingProperties;
import org.springframework.boot.configurationsample.immutable.ImmutableDeducedConstructorBindingProperties;
import org.springframework.boot.configurationsample.immutable.ImmutableMultiConstructorProperties;
import org.springframework.boot.configurationsample.immutable.ImmutableNameAnnotationProperties;
import org.springframework.boot.configurationsample.immutable.ImmutableSimpleProperties;
import org.springframework.boot.configurationsample.lombok.LombokExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleValueProperties;
import org.springframework.boot.configurationsample.simple.AutowiredProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesGrandparent;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesParent;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.specific.TwoConstructorsExample;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyDescriptorResolver}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class PropertyDescriptorResolverTests {

	@Test
	void propertiesWithJavaBeanProperties() {
		process(SimpleProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("theName", "flag", "comparator")));
	}

	@Test
	void propertiesWithJavaBeanHierarchicalProperties() {
		process(HierarchicalProperties.class,
				Arrays.asList(HierarchicalPropertiesParent.class, HierarchicalPropertiesGrandparent.class),
				(type, metadataEnv) -> {
					PropertyDescriptorResolver resolver = new PropertyDescriptorResolver(metadataEnv);
					assertThat(resolver.resolve(type, null).map(PropertyDescriptor::getName)).containsExactly("third",
							"second", "first");
					assertThat(resolver.resolve(type, null).map(
							(descriptor) -> descriptor.getGetter().getEnclosingElement().getSimpleName().toString()))
									.containsExactly("HierarchicalProperties", "HierarchicalPropertiesParent",
											"HierarchicalPropertiesParent");
					assertThat(resolver.resolve(type, null)
							.map((descriptor) -> descriptor.resolveItemMetadata("test", metadataEnv))
							.map(ItemMetadata::getDefaultValue)).containsExactly("three", "two", "one");
				});
	}

	@Test
	void propertiesWithLombokGetterSetterAtClassLevel() {
		process(LombokSimpleProperties.class, propertyNames(
				(stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
	}

	@Test
	void propertiesWithLombokGetterSetterAtFieldLevel() {
		process(LombokExplicitProperties.class, propertyNames(
				(stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
	}

	@Test
	void propertiesWithLombokDataClass() {
		process(LombokSimpleDataProperties.class, propertyNames(
				(stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
	}

	@Test
	void propertiesWithLombokValueClass() {
		process(LombokSimpleValueProperties.class, propertyNames(
				(stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
	}

	@Test
	void propertiesWithDeducedConstructorBinding() {
		process(ImmutableDeducedConstructorBindingProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("theName", "flag")));
		process(ImmutableDeducedConstructorBindingProperties.class, properties((stream) -> assertThat(stream)
				.allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
	}

	@Test
	void propertiesWithConstructorWithConstructorBinding() {
		process(ImmutableSimpleProperties.class, propertyNames(
				(stream) -> assertThat(stream).containsExactly("theName", "flag", "comparator", "counter")));
		process(ImmutableSimpleProperties.class, properties((stream) -> assertThat(stream)
				.allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
	}

	@Test
	void propertiesWithConstructorAndClassConstructorBinding() {
		process(ImmutableClassConstructorBindingProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("name", "description")));
		process(ImmutableClassConstructorBindingProperties.class, properties((stream) -> assertThat(stream)
				.allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
	}

	@Test
	void propertiesWithAutowiredConstructor() {
		process(AutowiredProperties.class, propertyNames((stream) -> assertThat(stream).containsExactly("theName")));
		process(AutowiredProperties.class, properties((stream) -> assertThat(stream)
				.allMatch((predicate) -> predicate instanceof JavaBeanPropertyDescriptor)));
	}

	@Test
	void propertiesWithMultiConstructor() {
		process(ImmutableMultiConstructorProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("name", "description")));
		process(ImmutableMultiConstructorProperties.class, properties((stream) -> assertThat(stream)
				.allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
	}

	@Test
	@Deprecated(since = "3.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void propertiesWithMultiConstructorAndDeprecatedAnnotation() {
		process(org.springframework.boot.configurationsample.immutable.DeprecatedImmutableMultiConstructorProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("name", "description")));
		process(org.springframework.boot.configurationsample.immutable.DeprecatedImmutableMultiConstructorProperties.class,
				properties((stream) -> assertThat(stream)
						.allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
	}

	@Test
	void propertiesWithMultiConstructorNoDirective() {
		process(TwoConstructorsExample.class, propertyNames((stream) -> assertThat(stream).containsExactly("name")));
		process(TwoConstructorsExample.class,
				properties((stream) -> assertThat(stream).element(0).isInstanceOf(JavaBeanPropertyDescriptor.class)));
	}

	@Test
	void propertiesWithNameAnnotationParameter() {
		process(ImmutableNameAnnotationProperties.class,
				propertyNames((stream) -> assertThat(stream).containsExactly("import")));
	}

	private BiConsumer<TypeElement, MetadataGenerationEnvironment> properties(
			Consumer<Stream<PropertyDescriptor<?>>> stream) {
		return (element, metadataEnv) -> {
			PropertyDescriptorResolver resolver = new PropertyDescriptorResolver(metadataEnv);
			stream.accept(resolver.resolve(element, null));
		};
	}

	private BiConsumer<TypeElement, MetadataGenerationEnvironment> propertyNames(Consumer<Stream<String>> stream) {
		return properties((result) -> stream.accept(result.map(PropertyDescriptor::getName)));
	}

	private void process(Class<?> target, BiConsumer<TypeElement, MetadataGenerationEnvironment> consumer) {
		process(target, Collections.emptyList(), consumer);
	}

	private void process(Class<?> target, Collection<Class<?>> additionalClasses,
			BiConsumer<TypeElement, MetadataGenerationEnvironment> consumer) {
		BiConsumer<RoundEnvironmentTester, MetadataGenerationEnvironment> internalConsumer = (roundEnv,
				metadataEnv) -> {
			TypeElement element = roundEnv.getRootElement(target);
			consumer.accept(element, metadataEnv);
		};
		TestableAnnotationProcessor<MetadataGenerationEnvironment> processor = new TestableAnnotationProcessor<>(
				internalConsumer, new MetadataGenerationEnvironmentFactory());
		SourceFile targetSource = SourceFile.forTestClass(target);
		List<SourceFile> additionalSource = additionalClasses.stream().map(SourceFile::forTestClass).toList();
		TestCompiler compiler = TestCompiler.forSystem().withProcessors(processor).withSources(targetSource)
				.withSources(additionalSource);
		compiler.compile((compiled) -> {
		});
	}

}
