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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageArchiveManifest}.
 *
 * @author Scott Frederick
 * @author Andy Wilkinson
 */
class ImageArchiveManifestTests extends AbstractJsonTests {

	@Test
	void getLayersReturnsLayers() throws Exception {
		ImageArchiveManifest manifest = getManifest();
		List<String> expectedLayers = new ArrayList<>();
		for (int blankLayersCount = 0; blankLayersCount < 46; blankLayersCount++) {
			expectedLayers.add("blank_" + blankLayersCount);
		}
		expectedLayers.add("bb09e17fd1bd2ee47155f1349645fcd9fff31e1247c7ed99cad469f1c16a4216.tar");
		assertThat(manifest.getEntries()).hasSize(1);
		assertThat(manifest.getEntries().get(0).getLayers()).hasSize(47);
		assertThat(manifest.getEntries().get(0).getLayers()).isEqualTo(expectedLayers);
	}

	@Test
	void getLayersWithNoLayersReturnsEmptyList() throws Exception {
		String content = "[{\"Layers\": []}]";
		ImageArchiveManifest manifest = new ImageArchiveManifest(getObjectMapper().readTree(content));
		assertThat(manifest.getEntries()).hasSize(1);
		assertThat(manifest.getEntries().get(0).getLayers()).isEmpty();
	}

	@Test
	void getLayersWithEmptyManifestReturnsEmptyList() throws Exception {
		String content = "[]";
		ImageArchiveManifest manifest = new ImageArchiveManifest(getObjectMapper().readTree(content));
		assertThat(manifest.getEntries()).isEmpty();
	}

	private ImageArchiveManifest getManifest() throws IOException {
		return new ImageArchiveManifest(getObjectMapper().readTree(getContent("image-archive-manifest.json")));
	}

}
