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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Bindable}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class BindableTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofClassWhenTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Bindable.of((Class<?>) null))
			.withMessageContaining("'type' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofTypeWhenTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Bindable.of((ResolvableType) null))
			.withMessageContaining("'type' must not be null");
	}

	@Test
	void ofClassShouldSetType() {
		assertThat(Bindable.of(String.class).getType()).isEqualTo(ResolvableType.forClass(String.class));
	}

	@Test
	void ofTypeShouldSetType() {
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(Bindable.of(type).getType()).isEqualTo(type);
	}

	@Test
	void ofInstanceShouldSetTypeAndExistingValue() {
		String instance = "foo";
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(Bindable.ofInstance(instance).getType()).isEqualTo(type);
		Supplier<String> value = Bindable.ofInstance(instance).getValue();
		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo("foo");
	}

	@Test
	void ofClassWithExistingValueShouldSetTypeAndExistingValue() {
		Supplier<String> value = Bindable.of(String.class).withExistingValue("foo").getValue();
		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo("foo");
	}

	@Test
	void ofTypeWithExistingValueShouldSetTypeAndExistingValue() {
		Supplier<Object> value = Bindable.of(ResolvableType.forClass(String.class)).withExistingValue("foo").getValue();
		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo("foo");
	}

	@Test
	void ofTypeWhenExistingValueIsNotInstanceOfTypeShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Bindable.of(ResolvableType.forClass(String.class)).withExistingValue(123))
			.withMessageContaining("'existingValue' must be an instance of " + String.class.getName());
	}

	@Test
	void ofTypeWhenPrimitiveWithExistingValueWrapperShouldNotThrowException() {
		Bindable<Integer> bindable = Bindable.<Integer>of(ResolvableType.forClass(int.class)).withExistingValue(123);
		assertThat(bindable.getType().resolve()).isEqualTo(int.class);
		Supplier<Integer> value = bindable.getValue();
		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo(123);
	}

	@Test
	void getBoxedTypeWhenNotBoxedShouldReturnType() {
		Bindable<String> bindable = Bindable.of(String.class);
		assertThat(bindable.getBoxedType()).isEqualTo(ResolvableType.forClass(String.class));
	}

	@Test
	void getBoxedTypeWhenPrimitiveShouldReturnBoxedType() {
		Bindable<Integer> bindable = Bindable.of(int.class);
		assertThat(bindable.getType()).isEqualTo(ResolvableType.forClass(int.class));
		assertThat(bindable.getBoxedType()).isEqualTo(ResolvableType.forClass(Integer.class));
	}

	@Test
	void getBoxedTypeWhenPrimitiveArrayShouldReturnBoxedType() {
		Bindable<int[]> bindable = Bindable.of(int[].class);
		assertThat(bindable.getType().getComponentType()).isEqualTo(ResolvableType.forClass(int.class));
		assertThat(bindable.getBoxedType().isArray()).isTrue();
		assertThat(bindable.getBoxedType().getComponentType()).isEqualTo(ResolvableType.forClass(Integer.class));
	}

	@Test
	void getAnnotationsShouldReturnEmptyArray() {
		assertThat(Bindable.of(String.class).getAnnotations()).isEmpty();
	}

	@Test
	void withAnnotationsShouldSetAnnotations() {
		Annotation annotation = mock(Annotation.class);
		assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotations()).containsExactly(annotation);
	}

	@Test
	void getAnnotationWhenMatchShouldReturnAnnotation() {
		Test annotation = AnnotationUtils.synthesizeAnnotation(Test.class);
		assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotation(Test.class))
			.isSameAs(annotation);
	}

	@Test
	void getAnnotationWhenNoMatchShouldReturnNull() {
		Test annotation = AnnotationUtils.synthesizeAnnotation(Test.class);
		assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotation(Bean.class)).isNull();
	}

	@Test
	void toStringShouldShowDetails() {
		Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
		Bindable<String> bindable = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
		assertThat(bindable.toString())
			.contains("type = java.lang.String, value = 'provided', annotations = array<Annotation>["
					+ "@org.springframework.boot.context.properties.bind.BindableTests.TestAnnotation()]");
	}

	@Test
	void equalsAndHashCode() {
		Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
		Bindable<String> bindable1 = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
		Bindable<String> bindable2 = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
		Bindable<String> bindable3 = Bindable.of(String.class).withExistingValue("fof").withAnnotations(annotation);
		assertThat(bindable1).hasSameHashCodeAs(bindable2);
		assertThat(bindable1).isEqualTo(bindable1).isEqualTo(bindable2);
		assertThat(bindable1).isEqualTo(bindable3);
	}

	@Test // gh-18218
	void withExistingValueDoesNotForgetAnnotations() {
		Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
		Bindable<?> bindable = Bindable.of(String.class).withAnnotations(annotation).withExistingValue("");
		assertThat(bindable.getAnnotations()).containsExactly(annotation);
	}

	@Test // gh-18218
	void withSuppliedValueDoesNotForgetAnnotations() {
		Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
		Bindable<?> bindable = Bindable.of(String.class).withAnnotations(annotation).withSuppliedValue(() -> "");
		assertThat(bindable.getAnnotations()).containsExactly(annotation);
	}

	@Test
	void hasBindRestrictionWhenDefaultReturnsFalse() {
		Bindable<String> bindable = Bindable.of(String.class);
		for (BindRestriction bindRestriction : BindRestriction.values()) {
			assertThat(bindable.hasBindRestriction(bindRestriction)).isFalse();
		}
	}

	@Test
	void withBindRestrictionAddsBindRestriction() {
		Bindable<String> bindable = Bindable.of(String.class);
		Bindable<String> restricted = bindable.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY);
		assertThat(bindable.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)).isFalse();
		assertThat(restricted.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)).isTrue();
	}

	@Test
	void whenTypeCouldUseJavaBeanOrValueObjectJavaBeanBindingCanBeSpecified() {
		BindMethod bindMethod = Bindable.of(JavaBeanOrValueObject.class)
			.withBindMethod(BindMethod.JAVA_BEAN)
			.getBindMethod();
		assertThat(bindMethod).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void whenTypeCouldUseJavaBeanOrValueObjectExistingValueForcesJavaBeanBinding() {
		BindMethod bindMethod = Bindable.of(JavaBeanOrValueObject.class)
			.withExistingValue(new JavaBeanOrValueObject("value"))
			.getBindMethod();
		assertThat(bindMethod).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void whenBindingIsValueObjectExistingValueThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> Bindable.of(JavaBeanOrValueObject.class)
			.withBindMethod(BindMethod.VALUE_OBJECT)
			.withExistingValue(new JavaBeanOrValueObject("value")));
	}

	@Test
	void whenBindableHasExistingValueValueObjectBindMethodThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> Bindable.of(JavaBeanOrValueObject.class)
			.withExistingValue(new JavaBeanOrValueObject("value"))
			.withBindMethod(BindMethod.VALUE_OBJECT));
	}

	@Test
	void whenBindableHasSuppliedValueValueObjectBindMethodThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> Bindable.of(JavaBeanOrValueObject.class)
			.withSuppliedValue(() -> new JavaBeanOrValueObject("value"))
			.withBindMethod(BindMethod.VALUE_OBJECT));
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation {

	}

	static class JavaBeanOrValueObject {

		private String property;

		JavaBeanOrValueObject(String property) {
			this.property = property;
		}

		String getProperty() {
			return this.property;
		}

		void setProperty(String property) {
			this.property = property;
		}

	}

}
