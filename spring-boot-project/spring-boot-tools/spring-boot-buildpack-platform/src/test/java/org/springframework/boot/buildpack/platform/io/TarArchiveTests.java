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

package org.springframework.boot.buildpack.platform.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TarArchive}.
 *
 * @author Phillip Webb
 */
class TarArchiveTests {

	@TempDir
	File tempDir;

	@Test
	void ofWritesTarContent() throws Exception {
		Owner owner = Owner.of(123, 456);
		TarArchive tarArchive = TarArchive.of((content) -> {
			content.directory("/workspace", owner);
			content.directory("/layers", owner);
			content.directory("/cnb", Owner.ROOT);
			content.directory("/cnb/buildpacks", Owner.ROOT);
			content.directory("/platform", Owner.ROOT);
			content.directory("/platform/env", Owner.ROOT);
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		tarArchive.writeTo(outputStream);
		try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			List<TarArchiveEntry> entries = new ArrayList<>();
			TarArchiveEntry entry = tarStream.getNextTarEntry();
			while (entry != null) {
				entries.add(entry);
				entry = tarStream.getNextTarEntry();
			}
			assertThat(entries).hasSize(6);
			assertThat(entries.get(0).getName()).isEqualTo("/workspace/");
			assertThat(entries.get(0).getLongUserId()).isEqualTo(123);
			assertThat(entries.get(0).getLongGroupId()).isEqualTo(456);
			assertThat(entries.get(2).getName()).isEqualTo("/cnb/");
			assertThat(entries.get(2).getLongUserId()).isEqualTo(0);
			assertThat(entries.get(2).getLongGroupId()).isEqualTo(0);
		}
	}

	@Test
	void fromZipFileReturnsZipFileAdapter() throws Exception {
		Owner owner = Owner.of(123, 456);
		File file = new File(this.tempDir, "test.zip");
		writeTestZip(file);
		TarArchive tarArchive = TarArchive.fromZip(file, owner);
		assertThat(tarArchive).isInstanceOf(ZipFileTarArchive.class);
	}

	private void writeTestZip(File file) throws IOException {
		try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
			ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
			zip.putArchiveEntry(dirEntry);
			zip.closeArchiveEntry();
		}
	}

}
