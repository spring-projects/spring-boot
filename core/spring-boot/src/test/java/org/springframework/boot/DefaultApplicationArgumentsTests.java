/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultApplicationArguments}.
 *
 * @author Phillip Webb
 */
class DefaultApplicationArgumentsTests {

	private static final String[] ARGS = new String[] { "--foo=bar", "--foo=baz", "--debug", "spring", "boot" };

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void argumentsMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultApplicationArguments((String[]) null))
			.withMessageContaining("'args' must not be null");
	}

	@Test
	void getArgs() {
		ApplicationArguments arguments = new DefaultApplicationArguments(ARGS);
		assertThat(arguments.getSourceArgs()).isEqualTo(ARGS);
	}

	@Test
	void optionNames() {
		ApplicationArguments arguments = new DefaultApplicationArguments(ARGS);
		Set<String> expected = new HashSet<>(Arrays.asList("foo", "debug"));
		assertThat(arguments.getOptionNames()).isEqualTo(expected);
	}

	@Test
	void containsOption() {
		ApplicationArguments arguments = new DefaultApplicationArguments(ARGS);
		assertThat(arguments.containsOption("foo")).isTrue();
		assertThat(arguments.containsOption("debug")).isTrue();
		assertThat(arguments.containsOption("spring")).isFalse();
	}

	@Test
	void getOptionValues() {
		ApplicationArguments arguments = new DefaultApplicationArguments(ARGS);
		assertThat(arguments.getOptionValues("foo")).isEqualTo(Arrays.asList("bar", "baz"));
		assertThat(arguments.getOptionValues("debug")).isEmpty();
		assertThat(arguments.getOptionValues("spring")).isNull();
	}

	@Test
	void getNonOptionArgs() {
		ApplicationArguments arguments = new DefaultApplicationArguments(ARGS);
		assertThat(arguments.getNonOptionArgs()).containsExactly("spring", "boot");
	}

	@Test
	void getNoNonOptionArgs() {
		ApplicationArguments arguments = new DefaultApplicationArguments("--debug");
		assertThat(arguments.getNonOptionArgs()).isEmpty();
	}

}
