/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.ansi;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.ansi.AnsiOutput.Enabled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link AnsiOutput}.
 *
 * @author Phillip Webb
 * @author Philemon Hilscher
 */
class AnsiOutputTests {

	@BeforeAll
	static void enable() {
		AnsiOutput.setEnabled(Enabled.ALWAYS);
	}

	@AfterAll
	static void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void encoding() {
		String encoded = AnsiOutput.toString("A", AnsiColor.RED, AnsiStyle.BOLD, "B", AnsiStyle.NORMAL, "D",
				AnsiColor.GREEN, "E", AnsiStyle.FAINT, "F");
		assertThat(encoded).isEqualTo("A[31;1mB[0mD[32mE[2mF[0;39m");
	}

	private static Stream<Arguments> provideOsNames() {
		return Stream.of(
				Arguments.of("", false),
				Arguments.of("Windows 7", true),
				Arguments.of("Windows 8", true),
				Arguments.of("Windows 8.1", true),
				Arguments.of("Windows 10", true),
				Arguments.of("Windows 11", true),
				Arguments.of("Linux", false),
				Arguments.of("Mac OS X", false),
				Arguments.of("Mac OS", false)
		);
	}

	@ParameterizedTest
	@MethodSource("provideOsNames")
	void testDetectIfIsWindows(String osName, boolean expected) {
		boolean actual = AnsiOutput.isWindows(osName);
		assertEquals(expected, actual);
	}

	private static Stream<Arguments> provideOsVersionNumbers() {
		return Stream.of(
				Arguments.of("", false),
				Arguments.of("6.1", false), // Windows 7 / Server 2008 R2
				Arguments.of("6.2", false), // Windows 8 / Server 2012
				Arguments.of("6.3", false), // Windows 8.1 / Server 2012 R2
				Arguments.of("10.0", true) // Windows 10 / 11 / Server 2016+
		);
	}

	@ParameterizedTest
	@MethodSource("provideOsVersionNumbers")
	void testDetectIfIsWindowsAnsiCapable(String osVersion, boolean expected) {
		boolean actual = AnsiOutput.isWindowsAnsiCapable(osVersion);
		assertEquals(expected, actual);
	}

}
