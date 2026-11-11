/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ObjectContentAssert}.
 *
 * @author Phillip Webb
 */
class ObjectContentAssertTests {

	private static final ExampleObject SOURCE = new ExampleObject();

	private static final ExampleObject DIFFERENT;

	static {
		DIFFERENT = new ExampleObject();
		DIFFERENT.setAge(123);
	}

	@Test
	void isEqualToWhenObjectsAreEqualShouldPass() {
		assertThat(forObject(SOURCE)).isEqualTo(SOURCE);
	}

	@Test
	void isEqualToWhenObjectsAreDifferentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forObject(SOURCE)).isEqualTo(DIFFERENT));
	}

	@Test
	void asArrayForArrayShouldReturnObjectArrayAssert() {
		ExampleObject[] source = new ExampleObject[] { SOURCE };
		assertThat(forObject(source)).asArray().containsExactly(SOURCE);
	}

	@Test
	void asArrayForNonArrayShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asArray());
	}

	@Test
	void asMapForMapShouldReturnMapAssert() {
		Map<String, ExampleObject> source = Collections.singletonMap("a", SOURCE);
		assertThat(forObject(source)).asMap().containsEntry("a", SOURCE);
	}

	@Test
	void asMapForNonMapShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asMap());
	}

	private AssertProvider<ObjectContentAssert<Object>> forObject(Object source) {
		return () -> new ObjectContentAssert<>(source);
	}

}
