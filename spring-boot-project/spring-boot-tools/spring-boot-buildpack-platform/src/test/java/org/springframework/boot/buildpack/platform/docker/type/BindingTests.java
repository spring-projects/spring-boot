/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Binding}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class BindingTests {

	@Test
	void ofReturnsValue() {
		Binding binding = Binding.of("host-src:container-dest:ro");
		assertThat(binding).hasToString("host-src:container-dest:ro");
	}

	@Test
	void ofWithNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.of(null))
			.withMessageContaining("'value' must not be null");
	}

	@Test
	void fromReturnsValue() {
		Binding binding = Binding.from("host-src", "container-dest");
		assertThat(binding).hasToString("host-src:container-dest");
	}

	@Test
	void fromWithNullSourceThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((String) null, "container-dest"))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	void fromWithNullDestinationThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from("host-src", null))
			.withMessageContaining("'destination' must not be null");
	}

	@Test
	void fromVolumeNameSourceReturnsValue() {
		Binding binding = Binding.from(VolumeName.of("host-src"), "container-dest");
		assertThat(binding).hasToString("host-src:container-dest");
	}

	@Test
	void fromVolumeNameSourceWithNullSourceThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((VolumeName) null, "container-dest"))
			.withMessageContaining("'sourceVolume' must not be null");
	}

	@Test
	void shouldReturnContainerDestinationPath() {
		Binding binding = Binding.from("/host", "/container");
		assertThat(binding.getContainerDestinationPath()).isEqualTo("/container");
	}

	@Test
	void shouldReturnContainerDestinationPathWithOptions() {
		Binding binding = Binding.of("/host:/container:ro");
		assertThat(binding.getContainerDestinationPath()).isEqualTo("/container");
	}

	@Test
	void shouldReturnContainerDestinationPathOnWindows() {
		Binding binding = Binding.from("C:\\host", "C:\\container");
		assertThat(binding.getContainerDestinationPath()).isEqualTo("C:\\container");
	}

	@Test
	void shouldReturnContainerDestinationPathOnWindowsWithOptions() {
		Binding binding = Binding.of("C:\\host:C:\\container:ro");
		assertThat(binding.getContainerDestinationPath()).isEqualTo("C:\\container");
	}

	@Test
	void shouldFailIfBindingIsMalformed() {
		Binding binding = Binding.of("some-invalid-binding");
		assertThatIllegalStateException().isThrownBy(binding::getContainerDestinationPath)
			.withMessage("Expected 2 or more parts, but found 1");
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			/cnb, true
			/layers, true
			/workspace, true
			/something, false
			c:\\cnb, true
			c:\\layers, true
			c:\\workspace, true
			c:\\something, false
			""")
	void shouldDetectSensitiveContainerPaths(String containerPath, boolean sensitive) {
		Binding binding = Binding.from("/host", containerPath);
		assertThat(binding.usesSensitiveContainerPath()).isEqualTo(sensitive);
	}

}
