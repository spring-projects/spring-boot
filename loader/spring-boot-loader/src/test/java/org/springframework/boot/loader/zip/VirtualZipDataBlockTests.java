/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link VirtualZipDataBlock}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class VirtualZipDataBlockTests {

	@TempDir
	File tempDir;

	private File file;

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.tempDir, "test.jar");
		TestJar.create(this.file);
	}

	@Test
	void createContainsValidZipContent() throws IOException {
		FileDataBlock data = new FileDataBlock(this.file.toPath());
		data.open();
		List<ZipCentralDirectoryFileHeaderRecord> centralRecords = new ArrayList<>();
		List<Long> centralRecordPositions = new ArrayList<>();
		ZipEndOfCentralDirectoryRecord eocd = ZipEndOfCentralDirectoryRecord.load(data).endOfCentralDirectoryRecord();
		long pos = eocd.offsetToStartOfCentralDirectory();
		for (int i = 0; i < eocd.totalNumberOfCentralDirectoryEntries(); i++) {
			ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(data, pos);
			String name = ZipString.readString(data, pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET,
					centralRecord.fileNameLength());
			if (name.endsWith(".jar")) {
				centralRecords.add(centralRecord);
				centralRecordPositions.add(pos);
			}
			pos += centralRecord.size();
		}
		NameOffsetLookups nameOffsetLookups = new NameOffsetLookups(2, centralRecords.size());
		for (int i = 0; i < centralRecords.size(); i++) {
			nameOffsetLookups.enable(i, true);
		}
		nameOffsetLookups.enable(0, true);
		File outputFile = new File(this.tempDir, "out.jar");
		try (VirtualZipDataBlock block = new VirtualZipDataBlock(data, nameOffsetLookups,
				centralRecords.toArray(ZipCentralDirectoryFileHeaderRecord[]::new),
				centralRecordPositions.stream().mapToLong(Long::longValue).toArray())) {
			try (FileOutputStream out = new FileOutputStream(outputFile)) {
				block.asInputStream().transferTo(out);
			}
		}
		try (FileSystem fileSystem = FileSystems.newFileSystem(outputFile.toPath())) {
			assertThatExceptionOfType(NoSuchFileException.class)
				.isThrownBy(() -> Files.size(fileSystem.getPath("nessted.jar")));
			assertThat(Files.size(fileSystem.getPath("sted.jar"))).isGreaterThan(0);
			assertThat(Files.size(fileSystem.getPath("other-nested.jar"))).isGreaterThan(0);
			assertThat(Files.size(fileSystem.getPath("ace nested.jar"))).isGreaterThan(0);
			assertThat(Files.size(fileSystem.getPath("lti-release.jar"))).isGreaterThan(0);
		}
	}

	@Test // gh-38063
	void createWithDescriptorRecordContainsValidZipContent() throws Exception {
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(this.file))) {
			ZipEntry entry = new ZipEntry("META-INF/");
			entry.setMethod(ZipEntry.DEFLATED);
			zip.putNextEntry(entry);
			zip.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
			zip.closeEntry();
		}
		byte[] bytes = Files.readAllBytes(this.file.toPath());
		CloseableDataBlock data = new ByteArrayDataBlock(bytes);
		List<ZipCentralDirectoryFileHeaderRecord> centralRecords = new ArrayList<>();
		List<Long> centralRecordPositions = new ArrayList<>();
		ZipEndOfCentralDirectoryRecord eocd = ZipEndOfCentralDirectoryRecord.load(data).endOfCentralDirectoryRecord();
		long pos = eocd.offsetToStartOfCentralDirectory();
		for (int i = 0; i < eocd.totalNumberOfCentralDirectoryEntries(); i++) {
			ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(data, pos);
			centralRecords.add(centralRecord);
			centralRecordPositions.add(pos);
			pos += centralRecord.size();
		}
		NameOffsetLookups nameOffsetLookups = new NameOffsetLookups(0, centralRecords.size());
		for (int i = 0; i < centralRecords.size(); i++) {
			nameOffsetLookups.enable(i, true);
		}
		nameOffsetLookups.enable(0, true);
		File outputFile = new File(this.tempDir, "out.jar");
		try (VirtualZipDataBlock block = new VirtualZipDataBlock(data, nameOffsetLookups,
				centralRecords.toArray(ZipCentralDirectoryFileHeaderRecord[]::new),
				centralRecordPositions.stream().mapToLong(Long::longValue).toArray())) {
			try (FileOutputStream out = new FileOutputStream(outputFile)) {
				block.asInputStream().transferTo(out);
			}
		}
		byte[] virtualBytes = Files.readAllBytes(outputFile.toPath());
		assertThat(bytes).isEqualTo(virtualBytes);
	}

}
