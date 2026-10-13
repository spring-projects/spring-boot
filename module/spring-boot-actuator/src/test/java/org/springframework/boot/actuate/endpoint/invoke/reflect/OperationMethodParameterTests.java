/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OperationMethodParameter}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class OperationMethodParameterTests {

	private final Method example = findMethod("example", String.class, String.class);

	private final Method exampleAnnotation = findMethod("exampleAnnotation", String.class);

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
	void getAnnotationShouldReturnAnnotation() {
		OperationMethodParameter parameter = new OperationMethodParameter("name",
				this.exampleAnnotation.getParameters()[0]);
		Selector annotation = parameter.getAnnotation(Selector.class);
		assertThat(annotation).isNotNull();
		assertThat(annotation.match()).isEqualTo(Match.ALL_REMAINING);
	}

	private Method findMethod(String name, Class<?>... parameters) {
		Method method = ReflectionUtils.findMethod(getClass(), name, parameters);
		assertThat(method).as("Method '%s'", name).isNotNull();
		return method;
	}

	void example(String one, @Nullable String two) {
	}

	void exampleAnnotation(@Selector(match = Match.ALL_REMAINING) String allRemaining) {
	}

}
