/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link Zip64EndOfCentralDirectoryRecord}.
 *
 * @author Phillip Webb
 */
class Zip64EndOfCentralDirectoryRecordTests {

	@Test
	void loadLoadsData() throws Exception {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x50, 0x4b, 0x06, 0x06, //
				0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, 0x00, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); //
		Zip64EndOfCentralDirectoryLocator locator = new Zip64EndOfCentralDirectoryLocator(56, 0, 0, 0);
		Zip64EndOfCentralDirectoryRecord eocd = Zip64EndOfCentralDirectoryRecord.load(dataBlock, locator);
		assertThat(eocd.size()).isEqualTo(56);
		assertThat(eocd.sizeOfZip64EndOfCentralDirectoryRecord()).isEqualTo(1);
		assertThat(eocd.versionMadeBy()).isEqualTo((short) 2);
		assertThat(eocd.versionNeededToExtract()).isEqualTo((short) 3);
		assertThat(eocd.numberOfThisDisk()).isEqualTo(4);
		assertThat(eocd.diskWhereCentralDirectoryStarts()).isEqualTo(5);
		assertThat(eocd.numberOfCentralDirectoryEntriesOnThisDisk()).isEqualTo(6);
		assertThat(eocd.totalNumberOfCentralDirectoryEntries()).isEqualTo(7);
		assertThat(eocd.sizeOfCentralDirectory()).isEqualTo(8);
		assertThat(eocd.offsetToStartOfCentralDirectory()).isEqualTo(9);
	}

	@Test
	void loadWhenSignatureDoesNotMatchThrowsException() {
		DataBlock dataBlock = new ByteArrayDataBlock(new byte[] { //
				0x51, 0x4b, 0x06, 0x06, //
				0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x02, 0x00, //
				0x03, 0x00, //
				0x04, 0x00, 0x00, 0x00, //
				0x05, 0x00, 0x00, 0x00, //
				0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //
				0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); //
		Zip64EndOfCentralDirectoryLocator locator = new Zip64EndOfCentralDirectoryLocator(56, 0, 0, 0);
		assertThatIOException().isThrownBy(() -> Zip64EndOfCentralDirectoryRecord.load(dataBlock, locator))
			.withMessageContaining("Zip64 'End Of Central Directory Record' not found at position");
	}

}
