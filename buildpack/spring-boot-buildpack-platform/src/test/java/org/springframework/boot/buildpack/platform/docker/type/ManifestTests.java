/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Manifest}.
 *
 * @author Phillip Webb
 */
class ManifestTests extends AbstractJsonTests {

	@Test
	void loadJsonFromDistributionManifest() {
		String content = getContentAsString("distribution-manifest.json");
		Manifest manifestList = getManifest(content);
		assertThat(manifestList.getSchemaVersion()).isEqualTo(2);
		assertThat(manifestList.getMediaType()).isEqualTo("application/vnd.docker.distribution.manifest.v2+json");
		assertThat(manifestList.getLayers()).hasSize(1);
	}

	@Test
	void loadJsonFromImageManifest() {
		String content = getContentAsString("image-manifest.json");
		Manifest manifestList = getManifest(content);
		assertThat(manifestList.getSchemaVersion()).isEqualTo(2);
		assertThat(manifestList.getMediaType()).isEqualTo("application/vnd.oci.image.manifest.v1+json");
		assertThat(manifestList.getLayers()).hasSize(1);
	}

	private Manifest getManifest(String content) {
		return new Manifest(getJsonMapper().readTree(content));
	}

}
