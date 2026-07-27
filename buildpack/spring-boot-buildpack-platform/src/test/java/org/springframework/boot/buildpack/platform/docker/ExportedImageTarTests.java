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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.TarArchive.Compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

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
		List<Path> temporaryDockerLayersTarFilesBefore = scanTemporaryDockerLayersTarFiles();
		ImageReference reference = ImageReference.of("test:latest");
		Path temporaryDockerFile;
		InputStream inputStream = spy(Objects.requireNonNull(getClass().getResourceAsStream(tarFile)));
		try (ExportedImageTar exportedImageTar = new ExportedImageTar(reference, inputStream)) {
			List<Path> temporaryDockerLayersTarFilesCurrent = scanTemporaryDockerLayersTarFiles();
			temporaryDockerLayersTarFilesCurrent.removeAll(temporaryDockerLayersTarFilesBefore);
			assertThat(temporaryDockerLayersTarFilesCurrent).hasSize(1);
			temporaryDockerFile = temporaryDockerLayersTarFilesCurrent.get(0);
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
		then(inputStream).should().close();
		assertThat(temporaryDockerFile).doesNotExist();
	}

	@Test
	void constructorWhenTarHasNoIndexOrManifestDeletesTempFile() throws IOException {
		List<Path> temporaryDockerLayersTarFilesBefore = scanTemporaryDockerLayersTarFiles();
		ImageReference reference = ImageReference.of("test:latest");
		InputStream notATarFile = spy(new ByteArrayInputStream("not-a-tar-file".getBytes(StandardCharsets.UTF_8)));
		assertThatIllegalStateException().isThrownBy(() -> new ExportedImageTar(reference, notATarFile))
			.withMessageContaining("does not contain 'index.json' or 'manifest.json'");
		then(notATarFile).should().close();
		assertThat(scanTemporaryDockerLayersTarFiles()).containsOnlyOnceElementsOf(temporaryDockerLayersTarFilesBefore)
			.hasSize(temporaryDockerLayersTarFilesBefore.size());
	}

	private static List<Path> scanTemporaryDockerLayersTarFiles() throws IOException {
		Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
		try (Stream<Path> stream = Files.list(tempDir)) {
			return stream.filter(Files::isRegularFile).filter((candidate) -> {
				String filename = candidate.getFileName().toString();
				return filename.startsWith("docker-layers") && filename.endsWith(".tar");
			}).collect(Collectors.toCollection(ArrayList::new));
		}
	}

}
