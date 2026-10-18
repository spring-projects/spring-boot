/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadableNames}.
 *
 * @author Phillip Webb
 */
class LoadableNamesTests {

	@Test
	void isValidClassNameWhenValid() {
		assertThatIsValidClassName("$").isTrue();
		assertThatIsValidClassName("MyClass").isTrue();
		assertThatIsValidClassName("com.example.MyClass").isTrue();
		assertThatIsValidClassName("com.example.MyClass$Nested").isTrue();
		assertThatIsValidClassName("com.example.MyClass$Nested$More").isTrue();
		assertThatIsValidClassName("com$$MyClass").isTrue();
		assertThatIsValidClassName("com/example/MyClass").isTrue();
		assertThatIsValidClassName("com/example/MyClass$3").isTrue();
		assertThatIsValidClassName("okio.-SegmentedByteString").isTrue();
		assertThatIsValidClassName("1Bad").isTrue();
		assertThatIsValidClassName("com.example.My Class").isTrue();
		assertThatIsValidClassName("com.example.my!class").isTrue();
		assertThatIsValidClassName("com.example.my:class").isTrue();
	}

	@Test
	void isValidClassNameWhenInvalid() {
		assertThatIsValidClassName(null).isFalse();
		assertThatIsValidClassName("").isFalse();
		assertThatIsValidClassName(".").isFalse();
		assertThatIsValidClassName(".MyClass").isFalse();
		assertThatIsValidClassName("MyClass.").isFalse();
		assertThatIsValidClassName("com..MyClass").isFalse();
		assertThatIsValidClassName("com//MyClass").isFalse();
		assertThatIsValidClassName("com./MyClass").isFalse();
		assertThatIsValidClassName("com;example").isFalse();
		assertThatIsValidClassName("com[example").isFalse();
	}

	@Test
	void isValidResourceNameWhenValid() {
		assertThatIsValidResourceName("resource.txt").isTrue();
		assertThatIsValidResourceName("/some/resource.txt").isTrue();
		assertThatIsValidResourceName("some/resource.txt").isTrue();
	}

	@Test
	void isValidResourceNameWhenInvalid() {
		assertThatIsValidResourceName("").isFalse();
		assertThatIsValidResourceName("test:test").isFalse();
	}

	private AbstractBooleanAssert<?> assertThatIsValidClassName(String n) {
		return assertThat(LoadableNames.isValidClassName(n));
	}

	private AbstractBooleanAssert<?> assertThatIsValidResourceName(String n) {
		return assertThat(LoadableNames.isValidResourceName(n));
	}

}
