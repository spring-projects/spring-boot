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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link ZipCentralDirectoryFileHeaderRecord}.
 *
 * @author Phillip Webb
 */
class ZipCentralDirectoryFileHeaderRecordTests {

	@Test
	void loadLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x01, 0x02, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, 0x00, 0x00, //
				0x0A, 0x00, //
				0x0B, 0x00, //
				0x0C, 0x00, //
				0x0D, 0x00, //
				0x0E, 0x00, //
				0x0F, 0x00, 0x00, 0x00, //
				0x10, 0x00, 0x00, 0x00 }); //
		ZipCentralDirectoryFileHeaderRecord record = ZipCentralDirectoryFileHeaderRecord.load(dataBlock, 0);
		assertThat(record.versionMadeBy()).isEqualTo((short) 1);
		assertThat(record.versionNeededToExtract()).isEqualTo((short) 2);
		assertThat(record.generalPurposeBitFlag()).isEqualTo((short) 3);
		assertThat(record.compressionMethod()).isEqualTo((short) 4);
		assertThat(record.lastModFileTime()).isEqualTo((short) 5);
		assertThat(record.lastModFileDate()).isEqualTo((short) 6);
		assertThat(record.crc32()).isEqualTo(7);
		assertThat(record.compressedSize()).isEqualTo(8);
		assertThat(record.uncompressedSize()).isEqualTo(9);
		assertThat(record.fileNameLength()).isEqualTo((short) 10);
		assertThat(record.extraFieldLength()).isEqualTo((short) 11);
		assertThat(record.fileCommentLength()).isEqualTo((short) 12);
		assertThat(record.diskNumberStart()).isEqualTo((short) 13);
		assertThat(record.internalFileAttributes()).isEqualTo((short) 14);
		assertThat(record.externalFileAttributes()).isEqualTo(15);
		assertThat(record.offsetToLocalHeader()).isEqualTo(16);
	}

	@Test
	void loadWhenSignatureDoesNotMatchThrowsException() {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x51, 0x4b, 0x01, 0x02, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, 0x00, 0x00, //
				0x0A, 0x00, //
				0x0B, 0x00, //
				0x0C, 0x00, //
				0x0D, 0x00, //
				0x0E, 0x00, //
				0x0F, 0x00, 0x00, 0x00, //
				0x10, 0x00, 0x00, 0x00 }); //
		assertThatIOException().isThrownBy(() -> ZipCentralDirectoryFileHeaderRecord.load(dataBlock, 0))
			.withMessageContaining("'Central Directory File Header Record' not found");
	}

	@Test
	void sizeReturnsSize() {
		ZipCentralDirectoryFileHeaderRecord record = new ZipCentralDirectoryFileHeaderRecord((short) 1, (short) 2,
				(short) 3, (short) 4, (short) 5, (short) 6, 7, 8, 9, (short) 10, (short) 11, (short) 12, (short) 13,
				(short) 14, 15, 16);
		assertThat(record.size()).isEqualTo(79L);
	}

	@Test
	void copyToCopiesDataToZipEntry() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x01, 0x02, //
				0x00, 0x00, //
				0x00, 0x00, //
				0x00, 0x00, //
				0x08, 0x00, //
				0x23, 0x74, //
				0x58, 0x36, //
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, //
				0x01, 0x00, //
				0x01, 0x00, //
				0x01, 0x00, //
				0x00, 0x00, //
				0x00, 0x00, //
				0x00, 0x00, 0x00, 0x00, //
				0x00, 0x00, 0x00, 0x00, //
				0x61, //
				0x62, //
				0x63 }); //
		ZipCentralDirectoryFileHeaderRecord record = ZipCentralDirectoryFileHeaderRecord.load(dataBlock, 0);
		ZipEntry entry = new ZipEntry("");
		record.copyTo(dataBlock, 0, entry);
		assertThat(entry.getMethod()).isEqualTo(ZipEntry.DEFLATED);
		assertThat(entry.getTimeLocal()).hasYear(2007);
		ZonedDateTime expectedTime = ZonedDateTime.of(2007, 02, 24, 14, 33, 06, 0, ZoneId.systemDefault());
		assertThat(entry.getTime()).isEqualTo(expectedTime.toEpochSecond() * 1000);
		assertThat(entry.getCrc()).isEqualTo(0xFFFFFFFFL);
		assertThat(entry.getCompressedSize()).isEqualTo(1);
		assertThat(entry.getSize()).isEqualTo(2);
		assertThat(entry.getExtra()).containsExactly(0x62);
		assertThat(entry.getComment()).isEqualTo("c");
	}

	@Test
	void withFileNameLengthReturnsUpdatedInstance() {
		ZipCentralDirectoryFileHeaderRecord record = new ZipCentralDirectoryFileHeaderRecord((short) 1, (short) 2,
				(short) 3, (short) 4, (short) 5, (short) 6, 7, 8, 9, (short) 10, (short) 11, (short) 12, (short) 13,
				(short) 14, 15, 16)
			.withFileNameLength((short) 100);
		assertThat(record.versionMadeBy()).isEqualTo((short) 1);
		assertThat(record.versionNeededToExtract()).isEqualTo((short) 2);
		assertThat(record.generalPurposeBitFlag()).isEqualTo((short) 3);
		assertThat(record.compressionMethod()).isEqualTo((short) 4);
		assertThat(record.lastModFileTime()).isEqualTo((short) 5);
		assertThat(record.lastModFileDate()).isEqualTo((short) 6);
		assertThat(record.crc32()).isEqualTo(7);
		assertThat(record.compressedSize()).isEqualTo(8);
		assertThat(record.uncompressedSize()).isEqualTo(9);
		assertThat(record.fileNameLength()).isEqualTo((short) 100);
		assertThat(record.extraFieldLength()).isEqualTo((short) 11);
		assertThat(record.fileCommentLength()).isEqualTo((short) 12);
		assertThat(record.diskNumberStart()).isEqualTo((short) 13);
		assertThat(record.internalFileAttributes()).isEqualTo((short) 14);
		assertThat(record.externalFileAttributes()).isEqualTo(15);
		assertThat(record.offsetToLocalHeader()).isEqualTo(16);
	}

	@Test
	void withOffsetToLocalHeaderReturnsUpdatedInstance() {
		ZipCentralDirectoryFileHeaderRecord record = new ZipCentralDirectoryFileHeaderRecord((short) 1, (short) 2,
				(short) 3, (short) 4, (short) 5, (short) 6, 7, 8, 9, (short) 10, (short) 11, (short) 12, (short) 13,
				(short) 14, 15, 16)
			.withOffsetToLocalHeader(100);
		assertThat(record.versionMadeBy()).isEqualTo((short) 1);
		assertThat(record.versionNeededToExtract()).isEqualTo((short) 2);
		assertThat(record.generalPurposeBitFlag()).isEqualTo((short) 3);
		assertThat(record.compressionMethod()).isEqualTo((short) 4);
		assertThat(record.lastModFileTime()).isEqualTo((short) 5);
		assertThat(record.lastModFileDate()).isEqualTo((short) 6);
		assertThat(record.crc32()).isEqualTo(7);
		assertThat(record.compressedSize()).isEqualTo(8);
		assertThat(record.uncompressedSize()).isEqualTo(9);
		assertThat(record.fileNameLength()).isEqualTo((short) 10);
		assertThat(record.extraFieldLength()).isEqualTo((short) 11);
		assertThat(record.fileCommentLength()).isEqualTo((short) 12);
		assertThat(record.diskNumberStart()).isEqualTo((short) 13);
		assertThat(record.internalFileAttributes()).isEqualTo((short) 14);
		assertThat(record.externalFileAttributes()).isEqualTo(15);
		assertThat(record.offsetToLocalHeader()).isEqualTo(100);
	}

	@Test
	void asByteArrayReturnsByteArray() throws Exception {
		byte[] bytes = new byte[] { //
				0x50, 0x4b, 0x01, 0x02, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, 0x00, 0x00, //
				0x0A, 0x00, //
				0x0B, 0x00, //
				0x0C, 0x00, //
				0x0D, 0x00, //
				0x0E, 0x00, //
				0x0F, 0x00, 0x00, 0x00, //
				0x10, 0x00, 0x00, 0x00 };
		DataBlock dataBlock = new ByteArrayDataBlock(bytes);
		ZipCentralDirectoryFileHeaderRecord record = ZipCentralDirectoryFileHeaderRecord.load(dataBlock, 0);
		assertThat(record.asByteArray()).containsExactly(bytes);
	}

}
