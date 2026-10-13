/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManifestList}.
 *
 * @author Phillip Webb
 */
class ManifestListTests extends AbstractJsonTests {

	@Test
	void loadJsonFromDistributionManifestList() {
		String content = getContentAsString("distribution-manifest-list.json");
		ManifestList manifestList = getManifestList(content);
		assertThat(manifestList.getSchemaVersion()).isEqualTo(2);
		assertThat(manifestList.getMediaType()).isEqualTo("application/vnd.docker.distribution.manifest.list.v2+json");
		assertThat(manifestList.getManifests()).hasSize(2);
	}

	private ManifestList getManifestList(String content) {
		return new ManifestList(getJsonMapper().readTree(content));
	}

}
