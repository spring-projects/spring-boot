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

package org.springframework.boot.maven;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TestClasspath}.
 *
 * @author Henrique (henriquejsza)
 */
class TestClasspathTests {

	@ParameterizedTest
	@CsvSource({ "false, OFF", "off, OFF", "true, DEPENDENCIES", "dependencies, DEPENDENCIES", "all, ALL" })
	void mapsValueToTestClasspath(String value, TestClasspath expected) {
		assertThat(TestClasspath.of(value)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "OFF, false, false", "DEPENDENCIES, true, false", "ALL, true, true" })
	void exposesIncludedParts(TestClasspath testClasspath, boolean useTestDependencies, boolean useTestClasses) {
		assertThat(testClasspath.isUseTestDependencies()).isEqualTo(useTestDependencies);
		assertThat(testClasspath.isUseTestClasses()).isEqualTo(useTestClasses);
	}

	@ParameterizedTest
	@CsvSource({ "unknown", "yes" })
	void rejectsUnsupportedValue(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> TestClasspath.of(value))
			.withMessage("Unsupported test classpath '%s'. Valid values are OFF, DEPENDENCIES, ALL, true, and false",
					value);
	}

}
