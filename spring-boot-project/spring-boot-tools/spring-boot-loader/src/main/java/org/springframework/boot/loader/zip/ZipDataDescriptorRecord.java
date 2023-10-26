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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * A ZIP File "Data Descriptor" record.
 *
 * @param includeSignature if the signature bytes are written or not (see note in spec)
 * @param crc32 the CRC32 checksum
 * @param compressedSize the size of the entry when compressed
 * @param uncompressedSize the size of the entry when uncompressed
 * @author Phillip Webb
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.9 of the Zip File Format Specification</a>
 */
record ZipDataDescriptorRecord(boolean includeSignature, int crc32, int compressedSize, int uncompressedSize) {

	private static final DebugLogger debug = DebugLogger.get(ZipDataDescriptorRecord.class);

	private static final int SIGNATURE = 0x08074b50;

	private static final int DATA_SIZE = 12;

	private static final int SIGNATURE_SIZE = 4;

	long size() {
		return (!includeSignature()) ? DATA_SIZE : DATA_SIZE + SIGNATURE_SIZE;
	}

	/**
	 * Return the contents of this record as a byte array suitable for writing to a zip.
	 * @return the record as a byte array
	 */
	byte[] asByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate((int) size());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		if (this.includeSignature) {
			buffer.putInt(SIGNATURE);
		}
		buffer.putInt(this.crc32);
		buffer.putInt(this.compressedSize);
		buffer.putInt(this.uncompressedSize);
		return buffer.array();
	}

	/**
	 * Load the {@link ZipDataDescriptorRecord} from the given data block.
	 * @param dataBlock the source data block
	 * @param pos the position of the record
	 * @return a new {@link ZipLocalFileHeaderRecord} instance
	 * @throws IOException on I/O error
	 */
	static ZipDataDescriptorRecord load(DataBlock dataBlock, long pos) throws IOException {
		debug.log("Loading ZipDataDescriptorRecord from position %s", pos);
		ByteBuffer buffer = ByteBuffer.allocate(SIGNATURE_SIZE + DATA_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.limit(SIGNATURE_SIZE);
		dataBlock.readFully(buffer, pos);
		buffer.rewind();
		int signatureOrCrc = buffer.getInt();
		boolean hasSignature = (signatureOrCrc == SIGNATURE);
		buffer.rewind();
		buffer.limit((!hasSignature) ? DATA_SIZE - SIGNATURE_SIZE : DATA_SIZE);
		dataBlock.readFully(buffer, pos + SIGNATURE_SIZE);
		buffer.rewind();
		return new ZipDataDescriptorRecord(hasSignature, (!hasSignature) ? signatureOrCrc : buffer.getInt(),
				buffer.getInt(), buffer.getInt());
	}

	/**
	 * Return if the {@link ZipDataDescriptorRecord} is present based on the general
	 * purpose bit flag in the given {@link ZipLocalFileHeaderRecord}.
	 * @param localRecord the local record to check
	 * @return if the bit flag is set
	 */
	static boolean isPresentBasedOnFlag(ZipLocalFileHeaderRecord localRecord) {
		return isPresentBasedOnFlag(localRecord.generalPurposeBitFlag());
	}

	/**
	 * Return if the {@link ZipDataDescriptorRecord} is present based on the general
	 * purpose bit flag in the given {@link ZipCentralDirectoryFileHeaderRecord}.
	 * @param centralRecord the central record to check
	 * @return if the bit flag is set
	 */
	static boolean isPresentBasedOnFlag(ZipCentralDirectoryFileHeaderRecord centralRecord) {
		return isPresentBasedOnFlag(centralRecord.generalPurposeBitFlag());
	}

	/**
	 * Return if the {@link ZipDataDescriptorRecord} is present based on the given general
	 * purpose bit flag.
	 * @param generalPurposeBitFlag the general purpose bit flag to check
	 * @return if the bit flag is set
	 */
	static boolean isPresentBasedOnFlag(int generalPurposeBitFlag) {
		return (generalPurposeBitFlag & 0b0000_1000) != 0;
	}

}
