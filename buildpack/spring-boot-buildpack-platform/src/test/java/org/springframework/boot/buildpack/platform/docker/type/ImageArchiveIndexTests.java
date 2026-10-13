/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageArchiveIndex}.
 *
 * @author Phillip Webb
 */
class ImageArchiveIndexTests extends AbstractJsonTests {

	@Test
	void loadJson() {
		String content = getContentAsString("image-archive-index.json");
		ImageArchiveIndex index = getIndex(content);
		assertThat(index.getSchemaVersion()).isEqualTo(2);
		assertThat(index.getManifests()).hasSize(1);
		BlobReference manifest = index.getManifests().get(0);
		assertThat(manifest.getMediaType()).isEqualTo("application/vnd.docker.distribution.manifest.list.v2+json");
		assertThat(manifest.getDigest())
			.isEqualTo("sha256:3bbe02431d8e5124ffe816ec27bf6508b50edd1d10218be1a03e799a186b9004");
	}

	private ImageArchiveIndex getIndex(String content) {
		return new ImageArchiveIndex(getJsonMapper().readTree(content));
	}

}
