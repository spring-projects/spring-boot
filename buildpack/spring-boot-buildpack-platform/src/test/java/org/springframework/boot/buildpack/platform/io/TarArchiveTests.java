/*
 * Copyright 2012-present the original author or authors.
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
			TarArchiveEntry entry = tarStream.getNextEntry();
			while (entry != null) {
				entries.add(entry);
				entry = tarStream.getNextEntry();
			}
			assertThat(entries).hasSize(6);
			assertThat(entries.get(0).getName()).isEqualTo("/workspace/");
			assertThat(entries.get(0).getLongUserId()).isEqualTo(123);
			assertThat(entries.get(0).getLongGroupId()).isEqualTo(456);
			assertThat(entries.get(2).getName()).isEqualTo("/cnb/");
			assertThat(entries.get(2).getLongUserId()).isZero();
			assertThat(entries.get(2).getLongGroupId()).isZero();
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
