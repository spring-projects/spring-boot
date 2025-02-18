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
 * Tests for {@link ZipEndOfCentralDirectoryRecord}.
 *
 * @author Phillip Webb
 */
class ZipEndOfCentralDirectoryRecordTests {

	@Test
	void loadLocatesAndLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x05, 0x06, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00 }); //
		ZipEndOfCentralDirectoryRecord.Located located = ZipEndOfCentralDirectoryRecord.load(dataBlock);
		assertThat(located.pos()).isEqualTo(0L);
		ZipEndOfCentralDirectoryRecord record = located.endOfCentralDirectoryRecord();
		assertThat(record.numberOfThisDisk()).isEqualTo((short) 1);
		assertThat(record.diskWhereCentralDirectoryStarts()).isEqualTo((short) 2);
		assertThat(record.numberOfCentralDirectoryEntriesOnThisDisk()).isEqualTo((short) 3);
		assertThat(record.totalNumberOfCentralDirectoryEntries()).isEqualTo((short) 4);
		assertThat(record.sizeOfCentralDirectory()).isEqualTo(5);
		assertThat(record.offsetToStartOfCentralDirectory()).isEqualTo(6);
		assertThat(record.commentLength()).isEqualTo((short) 7);
	}

	@Test
	void loadWhenMultipleBuffersBackLoadsData() throws Exception {
		byte[] bytes = new byte[ZipEndOfCentralDirectoryRecord.BUFFER_SIZE * 4];
		byte[] data = new byte[] { //
				0x50, 0x4b, 0x05, 0x06, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00 }; //
		System.arraycopy(data, 0, bytes, 4, data.length);
		ZipEndOfCentralDirectoryRecord.Located located = ZipEndOfCentralDirectoryRecord
			.load(new ByteArrayDataBlock(bytes));
		assertThat(located.pos()).isEqualTo(4L);
	}

	@Test
	void loadWhenSignatureDoesNotMatchThrowsException() {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x51, 0x4b, 0x05, 0x06, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00 }); //
		assertThatIOException().isThrownBy(() -> ZipEndOfCentralDirectoryRecord.load(dataBlock))
			.withMessageContaining("'End Of Central Directory Record' not found");
	}

	@Test
	void asByteArrayReturnsByteArray() throws Exception {
		byte[] bytes = new byte[] { //
				0x50, 0x4b, 0x05, 0x06, //
				0x01, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, //
				0x07, 0x00 }; //
		ZipEndOfCentralDirectoryRecord.Located located = ZipEndOfCentralDirectoryRecord
			.load(new ByteArrayDataBlock(bytes));
		assertThat(located.endOfCentralDirectoryRecord().asByteArray()).isEqualTo(bytes);
	}

	@Test
	void sizeReturnsSize() {
		ZipEndOfCentralDirectoryRecord record = new ZipEndOfCentralDirectoryRecord((short) 1, (short) 2, (short) 3,
				(short) 4, 5, 6, (short) 7);
		assertThat(record.size()).isEqualTo(29L);
	}

}
