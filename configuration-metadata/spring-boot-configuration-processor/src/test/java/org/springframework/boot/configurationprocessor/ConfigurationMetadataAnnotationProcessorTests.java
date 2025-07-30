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

package org.springframework.boot.configurationprocessor;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemIgnore;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationprocessor.test.CompiledMetadataReader;
import org.springframework.boot.configurationprocessor.test.TestConfigurationMetadataAnnotationProcessor;
import org.springframework.boot.configurationsample.deprecation.Dbcp2Configuration;
import org.springframework.boot.configurationsample.method.NestedPropertiesMethod;
import org.springframework.boot.configurationsample.record.ExampleRecord;
import org.springframework.boot.configurationsample.record.NestedPropertiesRecord;
import org.springframework.boot.configurationsample.record.RecordWithGetter;
import org.springframework.boot.configurationsample.recursive.RecursiveProperties;
import org.springframework.boot.configurationsample.simple.ClassWithNestedProperties;
import org.springframework.boot.configurationsample.simple.DeprecatedFieldSingleProperty;
import org.springframework.boot.configurationsample.simple.DeprecatedRecord;
import org.springframework.boot.configurationsample.simple.DeprecatedSingleProperty;
import org.springframework.boot.configurationsample.simple.DescriptionProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalProperties;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesGrandparent;
import org.springframework.boot.configurationsample.simple.HierarchicalPropertiesParent;
import org.springframework.boot.configurationsample.simple.IgnoredProperties;
import org.springframework.boot.configurationsample.simple.InnerClassWithPrivateConstructor;
import org.springframework.boot.configurationsample.simple.NotAnnotated;
import org.springframework.boot.configurationsample.simple.SimpleArrayProperties;
import org.springframework.boot.configurationsample.simple.SimpleCollectionProperties;
import org.springframework.boot.configurationsample.simple.SimplePrefixValueProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleTypeProperties;
import org.springframework.boot.configurationsample.source.ConcreteProperties;
import org.springframework.boot.configurationsample.source.ConcreteSource;
import org.springframework.boot.configurationsample.source.ConcreteSourceAnnotated;
import org.springframework.boot.configurationsample.source.ConventionSource;
import org.springframework.boot.configurationsample.source.ConventionSourceAnnotated;
import org.springframework.boot.configurationsample.source.ImmutableSource;
import org.springframework.boot.configurationsample.source.ImmutableSourceAnnotated;
import org.springframework.boot.configurationsample.source.LombokSource;
import org.springframework.boot.configurationsample.source.LombokSourceAnnotated;
import org.springframework.boot.configurationsample.source.ParentWithHintProperties;
import org.springframework.boot.configurationsample.source.RecordSource;
import org.springframework.boot.configurationsample.source.RecordSourceAnnotated;
import org.springframework.boot.configurationsample.source.SimpleSource;
import org.springframework.boot.configurationsample.source.SimpleSourceAnnotated;
import org.springframework.boot.configurationsample.source.generation.AbstractPropertiesSource;
import org.springframework.boot.configurationsample.source.generation.ConfigurationPropertySourcesContainer;
import org.springframework.boot.configurationsample.source.generation.ConfigurationPropertySourcesContainer.First;
import org.springframework.boot.configurationsample.source.generation.ConfigurationPropertySourcesContainer.Second;
import org.springframework.boot.configurationsample.source.generation.ConfigurationPropertySourcesContainer.Third;
import org.springframework.boot.configurationsample.source.generation.ImmutablePropertiesSource;
import org.springframework.boot.configurationsample.source.generation.LombokPropertiesSource;
import org.springframework.boot.configurationsample.source.generation.NestedPropertiesSource;
import org.springframework.boot.configurationsample.source.generation.RecordPropertiesSources;
import org.springframework.boot.configurationsample.source.generation.SimplePropertiesSource;
import org.springframework.boot.configurationsample.specific.AnnotatedGetter;
import org.springframework.boot.configurationsample.specific.BoxingPojo;
import org.springframework.boot.configurationsample.specific.BuilderPojo;
import org.springframework.boot.configurationsample.specific.DeprecatedLessPreciseTypePojo;
import org.springframework.boot.configurationsample.specific.DeprecatedSimplePojo;
import org.springframework.boot.configurationsample.specific.DeprecatedUnrelatedMethodPojo;
import org.springframework.boot.configurationsample.specific.DoubleRegistrationProperties;
import org.springframework.boot.configurationsample.specific.EmptyDefaultValueProperties;
import org.springframework.boot.configurationsample.specific.EnumValuesPojo;
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
import org.springframework.core.test.tools.CompilationException;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.ResourceFile;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Pavel Anisimov
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class ConfigurationMetadataAnnotationProcessorTests extends AbstractMetadataGenerationTests {

	@Test
	void supportedAnnotations() {
		assertThat(new ConfigurationMetadataAnnotationProcessor().getSupportedAnnotationTypes())
			.containsExactlyInAnyOrder("org.springframework.boot.autoconfigure.AutoConfiguration",
					"org.springframework.boot.context.properties.ConfigurationProperties",
					"org.springframework.boot.context.properties.ConfigurationPropertiesSource",
					"org.springframework.context.annotation.Configuration",
					"org.springframework.boot.actuate.endpoint.annotation.Endpoint",
					"org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint",
					"org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint",
					"org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint",
					"org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint",
					"org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint");
	}

	@Test
	void notAnnotated() {
		ConfigurationMetadata metadata = compile(NotAnnotated.class);
		assertThat(metadata).isNull();
	}

	@Test
	void simpleProperties() {
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
			.fromSource(SimpleProperties.class)
			.withDescription("The name of this simple properties.")
			.withDefaultValue("boot")
			.withDeprecation());
		assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(SimpleProperties.class)
			.withDescription("A simple flag.")
			.withDeprecation());
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
		assertThat(metadata.getItems()).hasSize(18);
	}

	@Test
	void hierarchicalProperties() {
		ConfigurationMetadata metadata = compile(HierarchicalProperties.class, HierarchicalPropertiesParent.class,
				HierarchicalPropertiesGrandparent.class);
		assertThat(metadata).has(Metadata.withGroup("hierarchical").fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.first", String.class)
			.withDefaultValue("one")
			.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.second", String.class)
			.withDefaultValue("two")
			.fromSource(HierarchicalProperties.class));
		assertThat(metadata).has(Metadata.withProperty("hierarchical.third", String.class)
			.withDefaultValue("three")
			.fromSource(HierarchicalProperties.class));
	}

	@Test
	void enumValues() {
		ConfigurationMetadata metadata = compile(EnumValuesPojo.class);
		assertThat(metadata).has(Metadata.withGroup("test").fromSource(EnumValuesPojo.class));
		assertThat(metadata).has(Metadata.withProperty("test.seconds", ChronoUnit.class).withDefaultValue("seconds"));
		assertThat(metadata)
			.has(Metadata.withProperty("test.hour-of-day", ChronoField.class).withDefaultValue("hour-of-day"));
	}

	@Test
	void descriptionProperties() {
		ConfigurationMetadata metadata = compile(DescriptionProperties.class);
		assertThat(metadata).has(Metadata.withGroup("description").fromSource(DescriptionProperties.class));
		assertThat(metadata).has(Metadata.withProperty("description.simple", String.class)
			.fromSource(DescriptionProperties.class)
			.withDescription("A simple description."));
		assertThat(metadata).has(Metadata.withProperty("description.multi-line", String.class)
			.fromSource(DescriptionProperties.class)
			.withDescription(
					"This is a lengthy description that spans across multiple lines to showcase that the line separators are cleaned automatically."));
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedProperties() {
		Class<?> type = org.springframework.boot.configurationsample.simple.DeprecatedProperties.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("deprecated").fromSource(type));
		assertThat(metadata)
			.has(Metadata.withProperty("deprecated.name", String.class).fromSource(type).withDeprecation());
		assertThat(metadata)
			.has(Metadata.withProperty("deprecated.description", String.class).fromSource(type).withDeprecation());
	}

	@Test
	void singleDeprecatedProperty() {
		Class<?> type = DeprecatedSingleProperty.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("singledeprecated").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.new-name", String.class).fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class)
			.fromSource(type)
			.withDeprecation("renamed", "singledeprecated.new-name", "1.2.3"));
	}

	@Test
	void singleDeprecatedFieldProperty() {
		Class<?> type = DeprecatedFieldSingleProperty.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("singlefielddeprecated").fromSource(type));
		assertThat(metadata)
			.has(Metadata.withProperty("singlefielddeprecated.name", String.class).fromSource(type).withDeprecation());
	}

	@Test
	void deprecatedOnUnrelatedSetter() {
		Class<?> type = DeprecatedUnrelatedMethodPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
		assertThat(metadata)
			.has(Metadata.withProperty("not.deprecated.counter", Integer.class).withNoDeprecation().fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("not.deprecated.flag", Boolean.class)
			.withDefaultValue(false)
			.withNoDeprecation()
			.fromSource(type));
	}

	@Test
	void deprecatedWithLessPreciseType() {
		Class<?> type = DeprecatedLessPreciseTypePojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("not.deprecated.flag", Boolean.class)
			.withDefaultValue(false)
			.withNoDeprecation()
			.fromSource(type));
	}

	@Test
	void deprecatedPropertyOnRecord() {
		Class<?> type = DeprecatedRecord.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("deprecated-record").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("deprecated-record.alpha", String.class)
			.fromSource(type)
			.withDeprecation("some-reason", null, null));
		assertThat(metadata).has(Metadata.withProperty("deprecated-record.bravo", String.class).fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("deprecated-record.named.charlie", String.class)
			.fromSource(type)
			.withDeprecation("another-reason", null, null));
	}

	@Test
	void typBoxing() {
		Class<?> type = BoxingPojo.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("boxing").fromSource(type));
		assertThat(metadata)
			.has(Metadata.withProperty("boxing.flag", Boolean.class).withDefaultValue(false).fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("boxing.another-flag", Boolean.class).fromSource(type));
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
	void parseArrayConfig() {
		ConfigurationMetadata metadata = compile(SimpleArrayProperties.class);
		assertThat(metadata).has(Metadata.withGroup("array").ofType(SimpleArrayProperties.class));
		assertThat(metadata).has(Metadata.withProperty("array.primitive", "java.lang.Integer[]"));
		assertThat(metadata).has(Metadata.withProperty("array.simple", "java.lang.String[]"));
		assertThat(metadata).has(Metadata.withProperty("array.inner",
				"org.springframework.boot.configurationsample.simple.SimpleArrayProperties$Holder[]"));
		assertThat(metadata)
			.has(Metadata.withProperty("array.name-to-integer", "java.util.Map<java.lang.String,java.lang.Integer>[]"));
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
			.fromSource(StaticAccessor.class)
			.withDefaultValue(42));
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
		assertThat(metadata).has(Metadata.withGroup("config.first")
			.ofType(InnerClassProperties.Foo.class)
			.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
		assertThat(metadata).has(Metadata.withGroup("config.the-second", InnerClassProperties.Foo.class)
			.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.name"));
		assertThat(metadata).has(Metadata.withProperty("config.the-second.bar.name"));
		assertThat(metadata)
			.has(Metadata.withGroup("config.third").ofType(SimplePojo.class).fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.third.value"));
		assertThat(metadata).has(Metadata.withProperty("config.fourth"));
		assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
		assertThat(metadata).has(Metadata.withGroup("config.fifth")
			.ofType(DeprecatedSimplePojo.class)
			.fromSource(InnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.fifth.value").withDeprecation());
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
	void nestedClassMethod() {
		ConfigurationMetadata metadata = compile(NestedPropertiesMethod.class);
		assertThat(metadata).has(Metadata.withGroup("method-nested.nested"));
		assertThat(metadata).has(Metadata.withProperty("method-nested.nested.my-nested-property"));
		assertThat(metadata).has(Metadata.withGroup("method-nested.inner.nested"));
		assertThat(metadata).has(Metadata.withProperty("method-nested.inner.nested.my-nested-property"));
	}

	@Test
	void nestedClassChildProperties() {
		ConfigurationMetadata metadata = compile(ClassWithNestedProperties.class);
		assertThat(metadata)
			.has(Metadata.withGroup("nestedChildProps").fromSource(ClassWithNestedProperties.NestedChildClass.class));
		assertThat(metadata).has(Metadata.withProperty("nestedChildProps.child-class-property", Integer.class)
			.fromSource(ClassWithNestedProperties.NestedChildClass.class)
			.withDefaultValue(20));
		assertThat(metadata).has(Metadata.withProperty("nestedChildProps.parent-class-property", Integer.class)
			.fromSource(ClassWithNestedProperties.NestedChildClass.class)
			.withDefaultValue(10));
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
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(InvalidDoubleRegistrationProperties.class))
			.withMessageContaining("Unable to compile source");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnNumber() {
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(InvalidDefaultValueNumberProperties.class))
			.withMessageContaining("Unable to compile source");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnFloatingPoint() {
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(InvalidDefaultValueFloatingPointProperties.class))
			.withMessageContaining("Unable to compile source");
	}

	@Test
	void constructorParameterPropertyWithInvalidDefaultValueOnCharacter() {
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(InvalidDefaultValueCharacterProperties.class))
			.withMessageContaining("Unable to compile source");
	}

	@Test
	void constructorParameterPropertyWithEmptyDefaultValueOnProperty() {
		ConfigurationMetadata metadata = compile(EmptyDefaultValueProperties.class);
		assertThat(metadata).has(Metadata.withProperty("test.name"));
		ItemMetadata nameMetadata = metadata.getItems()
			.stream()
			.filter((item) -> item.getName().equals("test.name"))
			.findFirst()
			.get();
		assertThat(nameMetadata.getDefaultValue()).isNull();
	}

	@Test
	void recursivePropertiesDoNotCauseAStackOverflow() {
		compile(RecursiveProperties.class);
	}

	@Test
	void recordProperties() {
		String source = """
				@org.springframework.boot.configurationsample.ConfigurationProperties("implicit")
				public record ExampleRecord(String someString, Integer someInteger) {
				}
				""";
		ConfigurationMetadata metadata = compile(source);
		assertThat(metadata).has(Metadata.withProperty("implicit.some-string"));
		assertThat(metadata).has(Metadata.withProperty("implicit.some-integer"));
	}

	@Test
	void recordPropertiesWithDefaultValues() {
		String source = """
				@org.springframework.boot.configurationsample.ConfigurationProperties("record.defaults")
				public record ExampleRecord(
					@org.springframework.boot.configurationsample.DefaultValue("An1s9n") String someString,
					@org.springframework.boot.configurationsample.DefaultValue("594") Integer someInteger) {
				}
				""";
		ConfigurationMetadata metadata = compile(source);
		assertThat(metadata)
			.has(Metadata.withProperty("record.defaults.some-string", String.class).withDefaultValue("An1s9n"));
		assertThat(metadata)
			.has(Metadata.withProperty("record.defaults.some-integer", Integer.class).withDefaultValue(594));
	}

	@Test
	void multiConstructorRecordProperties() {
		String source = """
				@org.springframework.boot.configurationsample.ConfigurationProperties("multi")
				public record ExampleRecord(String someString, Integer someInteger) {
					@org.springframework.boot.configurationsample.ConstructorBinding
					public ExampleRecord(String someString) {
						this(someString, 42);
					}
					public ExampleRecord(Integer someInteger) {
						this("someString", someInteger);
					}
				}
				""";
		ConfigurationMetadata metadata = compile(source);
		assertThat(metadata).has(Metadata.withProperty("multi.some-string"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("multi.some-integer"));
	}

	@Test
	void innerClassWithPrivateConstructor() {
		ConfigurationMetadata metadata = compile(InnerClassWithPrivateConstructor.class);
		assertThat(metadata).has(Metadata.withProperty("config.nested.name"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("config.nested.ignored"));
	}

	@Test
	void recordWithGetter() {
		ConfigurationMetadata metadata = compile(RecordWithGetter.class);
		assertThat(metadata).has(Metadata.withProperty("record-with-getter.alpha"));
		assertThat(metadata).doesNotHave(Metadata.withProperty("record-with-getter.bravo"));
	}

	@Test
	void recordNested() {
		ConfigurationMetadata metadata = compile(NestedPropertiesRecord.class);
		assertThat(metadata).has(Metadata.withGroup("record-nested.nested"));
		assertThat(metadata).has(Metadata.withProperty("record-nested.nested.my-nested-property"));
		assertThat(metadata).has(Metadata.withGroup("record-nested.inner.nested"));
		assertThat(metadata).has(Metadata.withProperty("record-nested.inner.nested.my-nested-property"));
	}

	@Test
	void shouldNotMarkDbcp2UsernameOrPasswordAsDeprecated() {
		ConfigurationMetadata metadata = compile(Dbcp2Configuration.class);
		assertThat(metadata).has(Metadata.withProperty("spring.datasource.dbcp2.username").withNoDeprecation());
		assertThat(metadata).has(Metadata.withProperty("spring.datasource.dbcp2.password").withNoDeprecation());
	}

	@Test
	void recordPropertiesWithDescriptions() {
		ConfigurationMetadata metadata = compile(ExampleRecord.class);
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.some-string", String.class)
			.withDescription("very long description that doesn't fit single line and is indented"));
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.some-integer", Integer.class)
			.withDescription("description with @param and @ pitfalls"));
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.some-boolean", Boolean.class)
			.withDescription("description with extra spaces"));
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.some-long", Long.class)
			.withDescription("description without space after asterisk"));
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.some-byte", Byte.class)
			.withDescription("last description in Javadoc"));
		assertThat(metadata).has(Metadata.withProperty("record.descriptions.named.record.component", String.class)
			.withDescription("description of a named component"));
	}

	@Test
	void shouldIgnoreProperties() {
		String additionalMetadata = """
				{
					"ignored": {
						"properties": [
							{
								"name": "ignored.prop3"
							}
						]
					}
				}
				""";
		ConfigurationMetadata metadata = compile(additionalMetadata, IgnoredProperties.class);
		assertThat(metadata).has(Metadata.withProperty("ignored.prop1", String.class));
		assertThat(metadata).has(Metadata.withProperty("ignored.prop2", String.class));
		assertThat(metadata).doesNotHave(Metadata.withProperty("ignored.prop3", String.class));
		assertThat(metadata.getIgnored()).containsExactly(ItemIgnore.forProperty("ignored.prop3"));
	}

	@Nested
	class SourceTests {

		@Test
		void javaBeansSourceIsMergedWithNestedConfigurationProperty() {
			ConfigurationMetadata metadata = compile(SimpleSourceAnnotated.class, SimpleSource.class);
			assertThat(metadata).has(Metadata.withGroup("example.nested", SimpleSource.class))
				.has(Metadata.withProperty("example.nested.name", String.class).withDescription("Name description."))
				.has(Metadata.withProperty("example.nested.description", String.class)
					.withDescription("Description description.")
					.withDefaultValue("Hello World"))
				.has(Metadata.withProperty("example.nested.type", String.class)
					.withDescription("A property with a fixed set of values.")
					.withDefaultValue("single"));
		}

		@Test
		void lombokSourceIsMergedWithNestedConfigurationProperty() {
			ConfigurationMetadata metadata = compile(LombokSourceAnnotated.class, LombokSource.class);
			assertThat(metadata).has(Metadata.withGroup("example.nested", LombokSource.class))
				.has(Metadata.withProperty("example.nested.name", String.class).withDescription("Name description."))
				.has(Metadata.withProperty("example.nested.description", String.class)
					.withDescription("Description description.")
					.withDefaultValue("Hello World"))
				.has(Metadata.withProperty("example.nested.type", String.class)
					.withDescription("A property with a fixed set of values.")
					.withDefaultValue("single"));
		}

		@Test
		void immutableSourceIsMergedWithNestedConfigurationProperty() {
			ConfigurationMetadata metadata = compile(ImmutableSourceAnnotated.class, ImmutableSource.class);
			assertThat(metadata).has(Metadata.withGroup("example.nested", ImmutableSource.class))
				.has(Metadata.withProperty("example.nested.name", String.class).withDescription("Name description."))
				.has(Metadata.withProperty("example.nested.description", String.class)
					.withDescription("Description description.")
					.withDefaultValue("Hello World"))
				.has(Metadata.withProperty("example.nested.type", String.class)
					.withDescription("A property with a fixed set of values.")
					.withDefaultValue("single"));
		}

		@Test
		void recordSourceIsMergedWithNestedConfigurationProperty() {
			ConfigurationMetadata metadata = compile(RecordSourceAnnotated.class, RecordSource.class);
			assertThat(metadata).has(Metadata.withGroup("example.nested", RecordSource.class))
				.has(Metadata.withProperty("example.nested.name", String.class).withDescription("Name description."))
				.has(Metadata.withProperty("example.nested.description", String.class)
					.withDescription("Description description.")
					.withDefaultValue("Hello World"))
				.has(Metadata.withProperty("example.nested.type", String.class)
					.withDescription("A property with a fixed set of values.")
					.withDefaultValue("single"));
		}

		@Test
		void sourceIsMergedWithConfigurationProperties() {
			ConfigurationMetadata metadata = compile(ParentWithHintProperties.class);
			assertThat(metadata).has(Metadata.withGroup("example", ParentWithHintProperties.class))
				.has(Metadata.withProperty("example.name", String.class).withDescription("Name description."))
				.has(Metadata.withProperty("example.description", String.class)
					.withDescription("Description description.")
					.withDefaultValue("Hello World"))
				.has(Metadata.withProperty("example.type", String.class)
					.withDescription("A property with a fixed set of values.")
					.withDefaultValue("single"))
				.has(Metadata.withProperty("example.enabled", Boolean.class)
					.withDescription("Whether this is enabled.")
					.withDefaultValue(false));
		}

		@Test
		void sourceHintIsMergedWithNestedConfigurationProperty() {
			ConfigurationMetadata metadata = compile(SimpleSourceAnnotated.class);
			assertThat(metadata).has(Metadata.withHint("example.nested.type")
				.withValue(0, "auto", "Detect the type automatically.")
				.withValue(1, "single", "Single type.")
				.withValue(2, "multi", "Multi type."));
		}

		@Test
		void sourceHintIsMergedWithConfigurationProperties() {
			ConfigurationMetadata metadata = compile(ParentWithHintProperties.class);
			assertThat(metadata).has(Metadata.withHint("example.type")
				.withValue(0, "auto", "Detect the type automatically.")
				.withValue(1, "single", "Single type.")
				.withValue(2, "multi", "Multi type."));
		}

		@Test
		void sourceWithNonCanonicalMetadataIsDiscovered() {
			ConfigurationMetadata metadata = compile(ConventionSourceAnnotated.class);
			assertThat(metadata).has(Metadata.withGroup("example.nested", ConventionSource.class))
				.has(Metadata.withProperty("example.nested.first-name", String.class).withDescription("Camel case."))
				.has(Metadata.withProperty("example.nested.last-name", String.class)
					.withDescription("Canonical format."));
			assertThat(metadata.getItems()).hasSize(4);
		}

		@Test
		void sourceFromParentClasIsDiscoveredForConcreteSource() {
			ConfigurationMetadata metadata = compile(ConcreteSourceAnnotated.class, ConcreteSource.class);
			assertThat(metadata).has(Metadata.withGroup("example", ConcreteSourceAnnotated.class))
				.has(Metadata.withGroup("example.nested", ConcreteSource.class))
				.has(Metadata.withProperty("example.nested.enabled", Boolean.class)
					.withDescription("Whether the feature is enabled."))
				.has(Metadata.withProperty("example.nested.username", String.class)
					.withDescription("User name.")
					.withDefaultValue("user"))
				.has(Metadata.withProperty("example.nested.password", String.class).withDescription("Password."));
			assertThat(metadata.getItems()).hasSize(5);
		}

		@Test
		void sourceFromParentClasIsDiscoveredForConfigurationProperties() {
			ConfigurationMetadata metadata = compile(ConcreteProperties.class);
			assertThat(metadata).has(Metadata.withGroup("example", ConcreteProperties.class))
				.has(Metadata.withProperty("example.enabled", Boolean.class)
					.withDescription("Whether the feature is enabled."))
				.has(Metadata.withProperty("example.username", String.class)
					.withDescription("User name.")
					.withDefaultValue("user"))
				.has(Metadata.withProperty("example.password", String.class).withDescription("Password."));
			assertThat(metadata.getItems()).hasSize(4);
		}

	}

	@Nested
	class SourceGenerationTests {

		@Test
		void simplePropertiesSource() {
			compile(withTestClasses(SimplePropertiesSource.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(SimplePropertiesSource.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name")
						.ofType(String.class)
						.withDefaultValue("boot")
						.withDescription("Description of this simple property."))
					.has(Metadata.withProperty("enabled")
						.ofType(Boolean.class)
						.withDefaultValue(false)
						.withDescription("Whether it is enabled."));
				assertThat(metadata.getItems()).hasSize(2);
				assertThat(metadata.getHints()).isEmpty();
			});
		}

		@Test
		void simplePropertiesSourceWithAdditionalMetadataIsMerged() {
			String additionalMetadata = """
					{
					   "properties": [
						 {
						   "name": "custom",
						   "type": "java.lang.Integer",
						   "description": "Custom property description."
						 }
					   ]
					 }""";
			compile(withTestClasses(SimplePropertiesSource.class)
				.andThen(withAdditionalMetadata(SimplePropertiesSource.class, additionalMetadata)), (compiled) -> {
					ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
							getSourceMetadataLocation(SimplePropertiesSource.class));
					assertThat(metadata).isNotNull()
						.has(Metadata.withProperty("name")
							.ofType(String.class)
							.withDefaultValue("boot")
							.withDescription("Description of this simple property."))
						.has(Metadata.withProperty("enabled")
							.ofType(Boolean.class)
							.withDefaultValue(false)
							.withDescription("Whether it is enabled."))
						.has(Metadata.withProperty("custom")
							.ofType(Integer.class)
							.withDescription("Custom property description."));
					assertThat(metadata.getItems()).hasSize(3);
				});
		}

		@Test
		void simplePropertiesSourceWithAdditionalMetadataHintIsMerged() {
			String additionalMetadata = """
					{
					 "hints": [
					   {
						 "name": "name",
						 "values": [
						   { "value": "boot", "description": "Spring Boot." },
						   { "value": "framework", "description": "Spring Framework." }
						 ]
					   }
					 ]
					}""";
			compile(withTestClasses(SimplePropertiesSource.class)
				.andThen(withAdditionalMetadata(SimplePropertiesSource.class, additionalMetadata)), (compiled) -> {
					ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
							getSourceMetadataLocation(SimplePropertiesSource.class));
					assertThat(metadata).isNotNull()
						.has(Metadata.withProperty("name")
							.ofType(String.class)
							.withDefaultValue("boot")
							.withDescription("Description of this simple property."))
						.has(Metadata.withProperty("enabled")
							.ofType(Boolean.class)
							.withDefaultValue(false)
							.withDescription("Whether it is enabled."))
						.has(Metadata.withHint("name")
							.withValue(0, "boot", "Spring Boot.")
							.withValue(1, "framework", "Spring Framework."));
					assertThat(metadata.getItems()).hasSize(2);
					assertThat(metadata.getHints()).hasSize(1);
				});
		}

		@Test
		void simplePropertiesSourceWithAdditionalMetadataCanBeOverridden() {
			String additionalMetadata = """
					{
					   "properties": [
						 {
						   "name": "name",
						   "description": "Custom description."
						 }
					   ]
					 }""";
			compile(withTestClasses(SimplePropertiesSource.class)
				.andThen(withAdditionalMetadata(SimplePropertiesSource.class, additionalMetadata)), (compiled) -> {
					ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
							getSourceMetadataLocation(SimplePropertiesSource.class));
					assertThat(metadata).isNotNull()
						.has(Metadata.withProperty("name")
							.ofType(String.class)
							.withDefaultValue("boot")
							.withDescription("Custom description."))
						.has(Metadata.withProperty("enabled")
							.ofType(Boolean.class)
							.withDefaultValue(false)
							.withDescription("Whether it is enabled."));
					assertThat(metadata.getItems()).hasSize(2);
				});
		}

		@Test
		void lombokPropertiesSource() {
			compile(withTestClasses(LombokPropertiesSource.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(LombokPropertiesSource.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name")
						.ofType(String.class)
						.withDefaultValue("boot")
						.withDescription("Description of this simple property."))
					.has(Metadata.withProperty("enabled")
						.ofType(Boolean.class)
						.withDefaultValue(false)
						.withDescription("Whether it is enabled."));
				assertThat(metadata.getItems()).hasSize(2);
				assertThat(metadata.getHints()).isEmpty();
			});
		}

		@Test
		void immutablePropertiesSource() {
			compile(withTestClasses(ImmutablePropertiesSource.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(ImmutablePropertiesSource.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name")
						.ofType(String.class)
						.withDefaultValue("boot")
						.withDescription("Description of this simple property."))
					.has(Metadata.withProperty("enabled")
						.ofType(Boolean.class)
						.withDefaultValue(false)
						.withDescription("Whether it is enabled."));
				assertThat(metadata.getItems()).hasSize(2);
				assertThat(metadata.getHints()).isEmpty();
			});
		}

		@Test
		void recordPropertiesSource() {
			compile(withTestClasses(RecordPropertiesSources.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(RecordPropertiesSources.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name")
						.ofType(String.class)
						.withDefaultValue("boot")
						.withDescription("Description of this simple property."))
					.has(Metadata.withProperty("enabled")
						.ofType(Boolean.class)
						.withDefaultValue(false)
						.withDescription("Whether it is enabled."));
				assertThat(metadata.getItems()).hasSize(2);
				assertThat(metadata.getHints()).isEmpty();
			});
		}

		@Test
		void abstractPropertiesSource() {
			compile(withTestClasses(AbstractPropertiesSource.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(AbstractPropertiesSource.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name")
						.ofType(String.class)
						.withDefaultValue("boot")
						.withDescription("Description of this simple property."))
					.has(Metadata.withProperty("enabled")
						.ofType(Boolean.class)
						.withDefaultValue(false)
						.withDescription("Whether it is enabled."));
				assertThat(metadata.getItems()).hasSize(2);
				assertThat(metadata.getHints()).isEmpty();
			});
		}

		@Test
		void nonRootConfigurationPropertiesSources() {
			compile(withTestClasses(ConfigurationPropertySourcesContainer.class), (compiled) -> {
				assertThat(CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(ConfigurationPropertySourcesContainer.class)))
					.isNull();
				ConfigurationMetadata firstMetadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(First.class));
				assertThat(firstMetadata).isNotNull()
					.has(Metadata.withProperty("name").ofType(String.class).withDescription("A name."));
				assertThat(firstMetadata.getItems()).hasSize(1);
				assertThat(firstMetadata.getHints()).isEmpty();
				ConfigurationMetadata secondMetadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(Second.class));
				assertThat(secondMetadata).isNotNull()
					.has(Metadata.withProperty("visible")
						.ofType(Boolean.class)
						.withDefaultValue(true)
						.withDescription("Whether this is visible."));
				assertThat(secondMetadata.getItems()).hasSize(1);
				assertThat(secondMetadata.getHints()).isEmpty();
				assertThat(CompiledMetadataReader.getMetadata(compiled, getSourceMetadataLocation(Third.class)))
					.isNull();

			});
		}

		@Test
		void nestedPropertiesSource() {
			compile(withTestClasses(NestedPropertiesSource.class), (compiled) -> {
				ConfigurationMetadata metadata = CompiledMetadataReader.getMetadata(compiled,
						getSourceMetadataLocation(NestedPropertiesSource.class));
				assertThat(metadata).isNotNull()
					.has(Metadata.withProperty("name").ofType(String.class).withDescription("A name."))
					.has(Metadata.withGroup("nested").ofType(NestedPropertiesSource.Nested.class))
					.has(Metadata.withProperty("nested.name").ofType(String.class).withDescription("Another name."));
				assertThat(metadata.getItems()).hasSize(3);
			});
		}

		private String getSourceMetadataLocation(Class<?> type) {
			return "META-INF/spring/configuration-metadata/%s.json".formatted(type.getName());
		}

		private void compile(Function<TestCompiler, TestCompiler> configuration, Consumer<Compiled> compiled) {
			TestCompiler testCompiler = TestCompiler.forSystem();
			configuration.apply(testCompiler)
				.withProcessors(new TestConfigurationMetadataAnnotationProcessor())
				.compile(compiled);
		}

		private Function<TestCompiler, TestCompiler> withTestClasses(Class<?>... testClasses) {
			return (compiler) -> compiler
				.withSources(Arrays.stream(testClasses).map(SourceFile::forTestClass).toList());
		}

		private Function<TestCompiler, TestCompiler> withAdditionalMetadata(Class<?> type, String content) {
			String location = "META-INF/spring/configuration-metadata/additional/%s.json".formatted(type.getName());
			return (compiler) -> compiler.withResources(ResourceFile.of(location, content));
		}

	}

}
