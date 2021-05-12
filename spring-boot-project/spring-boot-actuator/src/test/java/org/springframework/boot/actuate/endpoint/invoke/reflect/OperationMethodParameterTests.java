/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OperationMethodParameter}.
 *
 * @author Phillip Webb
 */
class OperationMethodParameterTests {

	private Method example = ReflectionUtils.findMethod(getClass(), "example", String.class, String.class);

	private Method exampleJsr305 = ReflectionUtils.findMethod(getClass(), "exampleJsr305", String.class, String.class);

	private Method exampleMetaJsr305 = ReflectionUtils.findMethod(getClass(), "exampleMetaJsr305", String.class,
			String.class);

	private Method exampleJsr305NonNull = ReflectionUtils.findMethod(getClass(), "exampleJsr305NonNull", String.class,
			String.class);

	@Test
	void getNameShouldReturnName() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0]);
		assertThat(parameter.getName()).isEqualTo("name");
	}

	@Test
	void getTypeShouldReturnType() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0]);
		assertThat(parameter.getType()).isEqualTo(String.class);
	}

	@Test
	void isMandatoryWhenNoAnnotationShouldReturnTrue() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[0]);
		assertThat(parameter.isMandatory()).isTrue();
	}

	@Test
	void isMandatoryWhenNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name", this.example.getParameters()[1]);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleJsr305.getParameters()[1]);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrMetaNullableAnnotationShouldReturnFalse() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleMetaJsr305.getParameters()[1]);
		assertThat(parameter.isMandatory()).isFalse();
	}

	@Test
	void isMandatoryWhenJsrNonnullAnnotationShouldReturnTrue() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleJsr305NonNull.getParameters()[1]);
		assertThat(parameter.isMandatory()).isTrue();
	}

	void example(String one, @Nullable String two) {
	}

	void exampleJsr305(String one, @javax.annotation.Nullable String two) {
	}

	void exampleMetaJsr305(String one, @MetaNullable String two) {
	}

	void exampleJsr305NonNull(String one, @javax.annotation.Nonnull String two) {
	}

	@TypeQualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Nonnull(when = When.MAYBE)
	@interface MetaNullable {

	}

}
