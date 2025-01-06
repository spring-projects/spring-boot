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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Zip64EndOfCentralDirectoryLocator}.
 *
 * @author Phillip Webb
 */
class Zip64EndOfCentralDirectoryLocatorTests {

	@Test
	void findReturnsRecord() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x06, 0x07, //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }); //
		Zip64EndOfCentralDirectoryLocator eocd = Zip64EndOfCentralDirectoryLocator.find(dataBlock, 20);
		assertThat(eocd.pos()).isEqualTo(0);
		assertThat(eocd.numberOfThisDisk()).isEqualTo(1);
		assertThat(eocd.offsetToZip64EndOfCentralDirectoryRecord()).isEqualTo(2);
		assertThat(eocd.totalNumberOfDisks()).isEqualTo(3);
	}

	@Test
	void findWhenSignatureDoesNotMatchReturnsNull() throws IOException {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x51, 0x4b, 0x06, 0x07, //
				0x01, 0x00, 0x00, 0x00, //
				0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x03, 0x00, 0x00, 0x00 }); //
		Zip64EndOfCentralDirectoryLocator eocd = Zip64EndOfCentralDirectoryLocator.find(dataBlock, 20);
		assertThat(eocd).isNull();

	}

}
