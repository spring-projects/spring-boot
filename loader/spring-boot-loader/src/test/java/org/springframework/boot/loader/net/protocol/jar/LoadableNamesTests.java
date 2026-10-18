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
	}

	@Test
	void isValidClassNameWhenPackageInfo() {
		assertThatIsValidClassName("package-info").isTrue();
		assertThatIsValidClassName("com.example.package-info").isTrue();
		assertThatIsValidClassName("com/example/package-info").isTrue();
		assertThatIsValidClassName("com.example.my-info").isFalse();
		assertThatIsValidClassName("com.1Bad.package-info").isFalse();
		assertThatIsValidClassName(".package-info").isFalse();
		assertThatIsValidClassName("com..package-info").isFalse();
		assertThatIsValidClassName("com//package-info").isFalse();
		assertThatIsValidClassName("package-info.package-info").isFalse();
	}

	@Test
	void isValidClassNameWhenInvalid() {
		assertThatIsValidClassName("").isFalse();
		assertThatIsValidClassName(".").isFalse();
		assertThatIsValidClassName("1Bad").isFalse();
		assertThatIsValidClassName("com.1Bad").isFalse();
		assertThatIsValidClassName("com..MyClass").isFalse();
		assertThatIsValidClassName("My Class").isFalse();
		assertThatIsValidClassName("my!class").isFalse();
		assertThatIsValidClassName("my:class").isFalse();
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
