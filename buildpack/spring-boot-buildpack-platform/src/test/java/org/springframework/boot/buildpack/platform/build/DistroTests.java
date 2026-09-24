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

package org.springframework.boot.buildpack.platform.build;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Distro}.
 *
 * @author Moritz Halbritter
 */
class DistroTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void shouldFailWhenImageIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> Distro.fromImage(null))
			.withMessage("'image' must not be null");
	}

	@Test
	void shouldReadBaseLabels() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "26.04")));
		assertThat(distro).hasToString("ubuntu 26.04");
	}

	@Test
	void shouldFormatNameOnly() {
		Distro distro = Distro.fromImage(image(Map.of("io.buildpacks.base.distro.name", "ubuntu")));
		assertThat(distro).hasToString("ubuntu");
	}

	@Test
	void shouldFormatVersionOnly() {
		Distro distro = Distro.fromImage(image(Map.of("io.buildpacks.base.distro.version", "26.04")));
		assertThat(distro).hasToString("26.04");
	}

	@Test
	void shouldFormatEmptyWhenLabelsAreMissing() {
		Distro distro = Distro.fromImage(image(Collections.emptyMap()));
		assertThat(distro).hasToString("");
	}

	@Test
	void shouldFallBackToStackLabels() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.stack.distro.name", "ubuntu", "io.buildpacks.stack.distro.version", "26.04")));
		assertThat(distro).hasToString("ubuntu 26.04");
	}

	@Test
	void shouldPreferBaseLabelsOverStackLabels() {
		Distro distro = Distro
			.fromImage(image(Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version",
					"26.04", "io.buildpacks.stack.distro.name", "debian", "io.buildpacks.stack.distro.version", "13")));
		assertThat(distro).hasToString("ubuntu 26.04");
	}

	@Test
	void shouldMatchWhenNameAndVersionAreEqual() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "26.04")));
		Distro other = Distro.fromImage(image(
				Map.of("io.buildpacks.stack.distro.name", "ubuntu", "io.buildpacks.stack.distro.version", "26.04")));
		assertThat(distro.matches(other)).isTrue();
	}

	@Test
	void shouldNotMatchWhenVersionDiffers() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "26.04")));
		Distro other = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "24.04")));
		assertThat(distro.matches(other)).isFalse();
	}

	@Test
	void shouldNotMatchWhenNameDiffers() {
		Distro distro = Distro.fromImage(image(Map.of("io.buildpacks.base.distro.name", "ubuntu")));
		Distro other = Distro.fromImage(image(Map.of("io.buildpacks.base.distro.name", "debian")));
		assertThat(distro.matches(other)).isFalse();
	}

	@Test
	void shouldMatchWhenVersionIsMissingOnOneSide() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "26.04")));
		Distro other = Distro.fromImage(image(Map.of("io.buildpacks.base.distro.name", "ubuntu")));
		assertThat(distro.matches(other)).isTrue();
		assertThat(other.matches(distro)).isTrue();
	}

	@Test
	void shouldMatchWhenLabelsAreMissing() {
		Distro distro = Distro.fromImage(image(
				Map.of("io.buildpacks.base.distro.name", "ubuntu", "io.buildpacks.base.distro.version", "26.04")));
		Distro other = Distro.fromImage(image(Collections.emptyMap()));
		assertThat(distro.matches(other)).isTrue();
		assertThat(other.matches(distro)).isTrue();
	}

	@Test
	void shouldIgnoreStackId() {
		Distro distro = Distro.fromImage(image(Map.of("io.buildpacks.stack.id", "io.buildpacks.stacks.resolute")));
		Distro other = Distro.fromImage(image(Map.of("io.buildpacks.stack.id", "io.buildpacks.stacks.resolute.tiny")));
		assertThat(distro.matches(other)).isTrue();
	}

	private Image image(Map<String, String> labels) {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		given(imageConfig.getLabels()).willReturn(labels);
		return image;
	}

}
