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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link ZipLocalFileHeaderRecord}.
 *
 * @author Phillip Webb
 */
class ZipLocalFileHeaderRecordTests {

	@Test
	void loadLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x03, 0x04, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, //
				0x0A, 0x00 }); //
		ZipLocalFileHeaderRecord record = ZipLocalFileHeaderRecord.load(dataBlock, 0);
		assertThat(record.versionNeededToExtract()).isEqualTo((short) 1);
		assertThat(record.generalPurposeBitFlag()).isEqualTo((short) 2);
		assertThat(record.compressionMethod()).isEqualTo((short) 3);
		assertThat(record.lastModFileTime()).isEqualTo((short) 4);
		assertThat(record.lastModFileDate()).isEqualTo((short) 5);
		assertThat(record.crc32()).isEqualTo(6);
		assertThat(record.compressedSize()).isEqualTo(7);
		assertThat(record.uncompressedSize()).isEqualTo(8);
		assertThat(record.fileNameLength()).isEqualTo((short) 9);
		assertThat(record.extraFieldLength()).isEqualTo((short) 10);
	}

	@Test
	void loadWhenSignatureDoesNotMatchThrowsException() {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x51, 0x4b, 0x03, 0x04, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, //
				0x0A, 0x00 }); //
		assertThatIOException().isThrownBy(() -> ZipLocalFileHeaderRecord.load(dataBlock, 0))
			.withMessageContaining("'Local File Header Record' not found");
	}

	@Test
	void sizeReturnsSize() {
		ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
				(short) 5, 6, 7, 8, (short) 9, (short) 10);
		assertThat(record.size()).isEqualTo(49L);
	}

	@Test
	void withExtraFieldLengthReturnsUpdatedInstance() {
		ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
				(short) 5, 6, 7, 8, (short) 9, (short) 10)
			.withExtraFieldLength((short) 100);
		assertThat(record.extraFieldLength()).isEqualTo((short) 100);
	}

	@Test
	void withFileNameLengthReturnsUpdatedInstance() {
		ZipLocalFileHeaderRecord record = new ZipLocalFileHeaderRecord((short) 1, (short) 2, (short) 3, (short) 4,
				(short) 5, 6, 7, 8, (short) 9, (short) 10)
			.withFileNameLength((short) 100);
		assertThat(record.fileNameLength()).isEqualTo((short) 100);
	}

	@Test
	void asByteArrayReturnsByteArray() throws Exception {
		byte[] bytes = new byte[] { //
				0x50, 0x4b, 0x03, 0x04, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, //
				0x09, 0x00, //
				0x0A, 0x00 }; //
		ZipLocalFileHeaderRecord record = ZipLocalFileHeaderRecord.load(new ByteArrayDataBlock(bytes), 0);
		assertThat(record.asByteArray()).isEqualTo(bytes);
	}

}
