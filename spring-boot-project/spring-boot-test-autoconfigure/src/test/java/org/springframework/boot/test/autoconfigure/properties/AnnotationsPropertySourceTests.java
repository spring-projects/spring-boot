/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.autoconfigure.properties.AnnotationsPropertySourceTests.DeeplyNestedAnnotations.Level1;
import org.springframework.boot.test.autoconfigure.properties.AnnotationsPropertySourceTests.DeeplyNestedAnnotations.Level2;
import org.springframework.boot.test.autoconfigure.properties.AnnotationsPropertySourceTests.EnclosingClass.PropertyMappedAnnotationOnEnclosingClass;
import org.springframework.boot.test.autoconfigure.properties.AnnotationsPropertySourceTests.NestedAnnotations.Entry;
import org.springframework.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AnnotationsPropertySource}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AnnotationsPropertySourceTests {

	@Test
	void createWhenSourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AnnotationsPropertySource(null))
				.withMessageContaining("Property source must not be null");
	}

	@Test
	void propertiesWhenHasNoAnnotationShouldBeEmpty() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(NoAnnotation.class);
		assertThat(source.getPropertyNames()).isEmpty();
		assertThat(source.getProperty("value")).isNull();
	}

	@Test
	void propertiesWhenHasTypeLevelAnnotationShouldUseAttributeName() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(TypeLevel.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("value")).isEqualTo("abc");
	}

	@Test
	void propertiesWhenHasTypeLevelWithPrefixShouldUsePrefixedName() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(TypeLevelWithPrefix.class);
		assertThat(source.getPropertyNames()).containsExactly("test.value");
		assertThat(source.getProperty("test.value")).isEqualTo("abc");
	}

	@Test
	void propertiesWhenHasAttributeLevelWithPrefixShouldUsePrefixedName() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(AttributeLevelWithPrefix.class);
		assertThat(source.getPropertyNames()).containsExactly("test");
		assertThat(source.getProperty("test")).isEqualTo("abc");
	}

	@Test
	void propertiesWhenHasTypeAndAttributeLevelWithPrefixShouldUsePrefixedName() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(TypeAndAttributeLevelWithPrefix.class);
		assertThat(source.getPropertyNames()).containsExactly("test.example");
		assertThat(source.getProperty("test.example")).isEqualTo("abc");
	}

	@Test
	void propertiesWhenNotMappedAtTypeLevelShouldIgnoreAttributes() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(NotMappedAtTypeLevel.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("ignore")).isNull();
	}

	@Test
	void propertiesWhenNotMappedAtAttributeLevelShouldIgnoreAttributes() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(NotMappedAtAttributeLevel.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("ignore")).isNull();
	}

	@Test
	void propertiesWhenContainsArraysShouldExpandNames() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(Arrays.class);
		assertThat(source.getPropertyNames()).contains("strings[0]", "strings[1]", "classes[0]", "classes[1]",
				"ints[0]", "ints[1]", "longs[0]", "longs[1]", "floats[0]", "floats[1]", "doubles[0]", "doubles[1]",
				"booleans[0]", "booleans[1]");
		assertThat(source.getProperty("strings[0]")).isEqualTo("a");
		assertThat(source.getProperty("strings[1]")).isEqualTo("b");
		assertThat(source.getProperty("classes[0]")).isEqualTo(Integer.class);
		assertThat(source.getProperty("classes[1]")).isEqualTo(Long.class);
		assertThat(source.getProperty("ints[0]")).isEqualTo(1);
		assertThat(source.getProperty("ints[1]")).isEqualTo(2);
		assertThat(source.getProperty("longs[0]")).isEqualTo(1L);
		assertThat(source.getProperty("longs[1]")).isEqualTo(2L);
		assertThat(source.getProperty("floats[0]")).isEqualTo(1.0f);
		assertThat(source.getProperty("floats[1]")).isEqualTo(2.0f);
		assertThat(source.getProperty("doubles[0]")).isEqualTo(1.0);
		assertThat(source.getProperty("doubles[1]")).isEqualTo(2.0);
		assertThat(source.getProperty("booleans[0]")).isEqualTo(false);
		assertThat(source.getProperty("booleans[1]")).isEqualTo(true);
	}

	@Test
	void propertiesWhenHasCamelCaseShouldConvertToKebabCase() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(CamelCaseToKebabCase.class);
		assertThat(source.getPropertyNames()).contains("camel-case-to-kebab-case");
	}

	@Test
	void propertiesFromMetaAnnotationsAreMapped() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(PropertiesFromSingleMetaAnnotation.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("value")).isEqualTo("foo");
	}

	@Test
	void propertiesFromMultipleMetaAnnotationsAreMappedUsingTheirOwnPropertyMapping() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(PropertiesFromMultipleMetaAnnotations.class);
		assertThat(source.getPropertyNames()).containsExactly("value", "test.value", "test.example");
		assertThat(source.getProperty("value")).isEqualTo("alpha");
		assertThat(source.getProperty("test.value")).isEqualTo("bravo");
		assertThat(source.getProperty("test.example")).isEqualTo("charlie");
	}

	@Test
	void propertyMappedAttributesCanBeAliased() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(PropertyMappedAttributeWithAnAlias.class);
		assertThat(source.getPropertyNames()).containsExactly("aliasing.value");
		assertThat(source.getProperty("aliasing.value")).isEqualTo("baz");
	}

	@Test
	void selfAnnotatingAnnotationDoesNotCauseStackOverflow() {
		new AnnotationsPropertySource(PropertyMappedWithSelfAnnotatingAnnotation.class);
	}

	@Test
	void typeLevelAnnotationOnSuperClass() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(PropertyMappedAnnotationOnSuperClass.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("value")).isEqualTo("abc");
	}

	@Test
	void typeLevelAnnotationOnEnclosingClass() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(
				PropertyMappedAnnotationOnEnclosingClass.class);
		assertThat(source.getPropertyNames()).containsExactly("value");
		assertThat(source.getProperty("value")).isEqualTo("abc");
	}

	@Test
	void aliasedPropertyMappedAttributeOnSuperClass() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(
				AliasedPropertyMappedAnnotationOnSuperClass.class);
		assertThat(source.getPropertyNames()).containsExactly("aliasing.value");
		assertThat(source.getProperty("aliasing.value")).isEqualTo("baz");
	}

	@Test
	void enumValueMapped() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(EnumValueMapped.class);
		assertThat(source.getProperty("testenum.value")).isEqualTo(EnumItem.TWO);
	}

	@Test
	void enumValueNotMapped() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(EnumValueNotMapped.class);
		assertThat(source.containsProperty("testenum.value")).isFalse();
	}

	@Test
	void nestedAnnotationsMapped() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(PropertyMappedWithNestedAnnotations.class);
		assertThat(source.getProperty("testnested")).isNull();
		assertThat(source.getProperty("testnested.entries[0]")).isNull();
		assertThat(source.getProperty("testnested.entries[0].value")).isEqualTo("one");
		assertThat(source.getProperty("testnested.entries[1]")).isNull();
		assertThat(source.getProperty("testnested.entries[1].value")).isEqualTo("two");
	}

	@Test
	void deeplyNestedAnnotationsMapped() {
		AnnotationsPropertySource source = new AnnotationsPropertySource(
				PropertyMappedWithDeeplyNestedAnnotations.class);
		assertThat(source.getProperty("testdeeplynested")).isNull();
		assertThat(source.getProperty("testdeeplynested.level1")).isNull();
		assertThat(source.getProperty("testdeeplynested.level1.level2")).isNull();
		assertThat(source.getProperty("testdeeplynested.level1.level2.value")).isEqualTo("level2");
	}

	static class NoAnnotation {

	}

	@TypeLevelAnnotation("abc")
	static class TypeLevel {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping
	@interface TypeLevelAnnotation {

		String value();

	}

	@TypeLevelWithPrefixAnnotation("abc")
	static class TypeLevelWithPrefix {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("test")
	@interface TypeLevelWithPrefixAnnotation {

		String value();

	}

	@AttributeLevelWithPrefixAnnotation("abc")
	static class AttributeLevelWithPrefix {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AttributeLevelWithPrefixAnnotation {

		@PropertyMapping("test")
		String value();

	}

	@TypeAndAttributeLevelWithPrefixAnnotation("abc")
	static class TypeAndAttributeLevelWithPrefix {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("test")
	@interface TypeAndAttributeLevelWithPrefixAnnotation {

		@PropertyMapping("example")
		String value();

	}

	@NotMappedAtTypeLevelAnnotation("abc")
	static class NotMappedAtTypeLevel {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping(skip = SkipPropertyMapping.YES)
	@interface NotMappedAtTypeLevelAnnotation {

		@PropertyMapping
		String value();

		String ignore() default "xyz";

	}

	@NotMappedAtAttributeLevelAnnotation("abc")
	static class NotMappedAtAttributeLevel {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping
	@interface NotMappedAtAttributeLevelAnnotation {

		String value();

		@PropertyMapping(skip = SkipPropertyMapping.YES)
		String ignore() default "xyz";

	}

	@ArraysAnnotation(strings = { "a", "b" }, classes = { Integer.class, Long.class }, ints = { 1, 2 },
			longs = { 1, 2 }, floats = { 1.0f, 2.0f }, doubles = { 1.0, 2.0 }, booleans = { false, true })
	static class Arrays {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping
	@interface ArraysAnnotation {

		String[] strings();

		Class<?>[] classes();

		int[] ints();

		long[] longs();

		float[] floats();

		double[] doubles();

		boolean[] booleans();

	}

	@CamelCaseToKebabCaseAnnotation
	static class CamelCaseToKebabCase {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping
	@interface CamelCaseToKebabCaseAnnotation {

		String camelCaseToKebabCase() default "abc";

	}

	@PropertiesFromSingleMetaAnnotationAnnotation
	static class PropertiesFromSingleMetaAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@TypeLevelAnnotation("foo")
	@interface PropertiesFromSingleMetaAnnotationAnnotation {

	}

	@PropertiesFromMultipleMetaAnnotationsAnnotation
	static class PropertiesFromMultipleMetaAnnotations {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@TypeLevelAnnotation("alpha")
	@TypeLevelWithPrefixAnnotation("bravo")
	@TypeAndAttributeLevelWithPrefixAnnotation("charlie")
	@interface PropertiesFromMultipleMetaAnnotationsAnnotation {

	}

	@AttributeWithAliasAnnotation("baz")
	static class PropertyMappedAttributeWithAnAlias {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AliasedAttributeAnnotation
	@interface AttributeWithAliasAnnotation {

		@AliasFor(annotation = AliasedAttributeAnnotation.class)
		String value() default "foo";

		String someOtherAttribute() default "shouldNotBeMapped";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("aliasing")
	@interface AliasedAttributeAnnotation {

		String value() default "bar";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@SelfAnnotating
	@interface SelfAnnotating {

	}

	@SelfAnnotating
	static class PropertyMappedWithSelfAnnotatingAnnotation {

	}

	static class PropertyMappedAnnotationOnSuperClass extends TypeLevel {

	}

	@TypeLevelAnnotation("abc")
	static class EnclosingClass {

		class PropertyMappedAnnotationOnEnclosingClass {

		}

	}

	static class AliasedPropertyMappedAnnotationOnSuperClass extends PropertyMappedAttributeWithAnAlias {

	}

	@EnumAnnotation(EnumItem.TWO)
	static class EnumValueMapped {

	}

	@EnumAnnotation(EnumItem.DEFAULT)
	static class EnumValueNotMapped {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("testenum")
	@interface EnumAnnotation {

		@PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
		EnumItem value() default EnumItem.DEFAULT;

	}

	enum EnumItem {

		DEFAULT,

		ONE,

		TWO

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("testnested")
	@interface NestedAnnotations {

		Entry[] entries();

		@Retention(RetentionPolicy.RUNTIME)
		@interface Entry {

			String value();

		}

	}

	@NestedAnnotations(entries = { @Entry("one"), @Entry("two") })
	static class PropertyMappedWithNestedAnnotations {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping("testdeeplynested")
	@interface DeeplyNestedAnnotations {

		Level1 level1();

		@Retention(RetentionPolicy.RUNTIME)
		@interface Level1 {

			Level2 level2();

		}

		@Retention(RetentionPolicy.RUNTIME)
		@interface Level2 {

			String value();

		}

	}

	@DeeplyNestedAnnotations(level1 = @Level1(level2 = @Level2("level2")))
	static class PropertyMappedWithDeeplyNestedAnnotations {

	}

	@TypeLevelAnnotation("outer")
	static class OuterWithTypeLevel {

		@Nested
		static class NestedClass {

		}

	}

}
