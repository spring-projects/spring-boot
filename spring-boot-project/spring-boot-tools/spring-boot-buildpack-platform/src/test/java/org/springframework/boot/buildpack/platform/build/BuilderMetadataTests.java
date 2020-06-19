/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuilderMetadata}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class BuilderMetadataTests extends AbstractJsonTests {

	@Test
	void fromImageLoadsMetadata() throws IOException {
		Image image = Image.of(getContent("image.json"));
		BuilderMetadata metadata = BuilderMetadata.fromImage(image);
		assertThat(metadata.getStack().getRunImage().getImage()).isEqualTo("cloudfoundry/run:base-cnb");
		assertThat(metadata.getStack().getRunImage().getMirrors()).isEmpty();
		assertThat(metadata.getLifecycle().getVersion()).isEqualTo("0.7.2");
		assertThat(metadata.getLifecycle().getApi().getBuildpack()).isEqualTo("0.2");
		assertThat(metadata.getLifecycle().getApi().getPlatform()).isEqualTo("0.3");
		assertThat(metadata.getCreatedBy().getName()).isEqualTo("Pack CLI");
		assertThat(metadata.getCreatedBy().getVersion())
				.isEqualTo("v0.9.0 (git sha: d42c384a39f367588f2653f2a99702db910e5ad7)");
	}

	@Test
	void fromImageWhenImageIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuilderMetadata.fromImage(null))
				.withMessage("Image must not be null");
	}

	@Test
	void fromImageWhenImageConfigIsNullThrowsException() {
		Image image = mock(Image.class);
		assertThatIllegalArgumentException().isThrownBy(() -> BuilderMetadata.fromImage(image))
				.withMessage("ImageConfig must not be null");
	}

	@Test
	void fromImageConfigWhenLabelIsMissingThrowsException() {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		assertThatIllegalArgumentException().isThrownBy(() -> BuilderMetadata.fromImage(image))
				.withMessage("No 'io.buildpacks.builder.metadata' label found in image config");
	}

	@Test
	void copyWithUpdatedCreatedByReturnsNewMetadata() throws IOException {
		Image image = Image.of(getContent("image.json"));
		BuilderMetadata metadata = BuilderMetadata.fromImage(image);
		BuilderMetadata copy = metadata.copy((update) -> update.withCreatedBy("test123", "test456"));
		assertThat(copy).isNotSameAs(metadata);
		assertThat(copy.getCreatedBy().getName()).isEqualTo("test123");
		assertThat(copy.getCreatedBy().getVersion()).isEqualTo("test456");
	}

	@Test
	void attachToUpdatesMetadata() throws IOException {
		Image image = Image.of(getContent("image.json"));
		ImageConfig imageConfig = image.getConfig();
		BuilderMetadata metadata = BuilderMetadata.fromImage(image);
		ImageConfig imageConfigCopy = imageConfig.copy(metadata::attachTo);
		String label = imageConfigCopy.getLabels().get("io.buildpacks.builder.metadata");
		BuilderMetadata metadataCopy = BuilderMetadata.fromJson(label);
		assertThat(metadataCopy.getStack().getRunImage().getImage())
				.isEqualTo(metadata.getStack().getRunImage().getImage());
	}

}
