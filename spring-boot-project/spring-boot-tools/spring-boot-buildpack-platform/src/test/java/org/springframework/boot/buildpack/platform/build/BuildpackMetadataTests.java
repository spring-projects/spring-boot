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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuildpackMetadata}.
 *
 * @author Scott Frederick
 */
class BuildpackMetadataTests extends AbstractJsonTests {

	@Test
	void fromImageLoadsMetadata() throws IOException {
		Image image = Image.of(getContent("buildpack-image.json"));
		BuildpackMetadata metadata = BuildpackMetadata.fromImage(image);
		assertThat(metadata.getId()).isEqualTo("example/hello-universe");
		assertThat(metadata.getVersion()).isEqualTo("0.0.1");
	}

	@Test
	void fromImageWhenImageIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(null))
			.withMessage("Image must not be null");
	}

	@Test
	void fromImageWhenImageConfigIsNullThrowsException() {
		Image image = mock(Image.class);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(image))
			.withMessage("ImageConfig must not be null");
	}

	@Test
	void fromImageConfigWhenLabelIsMissingThrowsException() {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		given(imageConfig.getLabels()).willReturn(Collections.singletonMap("alpha", "a"));
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(image))
			.withMessage("No 'io.buildpacks.buildpackage.metadata' label found in image config labels 'alpha'");
	}

	@Test
	void fromJsonLoadsMetadata() throws IOException {
		BuildpackMetadata metadata = BuildpackMetadata.fromJson(getContentAsString("buildpack-metadata.json"));
		assertThat(metadata.getId()).isEqualTo("example/hello-universe");
		assertThat(metadata.getVersion()).isEqualTo("0.0.1");
		assertThat(metadata.getHomepage()).isEqualTo("https://github.com/example/tree/main/buildpacks/hello-universe");
	}

}
