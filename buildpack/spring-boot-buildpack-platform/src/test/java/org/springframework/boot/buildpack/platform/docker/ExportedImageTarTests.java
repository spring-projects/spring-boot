/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.TarArchive.Compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ExportedImageTar}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ExportedImageTarTests {

	@ParameterizedTest
	@ValueSource(strings = { "export-docker-desktop.tar", "export-docker-desktop-containerd.tar",
			"export-docker-desktop-containerd-manifest-list.tar", "export-docker-engine.tar", "export-podman.tar",
			"export-docker-desktop-nested-index.tar", "export-docker-desktop-containerd-alt-mediatype.tar" })
	void test(String tarFile) throws Exception {
		ImageReference reference = ImageReference.of("test:latest");
		try (ExportedImageTar exportedImageTar = new ExportedImageTar(reference,
				getClass().getResourceAsStream(tarFile))) {
			Compression expectedCompression = (!tarFile.contains("containerd")) ? Compression.NONE : Compression.GZIP;
			String expectedName = (expectedCompression != Compression.GZIP)
					? "5caae51697b248b905dca1a4160864b0e1a15c300981736555cdce6567e8d477"
					: "f0f1fd1bdc71ac6a4dc99cea5f5e45c86c5ec26fe4d1daceeb78207303606429";
			List<String> names = new ArrayList<>();
			exportedImageTar.exportLayers((name, tarArchive) -> {
				names.add(name);
				assertThat(tarArchive.getCompression()).isEqualTo(expectedCompression);
			});
			assertThat(names).filteredOn((name) -> name.contains(expectedName)).isNotEmpty();
		}
	}

	@Test
	void constructorWhenTarHasNoIndexOrManifestDeletesTempFile() throws Exception {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		Set<String> tempsBefore = listTempFileNames(tempDir);
		ImageReference reference = ImageReference.of("test:latest");
		assertThatIllegalStateException().isThrownBy(() -> new ExportedImageTar(reference, tarWithoutIndexOrManifest()))
			.withMessageContaining("does not contain 'index.json' or 'manifest.json'");
		Set<String> leaked = new HashSet<>(listTempFileNames(tempDir));
		leaked.removeAll(tempsBefore);
		assertThat(leaked).as("temp file must be deleted when the constructor fails").isEmpty();
	}

	private InputStream tarWithoutIndexOrManifest() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (TarArchiveOutputStream tar = new TarArchiveOutputStream(out)) {
			byte[] data = "test".getBytes(StandardCharsets.UTF_8);
			TarArchiveEntry entry = new TarArchiveEntry("some-file.txt");
			entry.setSize(data.length);
			tar.putArchiveEntry(entry);
			tar.write(data);
			tar.closeArchiveEntry();
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	private Set<String> listTempFileNames(File tempDir) {
		File[] files = tempDir.listFiles((dir, name) -> name.startsWith("docker-layers-"));
		Set<String> names = new HashSet<>();
		if (files != null) {
			for (File file : files) {
				names.add(file.getName());
			}
		}
		return names;
	}

}
