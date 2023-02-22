/*
 * Copyright 2012-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Binding}.
 *
 * @author Scott Frederick
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
			.withMessageContaining("Value must not be null");
	}

	@Test
	void fromReturnsValue() {
		Binding binding = Binding.from("host-src", "container-dest");
		assertThat(binding).hasToString("host-src:container-dest");
	}

	@Test
	void fromWithNullSourceThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((String) null, "container-dest"))
			.withMessageContaining("Source must not be null");
	}

	@Test
	void fromWithNullDestinationThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from("host-src", null))
			.withMessageContaining("Destination must not be null");
	}

	@Test
	void fromVolumeNameSourceReturnsValue() {
		Binding binding = Binding.from(VolumeName.of("host-src"), "container-dest");
		assertThat(binding).hasToString("host-src:container-dest");
	}

	@Test
	void fromVolumeNameSourceWithNullSourceThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((VolumeName) null, "container-dest"))
			.withMessageContaining("SourceVolume must not be null");
	}

}
