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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OperationMethodParameter}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class OperationMethodParameterTests {

	private final Method example = ReflectionUtils.findMethod(getClass(), "example", String.class, String.class);

	private final Method exampleSpringNullable = ReflectionUtils.findMethod(getClass(), "exampleSpringNullable",
			String.class, String.class);

	private final Method exampleJsr305 = ReflectionUtils.findMethod(getClass(), "exampleJsr305", String.class,
			String.class);

	private final Method exampleMetaJsr305 = ReflectionUtils.findMethod(getClass(), "exampleMetaJsr305", String.class,
			String.class);

	private final Method exampleJsr305NonNull = ReflectionUtils.findMethod(getClass(), "exampleJsr305NonNull",
			String.class, String.class);

	private Method exampleAnnotation = ReflectionUtils.findMethod(getClass(), "exampleAnnotation", String.class);

	private final Method exampleJSpecifyNullable = ReflectionUtils.findMethod(getClass(), "exampleJSpecifyNullable",
			String.class, String.class);

	@Test
	void getNameShouldReturnName() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0],
				this::isOptionalParameter);
		assertThat(parameter.getName()).isEqualTo("name");
	}

	@Test
	void getTypeShouldReturnType() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0],
				this::isOptionalParameter);
		assertThat(parameter.getType()).isEqualTo(String.class);
	}

	@Test
	void isMandatoryWhenNoAnnotationShouldReturnTrue() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0],
				this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isTrue();
	}

	@Test
	void isMandatoryWhenOptionalAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[1],
				this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenSpringNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleSpringNullable.getParameters()[1], this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.exampleJsr305.getParameters()[1],
				this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrMetaNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleMetaJsr305.getParameters()[1], this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrNonnullAnnotationShouldReturnTrue() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleJsr305NonNull.getParameters()[1], this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isTrue();
	}

	@Test
	void getAnnotationShouldReturnAnnotation() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleAnnotation.getParameters()[0], this::isOptionalParameter);
		Selector annotation = parameter.getAnnotation(Selector.class);
		assertThat(annotation).isNotNull();
		assertThat(annotation.match()).isEqualTo(Match.ALL_REMAINING);
	}

	@Test
	void isMandatoryWhenJSpecifyNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleJSpecifyNullable.getParameters()[1], this::isOptionalParameter);
		assertThat(parameter.isMandatory()).isFalse();
	}

	private boolean isOptionalParameter(Parameter parameter) {
		return MergedAnnotations.from(parameter).isPresent(TestOptional.class);
	}

	void example(String one, @TestOptional String two) {
	}

	@SuppressWarnings("deprecation")
	void exampleSpringNullable(String one, @org.springframework.lang.Nullable String two) {
	}

	void exampleJsr305(String one, @javax.annotation.Nullable String two) {
	}

	void exampleMetaJsr305(String one, @MetaNullable String two) {
	}

	void exampleJsr305NonNull(String one, @javax.annotation.Nonnull String two) {
	}

	void exampleAnnotation(@Selector(match = Match.ALL_REMAINING) String allRemaining) {
	}

	void exampleJSpecifyNullable(String one, @org.jspecify.annotations.Nullable String two) {
	}

	@TypeQualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Nonnull(when = When.MAYBE)
	@interface MetaNullable {

	}

	@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TestOptional {

	}

}
