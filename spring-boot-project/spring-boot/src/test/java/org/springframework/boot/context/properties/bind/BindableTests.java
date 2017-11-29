/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Bindable}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class BindableTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofClassWhenTypeIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		Bindable.of((Class<?>) null);
	}

	@Test
	public void ofTypeWhenTypeIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		Bindable.of((ResolvableType) null);
	}

	@Test
	public void ofClassShouldSetType() throws Exception {
		assertThat(Bindable.of(String.class).getType())
				.isEqualTo(ResolvableType.forClass(String.class));
	}

	@Test
	public void ofTypeShouldSetType() throws Exception {
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(Bindable.of(type).getType()).isEqualTo(type);
	}

	@Test
	public void ofInstanceShouldSetTypeAndExistingValue() throws Exception {
		String instance = "foo";
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(Bindable.ofInstance(instance).getType()).isEqualTo(type);
		assertThat(Bindable.ofInstance(instance).getValue().get()).isEqualTo("foo");
	}

	@Test
	public void ofClassWithExistingValueShouldSetTypeAndExistingValue() throws Exception {
		assertThat(Bindable.of(String.class).withExistingValue("foo").getValue().get())
				.isEqualTo("foo");
	}

	@Test
	public void ofTypeWithExistingValueShouldSetTypeAndExistingValue() throws Exception {
		assertThat(Bindable.of(ResolvableType.forClass(String.class))
				.withExistingValue("foo").getValue().get()).isEqualTo("foo");
	}

	@Test
	public void ofTypeWhenExistingValueIsNotInstanceOfTypeShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(
				"ExistingValue must be an instance of " + String.class.getName());
		Bindable.of(ResolvableType.forClass(String.class)).withExistingValue(123);
	}

	@Test
	public void ofTypeWhenPrimitiveWithExistingValueWrapperShouldNotThrowException()
			throws Exception {
		Bindable<Integer> bindable = Bindable
				.<Integer>of(ResolvableType.forClass(int.class)).withExistingValue(123);
		assertThat(bindable.getType().resolve()).isEqualTo(int.class);
		assertThat(bindable.getValue().get()).isEqualTo(123);
	}

	@Test
	public void getBoxedTypeWhenNotBoxedShouldReturnType() throws Exception {
		Bindable<String> bindable = Bindable.of(String.class);
		assertThat(bindable.getBoxedType())
				.isEqualTo(ResolvableType.forClass(String.class));
	}

	@Test
	public void getBoxedTypeWhenPrimitiveShouldReturnBoxedType() throws Exception {
		Bindable<Integer> bindable = Bindable.of(int.class);
		assertThat(bindable.getType()).isEqualTo(ResolvableType.forClass(int.class));
		assertThat(bindable.getBoxedType())
				.isEqualTo(ResolvableType.forClass(Integer.class));
	}

	@Test
	public void getBoxedTypeWhenPrimitiveArrayShouldReturnBoxedType() throws Exception {
		Bindable<int[]> bindable = Bindable.of(int[].class);
		assertThat(bindable.getType().getComponentType())
				.isEqualTo(ResolvableType.forClass(int.class));
		assertThat(bindable.getBoxedType().isArray()).isTrue();
		assertThat(bindable.getBoxedType().getComponentType())
				.isEqualTo(ResolvableType.forClass(Integer.class));
	}

	@Test
	public void getAnnotationsShouldReturnEmptyArray() throws Exception {
		assertThat(Bindable.of(String.class).getAnnotations()).isEmpty();
	}

	@Test
	public void withAnnotationsShouldSetAnnotations() throws Exception {
		Annotation annotation = mock(Annotation.class);
		assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotations())
				.containsExactly(annotation);
	}

	@Test
	public void toStringShouldShowDetails() throws Exception {
		Annotation annotation = AnnotationUtils
				.synthesizeAnnotation(TestAnnotation.class);
		Bindable<String> bindable = Bindable.of(String.class).withExistingValue("foo")
				.withAnnotations(annotation);
		System.out.println(bindable.toString());
		assertThat(bindable.toString()).contains("type = java.lang.String, "
				+ "value = 'provided', annotations = array<Annotation>["
				+ "@org.springframework.boot.context.properties.bind."
				+ "BindableTests$TestAnnotation()]");
	}

	@Test
	public void equalsAndHashCode() throws Exception {
		Annotation annotation = AnnotationUtils
				.synthesizeAnnotation(TestAnnotation.class);
		Bindable<String> bindable1 = Bindable.of(String.class).withExistingValue("foo")
				.withAnnotations(annotation);
		Bindable<String> bindable2 = Bindable.of(String.class).withExistingValue("foo")
				.withAnnotations(annotation);
		Bindable<String> bindable3 = Bindable.of(String.class).withExistingValue("fof")
				.withAnnotations(annotation);
		assertThat(bindable1.hashCode()).isEqualTo(bindable2.hashCode());
		assertThat(bindable1).isEqualTo(bindable1).isEqualTo(bindable2);
		assertThat(bindable1).isEqualTo(bindable3);
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestAnnotation {

	}

	static class TestNewInstance {

		private String foo = "hello world";

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	static class TestNewInstanceWithNoDefaultConstructor {

		TestNewInstanceWithNoDefaultConstructor(String foo) {
			this.foo = foo;
		}

		private String foo = "hello world";

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

}
