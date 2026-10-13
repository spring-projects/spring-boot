/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RunArguments}.
 *
 * @author Stephane Nicoll
 */
class RunArgumentsTests {

	@Test
	void parseNull() {
		String[] args = parseArgs(null);
		assertThat(args).isNotNull();
		assertThat(args).isEmpty();
	}

	@Test
	@SuppressWarnings("NullAway") // Maven can't handle nullable arrays
	void parseNullArray() {
		String[] args = new RunArguments((String[]) null).asArray();
		assertThat(args).isNotNull();
		assertThat(args).isEmpty();
	}

	@Test
	void parseArrayContainingNullValue() {
		String[] args = new RunArguments(new String[] { "foo", null, "bar" }).asArray();
		assertThat(args).isNotNull();
		assertThat(args).containsOnly("foo", "bar");
	}

	@Test
	void parseArrayContainingEmptyValue() {
		String[] args = new RunArguments(new String[] { "foo", "", "bar" }).asArray();
		assertThat(args).isNotNull();
		assertThat(args).containsOnly("foo", "", "bar");
	}

	@Test
	void parseEmpty() {
		String[] args = parseArgs("   ");
		assertThat(args).isNotNull();
		assertThat(args).isEmpty();
	}

	@Test
	void parseDebugFlag() {
		String[] args = parseArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
		assertThat(args).hasSize(1);
		assertThat(args[0]).isEqualTo("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
	}

	@Test
	void parseWithExtraSpaces() {
		String[] args = parseArgs("     -Dfoo=bar        -Dfoo2=bar2  ");
		assertThat(args).hasSize(2);
		assertThat(args[0]).isEqualTo("-Dfoo=bar");
		assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
	}

	@Test
	void parseWithNewLinesAndTabs() {
		String[] args = parseArgs("     -Dfoo=bar \n\t\t -Dfoo2=bar2  ");
		assertThat(args).hasSize(2);
		assertThat(args[0]).isEqualTo("-Dfoo=bar");
		assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
	}

	@Test
	void quoteHandledProperly() {
		String[] args = parseArgs("-Dvalue=\"My Value\"    ");
		assertThat(args).hasSize(1);
		assertThat(args[0]).isEqualTo("-Dvalue=My Value");
	}

	private String[] parseArgs(@Nullable String args) {
		return new RunArguments(args).asArray();
	}

}
