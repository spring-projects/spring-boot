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
 * Tests for {@link BuildpackLayersMetadata}.
 *
 * @author Scott Frederick
 */
class BuildpackLayersMetadataTests extends AbstractJsonTests {

	@Test
	void fromImageLoadsMetadata() throws IOException {
		Image image = Image.of(getContent("buildpack-image.json"));
		BuildpackLayersMetadata metadata = BuildpackLayersMetadata.fromImage(image);
		assertThat(metadata.getBuildpack("example/hello-moon", "0.0.3")).extracting("homepage", "layerDiffId")
				.containsExactly("https://github.com/example/tree/main/buildpacks/hello-moon",
						"sha256:4bfdc8714aee68da6662c43bc28d3b41202c88e915641c356523dabe729814c2");
		assertThat(metadata.getBuildpack("example/hello-world", "0.0.2")).extracting("homepage", "layerDiffId")
				.containsExactly("https://github.com/example/tree/main/buildpacks/hello-world",
						"sha256:f752fe099c846e501bdc991d1a22f98c055ddc62f01cfc0495fff2c69f8eb940");
		assertThat(metadata.getBuildpack("example/hello-world", "version-does-not-exist")).isNull();
		assertThat(metadata.getBuildpack("id-does-not-exist", "9.9.9")).isNull();
	}

	@Test
	void fromImageWhenImageIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(null))
				.withMessage("Image must not be null");
	}

	@Test
	void fromImageWhenImageConfigIsNullThrowsException() {
		Image image = mock(Image.class);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(image))
				.withMessage("ImageConfig must not be null");
	}

	@Test
	void fromImageConfigWhenLabelIsMissingThrowsException() {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		given(imageConfig.getLabels()).willReturn(Collections.singletonMap("alpha", "a"));
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(image))
				.withMessage("No 'io.buildpacks.buildpack.layers' label found in image config labels 'alpha'");
	}

	@Test
	void fromJsonLoadsMetadata() throws IOException {
		BuildpackLayersMetadata metadata = BuildpackLayersMetadata
				.fromJson(getContentAsString("buildpack-layers-metadata.json"));
		assertThat(metadata.getBuildpack("example/hello-moon", "0.0.3")).extracting("name", "homepage", "layerDiffId")
				.containsExactly("Example hello-moon buildpack",
						"https://github.com/example/tree/main/buildpacks/hello-moon",
						"sha256:4bfdc8714aee68da6662c43bc28d3b41202c88e915641c356523dabe729814c2");
		assertThat(metadata.getBuildpack("example/hello-world", "0.0.1")).extracting("name", "homepage", "layerDiffId")
				.containsExactly("Example hello-world buildpack",
						"https://github.com/example/tree/main/buildpacks/hello-world",
						"sha256:1c90e0b80d92555a0523c9ee6500845328fc39ba9dca9d30a877ff759ffbff28");
		assertThat(metadata.getBuildpack("example/hello-world", "0.0.2")).extracting("name", "homepage", "layerDiffId")
				.containsExactly("Example hello-world buildpack",
						"https://github.com/example/tree/main/buildpacks/hello-world",
						"sha256:f752fe099c846e501bdc991d1a22f98c055ddc62f01cfc0495fff2c69f8eb940");
		assertThat(metadata.getBuildpack("example/hello-world", "version-does-not-exist")).isNull();
		assertThat(metadata.getBuildpack("id-does-not-exist", "9.9.9")).isNull();
	}

}
