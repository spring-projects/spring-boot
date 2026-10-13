/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

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
	void getLayersReturnsLayers() {
		String content = getContentAsString("image-archive-manifest.json");
		ImageArchiveManifest manifest = getManifest(content);
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
	void getLayersWithNoLayersReturnsEmptyList() {
		String content = "[{\"Layers\": []}]";
		ImageArchiveManifest manifest = getManifest(content);
		assertThat(manifest.getEntries()).hasSize(1);
		assertThat(manifest.getEntries().get(0).getLayers()).isEmpty();
	}

	@Test
	void getLayersWithEmptyManifestReturnsEmptyList() {
		String content = "[]";
		ImageArchiveManifest manifest = getManifest(content);
		assertThat(manifest.getEntries()).isEmpty();
	}

	private ImageArchiveManifest getManifest(String content) {
		return new ImageArchiveManifest(getJsonMapper().readTree(content));
	}

}
