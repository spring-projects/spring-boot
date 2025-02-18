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

package org.springframework.boot.loader.jar;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManifestInfo}.
 *
 * @author Phillip Webb
 */
class ManifestInfoTests {

	@Test
	void noneReturnsNoDetails() {
		assertThat(ManifestInfo.NONE.getManifest()).isNull();
		assertThat(ManifestInfo.NONE.isMultiRelease()).isFalse();
	}

	@Test
	void getManifestReturnsManifest() {
		Manifest manifest = new Manifest();
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.getManifest()).isSameAs(manifest);
	}

	@Test
	void isMultiReleaseWhenHasMultiReleaseAttributeReturnsTrue() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(new Name("Multi-Release"), "true");
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.isMultiRelease()).isTrue();
	}

	@Test
	void isMultiReleaseWhenHasNoMultiReleaseAttributeReturnsFalse() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(new Name("Random-Release"), "true");
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.isMultiRelease()).isFalse();
	}

}
