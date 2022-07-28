/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.maven.sample.ClassWithMainMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandLineBuilder}.
 *
 * @author Stephane Nicoll
 */
class CommandLineBuilderTests {

	public static final String CLASS_NAME = ClassWithMainMethod.class.getName();

	@Test
	void buildWithNullJvmArgumentsIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments((String[]) null).build())
				.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithNullIntermediateJvmArgumentIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments("-verbose:class", null, "-verbose:gc")
				.build()).containsExactly("-verbose:class", "-verbose:gc", CLASS_NAME);
	}

	@Test
	void buildWithJvmArgument() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments("-verbose:class").build())
				.containsExactly("-verbose:class", CLASS_NAME);
	}

	@Test
	void buildWithNullSystemPropertyIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(null).build())
				.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithSystemProperty() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(Map.of("flag", "enabled")).build())
				.containsExactly("-Dflag=\"enabled\"", CLASS_NAME);
	}

	@Test
	void buildWithNullArgumentsIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments((String[]) null).build())
				.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithNullIntermediateArgumentIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments("--test", null, "--another").build())
				.containsExactly(CLASS_NAME, "--test", "--another");
	}

}
