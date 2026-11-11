/*
 * Copyright 2012-present the original author or authors.
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
