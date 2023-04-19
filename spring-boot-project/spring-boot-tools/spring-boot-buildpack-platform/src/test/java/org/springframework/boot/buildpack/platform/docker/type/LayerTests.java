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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Layer}.
 *
 * @author Phillip Webb
 */
class LayerTests {

	@Test
	void ofWhenLayoutIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Layer.of((IOConsumer<Layout>) null))
			.withMessage("Layout must not be null");
	}

	@Test
	void fromTarArchiveWhenTarArchiveIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Layer.fromTarArchive(null))
			.withMessage("TarArchive must not be null");
	}

	@Test
	void ofCreatesLayer() throws Exception {
		Layer layer = Layer.of((layout) -> {
			layout.directory("/directory", Owner.ROOT);
			layout.file("/directory/file", Owner.ROOT, Content.of("test"));
		});
		assertThat(layer.getId())
			.hasToString("sha256:d03a34f73804698c875eb56ff694fc2fceccc69b645e4adceb004ed13588613b");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		layer.writeTo(outputStream);
		try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			assertThat(tarStream.getNextTarEntry().getName()).isEqualTo("/directory/");
			assertThat(tarStream.getNextTarEntry().getName()).isEqualTo("/directory/file");
			assertThat(tarStream.getNextTarEntry()).isNull();
		}
	}

}
