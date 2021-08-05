/*
 * Copyright 2012-2021 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility to create test tgz files.
 *
 * @author Scott Frederick
 */
class TestTarGzip {

	private final File buildpackDir;

	TestTarGzip(File buildpackDir) {
		this.buildpackDir = buildpackDir;
	}

	Path createArchive() throws Exception {
		return createArchive(true);
	}

	Path createEmptyArchive() throws Exception {
		return createArchive(false);
	}

	private Path createArchive(boolean addContent) throws Exception {
		Path path = Paths.get(this.buildpackDir.getAbsolutePath(), "buildpack.tar");
		Path archive = Files.createFile(path);
		if (addContent) {
			writeBuildpackContentToArchive(archive);
		}
		return compressBuildpackArchive(archive);
	}

	private Path compressBuildpackArchive(Path archive) throws Exception {
		Path tgzPath = Paths.get(this.buildpackDir.getAbsolutePath(), "buildpack.tgz");
		FileCopyUtils.copy(Files.newInputStream(archive),
				new GzipCompressorOutputStream(Files.newOutputStream(tgzPath)));
		return tgzPath;
	}

	private void writeBuildpackContentToArchive(Path archive) throws Exception {
		StringBuilder buildpackToml = new StringBuilder();
		buildpackToml.append("[buildpack]\n");
		buildpackToml.append("id = \"example/buildpack1\"\n");
		buildpackToml.append("version = \"0.0.1\"\n");
		buildpackToml.append("name = \"Example buildpack\"\n");
		buildpackToml.append("homepage = \"https://github.com/example/example-buildpack\"\n");
		buildpackToml.append("[[stacks]]\n");
		buildpackToml.append("id = \"io.buildpacks.stacks.bionic\"\n");
		String detectScript = "#!/usr/bin/env bash\n" + "echo \"---> detect\"\n";
		String buildScript = "#!/usr/bin/env bash\n" + "echo \"---> build\"\n";
		try (TarArchiveOutputStream tar = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
			writeEntry(tar, "buildpack.toml", buildpackToml.toString());
			writeEntry(tar, "bin/");
			writeEntry(tar, "bin/detect", detectScript);
			writeEntry(tar, "bin/build", buildScript);
			tar.finish();
		}
	}

	private void writeEntry(TarArchiveOutputStream tar, String entryName) throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(entryName);
		tar.putArchiveEntry(entry);
		tar.closeArchiveEntry();
	}

	private void writeEntry(TarArchiveOutputStream tar, String entryName, String content) throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(entryName);
		entry.setSize(content.length());
		tar.putArchiveEntry(entry);
		IOUtils.copy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), tar);
		tar.closeArchiveEntry();
	}

	void assertHasExpectedLayers(Buildpack buildpack) throws IOException {
		List<ByteArrayOutputStream> layers = new ArrayList<>();
		buildpack.apply((layer) -> {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			layer.writeTo(out);
			layers.add(out);
		});
		assertThat(layers).hasSize(1);
		byte[] content = layers.get(0).toByteArray();
		try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(content))) {
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/");
			assertThat(tar.getNextEntry().getName())
					.isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/detect");
			assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/build");
			assertThat(tar.getNextEntry()).isNull();
		}
	}

}
