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

package org.springframework.boot.loader.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Iterator;
import java.util.Random;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.ZipContent.Entry;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ZipContent}.
 *
 * @author Phillip Webb
 * @author Martin Lau
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ZipContentTests {

	@TempDir
	File tempDir;

	private File file;

	private ZipContent zipContent;

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.tempDir, "test.jar");
		TestJar.create(this.file);
		this.zipContent = ZipContent.open(this.file.toPath());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (this.zipContent != null) {
			try {
				this.zipContent.close();
			}
			catch (IllegalStateException ex) {
			}
		}
	}

	@Test
	void getCommentReturnsComment() {
		assertThat(this.zipContent.getComment()).isEqualTo("outer");
	}

	@Test
	void getCommentWhenClosedThrowsException() throws IOException {
		this.zipContent.close();
		assertThatIllegalStateException().isThrownBy(() -> this.zipContent.getComment())
			.withMessage("Zip content closed");
	}

	@Test
	void getEntryWhenPresentReturnsEntry() {
		Entry entry = this.zipContent.getEntry("1.dat");
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("1.dat");
	}

	@Test
	void getEntryWhenMissingReturnsNull() {
		assertThat(this.zipContent.getEntry("missing.dat")).isNull();
	}

	@Test
	void getEntryWithPrefixWhenPresentReturnsEntry() {
		Entry entry = this.zipContent.getEntry("1", ".dat");
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("1.dat");
	}

	@Test
	void getEntryWithLongPrefixWhenNameIsShorterReturnsNull() {
		Entry entry = this.zipContent.getEntry("iamaverylongprefixandiwontfindanything", "1.dat");
		assertThat(entry).isNull();
	}

	@Test
	void getEntryWithPrefixWhenMissingReturnsNull() {
		assertThat(this.zipContent.getEntry("miss", "ing.dat")).isNull();
	}

	@Test
	void getEntryWhenUsingSlashesIsCompatibleWithZipFile() throws IOException {
		try (ZipFile zipFile = new ZipFile(this.file)) {
			assertThat(zipFile.getEntry("META-INF").getName()).isEqualTo("META-INF/");
			assertThat(this.zipContent.getEntry("META-INF").getName()).isEqualTo("META-INF/");
			assertThat(zipFile.getEntry("META-INF/").getName()).isEqualTo("META-INF/");
			assertThat(this.zipContent.getEntry("META-INF/").getName()).isEqualTo("META-INF/");
			assertThat(zipFile.getEntry("d/9.dat").getName()).isEqualTo("d/9.dat");
			assertThat(this.zipContent.getEntry("d/9.dat").getName()).isEqualTo("d/9.dat");
			assertThat(zipFile.getEntry("d/9.dat/")).isNull();
			assertThat(this.zipContent.getEntry("d/9.dat/")).isNull();
		}
	}

	@Test
	void getManifestEntry() throws Exception {
		Entry entry = this.zipContent.getEntry("META-INF/MANIFEST.MF");
		try (CloseableDataBlock dataBlock = entry.openContent()) {
			Manifest manifest = new Manifest(asInflaterInputStream(dataBlock));
			assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("j1");
		}
	}

	@Test
	void getEntryAsCreatesCompatibleEntries() throws IOException {
		try (ZipFile zipFile = new ZipFile(this.file)) {
			Iterator<? extends ZipEntry> expected = zipFile.entries().asIterator();
			int i = 0;
			while (expected.hasNext()) {
				Entry actual = this.zipContent.getEntry(i++);
				assertThatFieldsAreEqual(actual.as(ZipEntry::new), expected.next());
			}
		}
	}

	private void assertThatFieldsAreEqual(ZipEntry actual, ZipEntry expected) {
		assertThat(actual.getName()).isEqualTo(expected.getName());
		assertThat(actual.getTime()).isEqualTo(expected.getTime());
		assertThat(actual.getLastModifiedTime()).isEqualTo(expected.getLastModifiedTime());
		assertThat(actual.getLastAccessTime()).isEqualTo(expected.getLastAccessTime());
		assertThat(actual.getCreationTime()).isEqualTo(expected.getCreationTime());
		assertThat(actual.getSize()).isEqualTo(expected.getSize());
		assertThat(actual.getCompressedSize()).isEqualTo(expected.getCompressedSize());
		assertThat(actual.getCrc()).isEqualTo(expected.getCrc());
		assertThat(actual.getMethod()).isEqualTo(expected.getMethod());
		assertThat(actual.getExtra()).isEqualTo(expected.getExtra());
		assertThat(actual.getComment()).isEqualTo(expected.getComment());
	}

	@Test
	void sizeReturnsNumberOfEntries() {
		assertThat(this.zipContent.size()).isEqualTo(12);
	}

	@Test
	void nestedJarFileReturnsNestedJar() throws IOException {
		try (ZipContent nested = ZipContent.open(this.file.toPath(), "nested.jar")) {
			assertThat(nested.size()).isEqualTo(5);
			assertThat(nested.getComment()).isEqualTo("nested");
			assertThat(nested.size()).isEqualTo(5);
			assertThat(nested.getEntry(0).getName()).isEqualTo("META-INF/");
			assertThat(nested.getEntry(1).getName()).isEqualTo("META-INF/MANIFEST.MF");
			assertThat(nested.getEntry(2).getName()).isEqualTo("3.dat");
			assertThat(nested.getEntry(3).getName()).isEqualTo("4.dat");
			assertThat(nested.getEntry(4).getName()).isEqualTo("\u00E4.dat");
		}
	}

	@Test
	void nestedJarFileWhenNameEndsInSlashThrowsException() {
		assertThatIOException().isThrownBy(() -> ZipContent.open(this.file.toPath(), "nested.jar/"))
			.withMessageStartingWith("Nested entry 'nested.jar/' not found in container zip");
	}

	@Test
	void nestedDirectoryReturnsNestedJar() throws IOException {
		try (ZipContent nested = ZipContent.open(this.file.toPath(), "d/")) {
			assertThat(nested.size()).isEqualTo(3);
			assertThat(nested.getEntry("9.dat")).isNotNull();
			assertThat(nested.getEntry(0).getName()).isEqualTo("META-INF/");
			assertThat(nested.getEntry(1).getName()).isEqualTo("META-INF/MANIFEST.MF");
			assertThat(nested.getEntry(2).getName()).isEqualTo("9.dat");
		}
	}

	@Test
	void nestedDirectoryWhenNotEndingInSlashThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ZipContent.open(this.file.toPath(), "d"))
			.withMessage("Nested entry name must end with '/'");
	}

	@Test
	void getDataWhenNestedDirectoryReturnsVirtualZipDataBlock() throws IOException {
		try (ZipContent nested = ZipContent.open(this.file.toPath(), "d/")) {
			File file = new File(this.tempDir, "included.zip");
			write(file, nested.openRawZipData());
			try (ZipFile loadedZipFile = new ZipFile(file)) {
				assertThat(loadedZipFile.size()).isEqualTo(3);
				assertThat(loadedZipFile.stream().map(ZipEntry::getName)).containsExactly("META-INF/",
						"META-INF/MANIFEST.MF", "9.dat");
				assertThat(loadedZipFile.getEntry("9.dat")).isNotNull();
				try (InputStream in = loadedZipFile.getInputStream(loadedZipFile.getEntry("9.dat"))) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					in.transferTo(out);
					assertThat(out.toByteArray()).containsExactly(0x09);
				}
			}
		}
	}

	@Test
	void loadWhenHasFrontMatterOpensZip() throws IOException {
		File fileWithFrontMatter = new File(this.tempDir, "withfrontmatter.jar");
		FileOutputStream outputStream = new FileOutputStream(fileWithFrontMatter);
		StreamUtils.copy("#/bin/bash", Charset.defaultCharset(), outputStream);
		FileCopyUtils.copy(new FileInputStream(this.file), outputStream);
		try (ZipContent zip = ZipContent.open(fileWithFrontMatter.toPath())) {
			assertThat(zip.size()).isEqualTo(12);
			assertThat(zip.getEntry(0).getName()).isEqualTo("META-INF/");
			assertThat(zip.getEntry(1).getName()).isEqualTo("META-INF/MANIFEST.MF");
			assertThat(zip.getEntry(2).getName()).isEqualTo("1.dat");
			assertThat(zip.getEntry(3).getName()).isEqualTo("2.dat");
			assertThat(zip.getEntry(4).getName()).isEqualTo("d/");
			assertThat(zip.getEntry(5).getName()).isEqualTo("d/9.dat");
			assertThat(zip.getEntry(6).getName()).isEqualTo("special/");
			assertThat(zip.getEntry(7).getName()).isEqualTo("special/\u00EB.dat");
			assertThat(zip.getEntry(8).getName()).isEqualTo("nested.jar");
			assertThat(zip.getEntry(9).getName()).isEqualTo("another-nested.jar");
			assertThat(zip.getEntry(10).getName()).isEqualTo("space nested.jar");
			assertThat(zip.getEntry(11).getName()).isEqualTo("multi-release.jar");
		}
	}

	@Test
	void openWhenZip64ThatExceedsZipEntryLimitOpensZip() throws Exception {
		File zip64File = new File(this.tempDir, "zip64.zip");
		FileCopyUtils.copy(zip64Bytes(), zip64File);
		try (ZipContent zip64Content = ZipContent.open(zip64File.toPath())) {
			assertThat(zip64Content.size()).isEqualTo(65537);
			for (int i = 0; i < zip64Content.size(); i++) {
				Entry entry = zip64Content.getEntry(i);
				try (CloseableDataBlock dataBlock = entry.openContent()) {
					assertThat(asInflaterInputStream(dataBlock)).hasContent("Entry " + (i + 1));
				}
			}
		}
	}

	@Test
	void openWhenZip64ThatExceedsZipSizeLimitOpensZip() throws Exception {
		Assumptions.assumeTrue(this.tempDir.getFreeSpace() > 6 * 1024 * 1024 * 1024, "Insufficient disk space");
		File zip64File = new File(this.tempDir, "zip64.zip");
		File entryFile = new File(this.tempDir, "entry.dat");
		CRC32 crc32 = new CRC32();
		try (FileOutputStream entryOut = new FileOutputStream(entryFile)) {
			byte[] data = new byte[1024 * 1024];
			new Random().nextBytes(data);
			for (int i = 0; i < 1024; i++) {
				entryOut.write(data);
				crc32.update(data);
			}
		}
		try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zip64File))) {
			for (int i = 0; i < 6; i++) {
				ZipEntry storedEntry = new ZipEntry("huge-" + i);
				storedEntry.setSize(entryFile.length());
				storedEntry.setCompressedSize(entryFile.length());
				storedEntry.setCrc(crc32.getValue());
				storedEntry.setMethod(ZipEntry.STORED);
				zipOutput.putNextEntry(storedEntry);
				try (FileInputStream entryIn = new FileInputStream(entryFile)) {
					StreamUtils.copy(entryIn, zipOutput);
				}
				zipOutput.closeEntry();
			}
		}
		try (ZipContent zip64Content = ZipContent.open(zip64File.toPath())) {
			assertThat(zip64Content.size()).isEqualTo(6);
		}
	}

	@Test
	void nestedZip64CanBeRead() throws Exception {
		File containerFile = new File(this.tempDir, "outer.zip");
		try (ZipOutputStream jarOutput = new ZipOutputStream(new FileOutputStream(containerFile))) {
			ZipEntry nestedEntry = new ZipEntry("nested-zip64.zip");
			byte[] contents = zip64Bytes();
			nestedEntry.setSize(contents.length);
			nestedEntry.setCompressedSize(contents.length);
			CRC32 crc32 = new CRC32();
			crc32.update(contents);
			nestedEntry.setCrc(crc32.getValue());
			nestedEntry.setMethod(ZipEntry.STORED);
			jarOutput.putNextEntry(nestedEntry);
			jarOutput.write(contents);
			jarOutput.closeEntry();
		}
		try (ZipContent nestedZip = ZipContent.open(containerFile.toPath(), "nested-zip64.zip")) {
			assertThat(nestedZip.size()).isEqualTo(65537);
			for (int i = 0; i < nestedZip.size(); i++) {
				Entry entry = nestedZip.getEntry(i);
				try (CloseableDataBlock content = entry.openContent()) {
					assertThat(asInflaterInputStream(content)).hasContent("Entry " + (i + 1));
				}
			}
		}
	}

	private byte[] zip64Bytes() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ZipOutputStream zipOutput = new ZipOutputStream(bytes);
		for (int i = 0; i < 65537; i++) {
			zipOutput.putNextEntry(new ZipEntry(i + ".dat"));
			zipOutput.write(("Entry " + (i + 1)).getBytes(StandardCharsets.UTF_8));
			zipOutput.closeEntry();
		}
		zipOutput.close();
		return bytes.toByteArray();
	}

	@Test
	void entryWithEpochTimeOfZeroShouldNotFail() throws Exception {
		File file = createZipFileWithEpochTimeOfZero();
		try (ZipContent zip = ZipContent.open(file.toPath())) {
			ZipEntry entry = zip.getEntry(0).as(ZipEntry::new);
			assertThat(entry.getLastModifiedTime().toInstant()).isEqualTo(Instant.EPOCH);
			assertThat(entry.getName()).isEqualTo("1.dat");
		}
	}

	private File createZipFileWithEpochTimeOfZero() throws Exception {
		File file = new File(this.tempDir, "temp.zip");
		String comment = "outer";
		try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(file))) {
			zipOutput.setComment(comment);
			ZipEntry entry = new ZipEntry("1.dat");
			entry.setLastModifiedTime(FileTime.from(Instant.EPOCH));
			zipOutput.putNextEntry(entry);
			zipOutput.write(new byte[] { (byte) 1 });
			zipOutput.closeEntry();
		}
		ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
		data.order(ByteOrder.LITTLE_ENDIAN);
		int endOfCentralDirectoryRecordPos = data.remaining() - ZipFile.ENDHDR - comment.getBytes().length;
		data.position(endOfCentralDirectoryRecordPos + ZipFile.ENDOFF);
		int startOfCentralDirectoryOffset = data.getInt();
		data.position(startOfCentralDirectoryOffset + ZipFile.CENOFF);
		int localHeaderPosition = data.getInt();
		writeTimeBlock(data.array(), startOfCentralDirectoryOffset + ZipFile.CENTIM, 0);
		writeTimeBlock(data.array(), localHeaderPosition + ZipFile.LOCTIM, 0);
		File zerotimedFile = new File(this.tempDir, "zerotimed.zip");
		Files.write(zerotimedFile.toPath(), data.array());
		return zerotimedFile;
	}

	@Test
	void getInfoReturnsComputedInfo() {
		ZipInfo info = this.zipContent.getInfo(ZipInfo.class, ZipInfo::get);
		assertThat(info.size()).isEqualTo(12);
	}

	private static void writeTimeBlock(byte[] data, int pos, int value) {
		data[pos] = (byte) (value & 0xff);
		data[pos + 1] = (byte) ((value >> 8) & 0xff);
		data[pos + 2] = (byte) ((value >> 16) & 0xff);
		data[pos + 3] = (byte) ((value >> 24) & 0xff);
	}

	private InputStream asInflaterInputStream(DataBlock dataBlock) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate((int) dataBlock.size() + 1);
		buffer.limit(buffer.limit() - 1);
		dataBlock.readFully(buffer, 0);
		ByteArrayInputStream in = new ByteArrayInputStream(buffer.array());
		return new InflaterInputStream(in, new Inflater(true));
	}

	private void write(File file, CloseableDataBlock dataBlock) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate((int) dataBlock.size());
		dataBlock.readFully(buffer, 0);
		Files.write(file.toPath(), buffer.array());
		dataBlock.close();
	}

	private static class ZipInfo {

		private int size;

		ZipInfo(int size) {
			this.size = size;
		}

		int size() {
			return this.size;
		}

		static ZipInfo get(ZipContent content) {
			return new ZipInfo(content.size());
		}

	}

}
