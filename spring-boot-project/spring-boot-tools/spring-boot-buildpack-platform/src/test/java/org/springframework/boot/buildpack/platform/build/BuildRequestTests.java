/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link BuildRequest}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
public class BuildRequestTests {

	@TempDir
	File tempDir;

	@Test
	void forJarFileReturnsRequest() throws IOException {
		File jarFile = new File(this.tempDir, "my-app-0.0.1.jar");
		writeTestJarFile(jarFile);
		BuildRequest request = BuildRequest.forJarFile(jarFile);
		assertThat(request.getName().toString()).isEqualTo("docker.io/library/my-app:0.0.1");
		assertThat(request.getBuilder().toString()).isEqualTo(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME);
		assertThat(request.getApplicationContent(Owner.ROOT)).satisfies(this::hasExpectedJarContent);
		assertThat(request.getEnv()).isEmpty();
	}

	@Test
	void forJarFileWithNameReturnsRequest() throws IOException {
		File jarFile = new File(this.tempDir, "my-app-0.0.1.jar");
		writeTestJarFile(jarFile);
		BuildRequest request = BuildRequest.forJarFile(ImageReference.of("test-app"), jarFile);
		assertThat(request.getName().toString()).isEqualTo("docker.io/library/test-app:latest");
		assertThat(request.getBuilder().toString()).isEqualTo(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME);
		assertThat(request.getApplicationContent(Owner.ROOT)).satisfies(this::hasExpectedJarContent);
		assertThat(request.getEnv()).isEmpty();
	}

	@Test
	void forJarFileWhenJarFileIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildRequest.forJarFile(null))
				.withMessage("JarFile must not be null");
	}

	@Test
	void forJarFileWhenJarFileIsMissingThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BuildRequest.forJarFile(new File(this.tempDir, "missing.jar")))
				.withMessage("JarFile must exist");

	}

	@Test
	void forJarFileWhenJarFileIsDirectoryThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildRequest.forJarFile(this.tempDir))
				.withMessage("JarFile must be a file");
	}

	@Test
	void withBuilderUpdatesBuilder() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"))
				.withBuilder(ImageReference.of("spring/builder"));
		assertThat(request.getBuilder().toString()).isEqualTo("docker.io/spring/builder:latest");
	}

	@Test
	void withCreatorUpdatesCreator() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
		BuildRequest withCreator = request.withCreator(Creator.withVersion("1.0.0"));
		assertThat(request.getCreator().getName()).isEqualTo("Spring Boot");
		assertThat(request.getCreator().getVersion()).isEqualTo("");
		assertThat(withCreator.getCreator().getName()).isEqualTo("Spring Boot");
		assertThat(withCreator.getCreator().getVersion()).isEqualTo("1.0.0");
	}

	@Test
	void withEnvAddsEnvEntry() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
		BuildRequest withEnv = request.withEnv("spring", "boot");
		assertThat(request.getEnv()).isEmpty();
		assertThat(withEnv.getEnv()).containsExactly(entry("spring", "boot"));
	}

	@Test
	void withEnvMapAddsEnvEntries() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
		Map<String, String> env = new LinkedHashMap<>();
		env.put("spring", "boot");
		env.put("test", "test");
		BuildRequest withEnv = request.withEnv(env);
		assertThat(request.getEnv()).isEmpty();
		assertThat(withEnv.getEnv()).containsExactly(entry("spring", "boot"), entry("test", "test"));
	}

	@Test
	void withEnvWhenKeyIsNullThrowsException() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
		assertThatIllegalArgumentException().isThrownBy(() -> request.withEnv(null, "test"))
				.withMessage("Name must not be empty");
	}

	@Test
	void withEnvWhenValueIsNullThrowsException() throws IOException {
		BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
		assertThatIllegalArgumentException().isThrownBy(() -> request.withEnv("test", null))
				.withMessage("Value must not be empty");
	}

	private void hasExpectedJarContent(TarArchive archive) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			archive.writeTo(outputStream);
			try (TarArchiveInputStream tar = new TarArchiveInputStream(
					new ByteArrayInputStream(outputStream.toByteArray()))) {
				assertThat(tar.getNextEntry().getName()).isEqualTo("spring/");
				assertThat(tar.getNextEntry().getName()).isEqualTo("spring/boot");
				assertThat(tar.getNextEntry()).isNull();
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private File writeTestJarFile(String name) throws IOException {
		File file = new File(this.tempDir, name);
		writeTestJarFile(file);
		return file;
	}

	private void writeTestJarFile(File file) throws IOException {
		try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
			ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
			zip.putArchiveEntry(dirEntry);
			zip.closeArchiveEntry();
			ZipArchiveEntry fileEntry = new ZipArchiveEntry("spring/boot");
			zip.putArchiveEntry(fileEntry);
			zip.write("test".getBytes(StandardCharsets.UTF_8));
			zip.closeArchiveEntry();
		}
	}

}
