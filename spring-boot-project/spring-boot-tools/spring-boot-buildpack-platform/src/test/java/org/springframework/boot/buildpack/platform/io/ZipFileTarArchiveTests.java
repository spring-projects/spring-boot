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

package org.springframework.boot.buildpack.platform.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ZipFileTarArchive}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ZipFileTarArchiveTests {

	@TempDir
	File tempDir;

	@Test
	void createWhenZipIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ZipFileTarArchive(null, Owner.ROOT))
				.withMessage("Zip must not be null");
	}

	@Test
	void createWhenOwnerIsNullThrowsException() throws Exception {
		File file = new File(this.tempDir, "test.zip");
		writeTestZip(file);
		assertThatIllegalArgumentException().isThrownBy(() -> new ZipFileTarArchive(file, null))
				.withMessage("Owner must not be null");
	}

	@Test
	void writeToAdaptsContent() throws Exception {
		Owner owner = Owner.of(123, 456);
		File file = new File(this.tempDir, "test.zip");
		writeTestZip(file);
		TarArchive tarArchive = TarArchive.fromZip(file, owner);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		tarArchive.writeTo(outputStream);
		try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			TarArchiveEntry dirEntry = tarStream.getNextTarEntry();
			assertThat(dirEntry.getName()).isEqualTo("spring/");
			assertThat(dirEntry.getLongUserId()).isEqualTo(123);
			assertThat(dirEntry.getLongGroupId()).isEqualTo(456);
			TarArchiveEntry fileEntry = tarStream.getNextTarEntry();
			assertThat(fileEntry.getName()).isEqualTo("spring/boot");
			assertThat(fileEntry.getLongUserId()).isEqualTo(123);
			assertThat(fileEntry.getLongGroupId()).isEqualTo(456);
			assertThat(fileEntry.getSize()).isEqualTo(4);
			assertThat(fileEntry.getMode()).isEqualTo(0755);
			String fileContent = StreamUtils.copyToString(tarStream, StandardCharsets.UTF_8);
			assertThat(fileContent).isEqualTo("test");
		}
	}

	private void writeTestZip(File file) throws IOException {
		try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
			ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
			zip.putArchiveEntry(dirEntry);
			zip.closeArchiveEntry();
			ZipArchiveEntry fileEntry = new ZipArchiveEntry("spring/boot");
			fileEntry.setUnixMode(0755);
			zip.putArchiveEntry(fileEntry);
			zip.write("test".getBytes(StandardCharsets.UTF_8));
			zip.closeArchiveEntry();
		}
	}

}
