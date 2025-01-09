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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ImagePlatformTests extends AbstractJsonTests {

	@Test
	void ofWithOsParses() {
		ImagePlatform platform = ImagePlatform.of("linux");
		assertThat(platform.toString()).isEqualTo("linux");
	}

	@Test
	void ofWithOsAndArchitectureParses() {
		ImagePlatform platform = ImagePlatform.of("linux/amd64");
		assertThat(platform.toString()).isEqualTo("linux/amd64");
	}

	@Test
	void ofWithOsAndArchitectureAndVariantParses() {
		ImagePlatform platform = ImagePlatform.of("linux/amd64/v1");
		assertThat(platform.toString()).isEqualTo("linux/amd64/v1");
	}

	@Test
	void ofWithEmptyValueFails() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImagePlatform.of(""))
			.withMessageContaining("'value' must not be empty");
	}

	@Test
	void ofWithTooManySegmentsFails() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImagePlatform.of("linux/amd64/v1/extra"))
			.withMessageContaining("'value' [linux/amd64/v1/extra] must be in the form");
	}

	@Test
	void fromImageMatchesImage() throws IOException {
		ImagePlatform platform = ImagePlatform.from(getImage());
		assertThat(platform.toString()).isEqualTo("linux/amd64/v1");
	}

	private Image getImage() throws IOException {
		return Image.of(getContent("image.json"));
	}

}
