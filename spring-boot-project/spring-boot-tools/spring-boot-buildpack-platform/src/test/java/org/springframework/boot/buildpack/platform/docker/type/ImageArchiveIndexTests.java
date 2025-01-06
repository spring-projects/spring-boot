/*
 * Copyright 2012-2024 the original author or authors.
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

/**
 * Tests for {@link ImageArchiveIndex}.
 *
 * @author Phillip Webb
 */
class ImageArchiveIndexTests extends AbstractJsonTests {

	@Test
	void loadJson() throws IOException {
		String content = getContentAsString("image-archive-index.json");
		ImageArchiveIndex index = getIndex(content);
		assertThat(index.getSchemaVersion()).isEqualTo(2);
		assertThat(index.getManifests()).hasSize(1);
		BlobReference manifest = index.getManifests().get(0);
		assertThat(manifest.getMediaType()).isEqualTo("application/vnd.docker.distribution.manifest.list.v2+json");
		assertThat(manifest.getDigest())
			.isEqualTo("sha256:3bbe02431d8e5124ffe816ec27bf6508b50edd1d10218be1a03e799a186b9004");
	}

	private ImageArchiveIndex getIndex(String content) throws IOException {
		return new ImageArchiveIndex(getObjectMapper().readTree(content));
	}

}
