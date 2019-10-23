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

package org.springframework.boot.maven;

import org.assertj.core.api.ObjectArrayAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationArguments}.
 *
 * @author Dmytro Nosan
 */
class ApplicationArgumentsTest {

	@Test
	void parseMavenSplitArgs() {
		assertArgs("'--management.include=prometheus", "health", "info'", "'--spring.profiles.active=foo", "bar'")
				.containsExactly("--management.include=prometheus,health,info", "--spring.profiles.active=foo,bar");
	}

	@Test
	void parseArgs() {
		assertThat(assertArgs("'--management.include=prometheus,info'", "'--spring.profiles.active=foo,bar'")
				.containsExactly("--management.include=prometheus,info", "--spring.profiles.active=foo,bar"));
	}

	@Test
	void parseArgumentsAsLine() {
		assertArgs("'--management.include=prometheus,info,--spring.profiles.active=foo,bar'")
				.containsExactly("--management.include=prometheus,info", "--spring.profiles.active=foo,bar");
	}

	@Test
	void parseOneArg() {
		assertArgs("'--management.include=prometheus,info'").containsExactly("--management.include=prometheus,info");
	}

	@Test
	void parseNull() {
		assertArgs((String[]) null).isEmpty();
	}

	@Test
	void parseEmpty() {
		assertArgs().isEmpty();
	}

	private static ObjectArrayAssert<String> assertArgs(String... args) {
		return assertThat(new ApplicationArguments(args).asArray());
	}

}
