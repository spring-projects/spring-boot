/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.IOException;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * A ZIP File "End of central directory record" (EOCD).
 * 
 * @author Phillip Webb
 * @see <a href="http://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
 */
class CentralDirectoryEndRecord {

	private static final int MINIMUM_SIZE = 22;

	private static final int MAXIMUM_COMMENT_LENGTH = 0xFFFF;

	private static final int MAXIMUM_SIZE = MINIMUM_SIZE + MAXIMUM_COMMENT_LENGTH;

	private static final int SIGNATURE = 0x06054b50;

	private static final int COMMENT_LENGTH_OFFSET = 20;

	private static final int READ_BLOCK_SIZE = 256;

	private byte[] block;

	private int offset;

	private int size;

	/**
	 * Create a new {@link CentralDirectoryEndRecord} instance from the specified
	 * {@link RandomAccessData}, searching backwards from the end until a valid block is
	 * located.
	 * @param data the source data
	 * @throws IOException
	 */
	public CentralDirectoryEndRecord(RandomAccessData data) throws IOException {
		this.block = createBlockFromEndOfData(data, READ_BLOCK_SIZE);
		this.size = MINIMUM_SIZE;
		this.offset = this.block.length - this.size;
		while (!isValid()) {
			this.size++;
			if (this.size > this.block.length) {
				if (this.size >= MAXIMUM_SIZE || this.size > data.getSize()) {
					throw new IOException("Unable to find ZIP central directory "
							+ "records after reading " + this.size + " bytes");
				}
				this.block = createBlockFromEndOfData(data, this.size + READ_BLOCK_SIZE);
			}
			this.offset = this.block.length - this.size;
		}
	}

	private byte[] createBlockFromEndOfData(RandomAccessData data, int size)
			throws IOException {
		int length = (int) Math.min(data.getSize(), size);
		return Bytes.get(data.getSubsection(data.getSize() - length, length));
	}

	private boolean isValid() {
		if (this.block.length < MINIMUM_SIZE
				|| Bytes.littleEndianValue(this.block, this.offset + 0, 4) != SIGNATURE) {
			return false;
		}
		// Total size must be the structure size + comment
		long commentLength = Bytes.littleEndianValue(this.block, this.offset
				+ COMMENT_LENGTH_OFFSET, 2);
		return this.size == MINIMUM_SIZE + commentLength;
	}

	/**
	 * Return the bytes of the "Central directory" based on the offset indicated in this
	 * record.
	 * @param data the source data
	 * @return the central directory data
	 */
	public RandomAccessData getCentralDirectory(RandomAccessData data) {
		long offset = Bytes.littleEndianValue(this.block, this.offset + 16, 4);
		long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
		return data.getSubsection(offset, length);
	}

	/**
	 * Return the number of ZIP entries in the file.
	 */
	public int getNumberOfRecords() {
		return (int) Bytes.littleEndianValue(this.block, this.offset + 10, 2);
	}
}
