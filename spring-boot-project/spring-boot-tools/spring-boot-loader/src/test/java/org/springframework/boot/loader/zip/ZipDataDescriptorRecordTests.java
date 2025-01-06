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

/**
 * Tests for {@link ZipDataDescriptorRecord}.
 *
 * @author Phillip Webb
 */
class ZipDataDescriptorRecordTests {

	private static final short S0 = 0;

	@Test
	void loadWhenHasSignatureLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x07, 0x08, //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }); //
		ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(dataBlock, 0);
		assertThat(record.includeSignature()).isTrue();
		assertThat(record.crc32()).isEqualTo(1);
		assertThat(record.compressedSize()).isEqualTo(2);
		assertThat(record.uncompressedSize()).isEqualTo(3);
	}

	@Test
	void loadWhenHasNoSignatureLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }); //
		ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(dataBlock, 0);
		assertThat(record.includeSignature()).isFalse();
		assertThat(record.crc32()).isEqualTo(1);
		assertThat(record.compressedSize()).isEqualTo(2);
		assertThat(record.uncompressedSize()).isEqualTo(3);
	}

	@Test
	void sizeWhenIncludeSignatureReturnsSize() {
		ZipDataDescriptorRecord record = new ZipDataDescriptorRecord(true, 0, 0, 0);
		assertThat(record.size()).isEqualTo(16);
	}

	@Test
	void sizeWhenNotIncludeSignatureReturnsSize() {
		ZipDataDescriptorRecord record = new ZipDataDescriptorRecord(false, 0, 0, 0);
		assertThat(record.size()).isEqualTo(12);
	}

	@Test
	void asByteArrayWhenIncludeSignatureReturnsByteArray() throws Exception {
		byte[] bytes = new byte[] { //
				0x50, 0x4b, 0x07, 0x08, //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }; //
		ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(new ByteArrayDataBlock(bytes), 0);
		assertThat(record.asByteArray()).isEqualTo(bytes);
	}

	@Test
	void asByteArrayWhenNotIncludeSignatureReturnsByteArray() throws Exception {
		byte[] bytes = new byte[] { //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }; //
		ZipDataDescriptorRecord record = ZipDataDescriptorRecord.load(new ByteArrayDataBlock(bytes), 0);
		assertThat(record.asByteArray()).isEqualTo(bytes);
	}

	@Test
	void isPresentBasedOnFlagWhenPresentReturnsTrue() {
		testIsPresentBasedOnFlag((short) 0x8, true);
	}

	@Test
	void isPresentBasedOnFlagWhenNotPresentReturnsFalse() {
		testIsPresentBasedOnFlag((short) 0x0, false);
	}

	private void testIsPresentBasedOnFlag(short flag, boolean expected) {
		ZipCentralDirectoryFileHeaderRecord centralRecord = new ZipCentralDirectoryFileHeaderRecord(S0, S0, flag, S0,
				S0, S0, S0, S0, S0, S0, S0, S0, S0, S0, S0, S0);
		ZipLocalFileHeaderRecord localRecord = new ZipLocalFileHeaderRecord(S0, flag, S0, S0, S0, S0, S0, S0, S0, S0);
		assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(flag)).isEqualTo(expected);
		assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(centralRecord)).isEqualTo(expected);
		assertThat(ZipDataDescriptorRecord.isPresentBasedOnFlag(localRecord)).isEqualTo(expected);
	}

}
