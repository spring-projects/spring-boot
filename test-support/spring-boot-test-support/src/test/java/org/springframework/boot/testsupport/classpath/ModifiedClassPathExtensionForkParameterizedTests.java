/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForkedClassPath @ForkedClassPath}.
 *
 * @author Andy Wilkinson
 */
@ForkedClassPath
class ModifiedClassPathExtensionForkParameterizedTests {

	private static final List<String> arguments = new ArrayList<>();

	@ParameterizedTest
	@ValueSource(strings = { "one", "two", "three" })
	void testIsInvokedOnceForEachArgument(String argument) {
		if (argument.equals("one")) {
			assertThat(arguments).isEmpty();
		}
		else if (argument.equals("two")) {
			assertThat(arguments).doesNotContain("two", "three");
		}
		else if (argument.equals("three")) {
			assertThat(arguments).doesNotContain("three");
		}
		arguments.add(argument);
	}

}
