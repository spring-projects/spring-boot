/*
 * Copyright 2012-2019 the original author or authors.
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
	void argumentsMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultApplicationArguments((String[]) null))
				.withMessageContaining("Args must not be null");
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
