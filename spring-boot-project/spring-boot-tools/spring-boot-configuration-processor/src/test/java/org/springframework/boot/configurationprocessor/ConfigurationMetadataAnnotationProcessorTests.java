/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.recursive.RecursiveProperties;
import org.springframework.boot.configurationsample.simple.ClassWithNestedProperties;
import org.springframework.boot.configurationsample.simple.DeprecatedFieldSingleProperty;
import org.springframework.boot.configurationsample.simple.DeprecatedSingleProperty;
import org.springframework.boot.configurationsample.simple.DescriptionProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesGrandparent;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesParent;
import org.springframework.boot.configurationsample.simple.NotAnnotated;
import org.springframework.boot.configurationsample.simple.SimpleArrayProperties;
import org.springframework.boot.configurationsample.simple.SimpleCollectionProperties;
import org.springframework.boot.configurationsample.simple.SimplePrefixValueProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleTypeProperties;
import org.springframework.boot.configurationsample.specific.AnnotatedGetter;
import org.springframework.boot.configurationsample.specific.BoxingPojo;
import org.springframework.boot.configurationsample.specific.BuilderPojo;
import org.springframework.boot.configurationsample.specific.DeprecatedUnrelatedMethodPojo;
import org.springframework.boot.configurationsample.specific.DoubleRegistrationProperties;
import org.springframework.boot.configurationsample.specific.ExcludedTypesPojo;
import org.springframework.boot.configurationsample.specific.InnerClassAnnotatedGetterConfig;
import org.springframework.boot.configurationsample.specific.InnerClassHierarchicalProperties;
import org.springframework.boot.configurationsample.specific.InnerClassProperties;
import org.springframework.boot.configurationsample.specific.InnerClassRootConfig;
import org.springframework.boot.configurationsample.specific.InvalidAccessorProperties;
import org.springframework.boot.configurationsample.specific.InvalidDefaultValueCharacterProperties;
import org.springframework.boot.configurationsample.specific.InvalidDefaultValueFloatingPointProperties;
import org.springframework.boot.configurationsample.specific.InvalidDefaultValueNumberProperties;
import org.springframework.boot.configurationsample.specific.InvalidDoubleRegistrationProperties;
import org.springframework.boot.configurationsample.specific.SimplePojo;
import org.springframework.boot.configurationsample.specific.StaticAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Jonas Keßler
 */
class ConfigurationMetadataAnnotationProcessorTests extends AbstractMetadataGenerationTests {

	@Test
	void notAnnotated() {
		ConfigurationMetadata metadata = compile(NotAnnotated.class);
		assertThat(metadata.getItems()).isEmpty();
	}

	@Test
	void simpleProperties() {
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class).withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class).withDefaultValue(false)
				.fromSource(SimpleProperties.class).withDescription("A simple flag.").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("simple.counter"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("simple.size"));
	}

	@Test
	void simplePrefixValueProperties() {
		ConfigurationMetadata metadata = compile(SimplePrefixValueProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimplePrefixValueProperties.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.name", String.class).fromSource(SimplePrefixValueProperties.class));
	}

	@Test
	void simpleTypeProperties() {
		ConfigurationMetadata metadata = compile(SimpleTypeProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple.type").fromSource(SimpleTypeProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-string", String.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-byte", Byte.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-byte", Byte.class).withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-char", Character.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-char", Character.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-boolean", Boolean.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-boolean", Boolean.class).withDefaultValue(false));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-short", Short.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-short", Short.class).withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-integer", Integer.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-integer", Integer.class).withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-long", Long.class));
		assertThat(metadata)
				.has(Metadata.withProperty("simple.type.my-primitive-long", Long.class).withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-double", Double.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-double", Double.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-float", Float.class));
		assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-float", Float.class));
		assertThat(metadata.getItems().size()).isEqualTo(18);
	}

	@Test
	void hierarchicalProperties() {
		ConfigurationMetadata metadata = compile(HierarchicalProperties.class, HierarchicalPropertiesParent.class,
				HierarchicalPropertiesGrandparent.class);
		assertThat(metadata).has(Metadata.withGroup("hierarchical").fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.first", String.class).withDefaultValue("one")
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.second", String.class).withDefaultValue("two")
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.third", String.class).withDefaultValue("three")
				.fromSource(HierarchicalProperties.class));
	}

	@Test
	void descriptionProperties() {
		ConfigurationMetadata metadata = compile(DescriptionProperties.class);
		assertThat(metadata).has(Metadata.withGroup("description").fromSource(DescriptionProperties.class));
		assertThat(metadata).has(Metadata.withProperty("description.simple", String.class)
				.fromSource(DescriptionProperties.class).withDescription("A simple description."));
		assertThat(metadata).has(Metadata.withProperty("description.multi-line", String.class)
				.fromSource(DescriptionProperties.class).withDescription(
						"This is a lengthy description that spans across multiple lines to showcase that the line separators are cleaned automatically."));
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedProperties() {
		Class<?> type = org.springframework.boot.configurationsample.simple.DeprecatedProperties.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("deprecated").fromSource(type));
		assertThat(metadata).has(
				Metadata.withProperty("deprecated.name", String.class).fromSource(type).withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("deprecated.description", String.class).fromSource(type)
				.withDeprecation(null, null));
	}

	@Test
	void singleDeprecatedProperty() {
		Class<?> type = DeprecatedSingleProperty.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("singledeprecated").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.new-name", String.class).fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class).fromSource(type)
				.withDeprecation("renamed", "singledeprecated.new-name"));
	}

	@Test
	void singleDeprecatedFieldProperty() {
		Class<?> type = DeprecatedFieldSingleProperty.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("singlefielddeprecated").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("singlefielddeprecated.name", String.class).fromSource(type)
				.withDeprecation(null, null));
	}

	@Test
	void deprecatedOnUnrelatedSetter() {
		Class<?> type = DeprecatedUnrelatedMethodPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
		assertThat(metadata).has(
				Metadata.withProperty("not.deprecated.counter", Integer.class).withNoDeprecation().fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("not.deprecated.flag", Boolean.class).withDefaultValue(false)
				.withNoDeprecation().fromSource(type));
	}

	@Test
	void boxingOnSetter() {
		Class<?> type = BoxingPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("boxing").fromSource(type));
		assertThat(metadata)
				.has(Metadata.withProperty("boxing.flag", Boolean.class).withDefaultValue(false).fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("boxing.counter", Integer.class).fromSource(type));
	}

	@Test
	void parseCollectionConfig() {
		ConfigurationMetadata metadata = compile(SimpleCollectionProperties.class);
		// getter and setter
		assertThat(metadata).has(Metadata.withProperty("collection.integers-to-names",
				"java.util.Map<java.lang.Integer,java.lang.String>"));
		assertThat(metadata).has(Metadata.withProperty("collection.longs", "java.util.Collection<java.lang.Long>"));
		assertThat(metadata).has(Metadata.withProperty("collection.floats", "java.util.List<java.lang.Float>"));
		// getter only
		assertThat(metadata).has(Metadata.withProperty("collection.names-to-integers",
				"java.util.Map<java.lang.String,java.lang.Integer>"));
		assertThat(metadata).has(Metadata.withProperty("collection.bytes", "java.util.Collection<java.lang.Byte>"));
		assertThat(metadata).has(Metadata.withProperty("collection.doubles", "java.util.List<java.lang.Double>"));
		assertThat(metadata).has(Metadata.withProperty("collection.names-to-holders",
				"java.util.Map<java.lang.String,org.springframework.boot.configurationsample.simple.SimpleCollectionProperties$Holder<java.lang.String>>"));
	}

	@Test
	void parseArrayConfig() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleArrayProperties.class);
		assertThat(metadata).has(Metadata.withGroup("array").ofType(SimpleArrayProperties.class));
		assertThat(metadata).has(Metadata.withProperty("array.primitive", "java.lang.Integer[]"));
		assertThat(metadata).has(Metadata.withProperty("array.simple", "java.lang.String[]"));
		assertThat(metadata).has(Metadata.withProperty("array.inner",
				"org.springframework.boot.configurationsample.simple.SimpleArrayProperties$Holder[]"));
		assertThat(metadata).has(
				Metadata.withProperty("array.name-to-integer", "java.util.Map<java.lang.String,java.lang.Integer>[]"));
		assertThat(metadata.getItems()).hasSize(5);
	}

	@Test
	void annotatedGetter() {
		ConfigurationMetadata metadata = compile(AnnotatedGetter.class);
		assertThat(metadata).has(Metadata.withGroup("specific").fromSource(AnnotatedGetter.class));
		assertThat(metadata)
				.has(Metadata.withProperty("specific.name", String.class).fromSource(AnnotatedGetter.class));
	}

	@Test
	void staticAccessor() {
		ConfigurationMetadata metadata = compile(StaticAccessor.class);
		assertThat(metadata).has(Metadata.withGroup("specific").fromSource(StaticAccessor.class));
		assertThat(metadata).has(Metadata.withProperty("specific.counter", Integer.class)
				.fromSource(StaticAccessor.class).withDefaultValue(42));
		assertThat(metadata)
				.doesNotHave(Metadata.withProperty("specific.name", String.class).fromSource(StaticAccessor.class));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void innerClassRootConfig() {
		ConfigurationMetadata metadata = compile(InnerClassRootConfig.class);
		assertThat(metadata).has(Metadata.withProperty("config.name"));
	}

	@Test
	void innerClassProperties() {
		ConfigurationMetadata metadata = compile(InnerClassProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config").fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withGroup("config.first").ofType(InnerClassProperties.Foo.class)
				.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
		assertThat(metadata).has(Metadata.withGroup("config.the-second", InnerClassProperties.Foo.class)
				.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.name"));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.bar.name"));
		assertThat(metadata).has(
				Metadata.withGroup("config.third").ofType(SimplePojo.class).fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.third.value"));
		assertThat(metadata).has(Metadata.withProperty("config.fourth"));
		assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
	}

	@Test
	void innerClassPropertiesHierarchical() {
		ConfigurationMetadata metadata = compile(InnerClassHierarchicalProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config.foo").ofType(InnerClassHierarchicalProperties.Foo.class));
		assertThat(metadata)
				.has(Metadata.withGroup("config.foo.bar").ofType(InnerClassHierarchicalProperties.Bar.class));
		assertThat(metadata)
				.has(Metadata.withGroup("config.foo.bar.baz").ofType(InnerClassHierarchicalProperties.Foo.Baz.class));
		assertThat(metadata).has(Metadata.withProperty("config.foo.bar.baz.blah"));
		assertThat(metadata).has(Metadata.withProperty("config.foo.bar.bling"));
	}

	@Test
	void innerClassAnnotatedGetterConfig() {
		ConfigurationMetadata metadata = compile(InnerClassAnnotatedGetterConfig.class);
		assertThat(metadata).has(Metadata.withProperty("specific.value"));
		assertThat(metadata).has(Metadata.withProperty("foo.name"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("specific.foo"));
	}

	@Test
	void nestedClassChildProperties() {
		ConfigurationMetadata metadata = compile(ClassWithNestedProperties.class);
		assertThat(metadata).has(
				Metadata.withGroup("nestedChildProps").fromSource(ClassWithNestedProperties.NestedChildClass.class));
		assertThat(metadata).has(Metadata.withProperty("nestedChildProps.child-class-property", Integer.class)
				.fromSource(ClassWithNestedProperties.NestedChildClass.class).withDefaultValue(20));
		assertThat(metadata).has(Metadata.withProperty("nestedChildProps.parent-class-property", Integer.class)
				.fromSource(ClassWithNestedProperties.NestedChildClass.class).withDefaultValue(10));
	}

	@Test
	void builderPojo() {
		ConfigurationMetadata metadata = compile(BuilderPojo.class);
		assertThat(metadata).has(Metadata.withProperty("builder.name"));
	}

	@Test
	void excludedTypesPojo() {
		ConfigurationMetadata metadata = compile(ExcludedTypesPojo.class);
		assertThat(metadata).has(Metadata.withProperty("excluded.name"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.class-loader"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.data-source"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.print-writer"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer"));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer-array"));
	}

	@Test
	void invalidAccessor() {
		ConfigurationMetadata metadata = compile(InvalidAccessorProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config"));
		assertThat(metadata.getItems()).hasSize(1);
	}

	@Test
	void doubleRegistration() {
		ConfigurationMetadata metadata = compile(DoubleRegistrationProperties.class);
		assertThat(metadata).has(Metadata.withGroup("one"));
		assertThat(metadata).has(Metadata.withGroup("two"));
		assertThat(metadata).has(Metadata.withProperty("one.value"));
		assertThat(metadata).has(Metadata.withProperty("two.value"));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	void invalidDoubleRegistration() {
		assertThatIllegalStateException().isThrownBy(() -> compile(InvalidDoubleRegistrationProperties.class))
				.withMessageContaining("Compilation failed");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnNumber() {
		assertThatIllegalStateException().isThrownBy(() -> compile(InvalidDefaultValueNumberProperties.class))
				.withMessageContaining("Compilation failed");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnFloatingPoint() {
		assertThatIllegalStateException().isThrownBy(() -> compile(InvalidDefaultValueFloatingPointProperties.class))
				.withMessageContaining("Compilation failed");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnCharacter() {
		assertThatIllegalStateException().isThrownBy(() -> compile(InvalidDefaultValueCharacterProperties.class))
				.withMessageContaining("Compilation failed");
	}

	@Test
	void recursivePropertiesDoNotCauseAStackOverflow() {
		compile(RecursiveProperties.class);
	}

}
