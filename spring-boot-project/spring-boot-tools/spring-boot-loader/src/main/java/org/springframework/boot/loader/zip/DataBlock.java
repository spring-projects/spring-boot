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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Provides read access to a block of data contained somewhere in a zip file.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public interface DataBlock {

	/**
	 * Return the size of this block.
	 * @return the block size
	 * @throws IOException on I/O error
	 */
	long size() throws IOException;

	/**
	 * Read a sequence of bytes from this channel into the given buffer, starting at the
	 * given block position.
	 * @param dst the buffer into which bytes are to be transferred
	 * @param pos the position within the block at which the transfer is to begin
	 * @return the number of bytes read, possibly zero, or {@code -1} if the given
	 * position is greater than or equal to the block size
	 * @throws IOException on I/O error
	 * @see #readFully(ByteBuffer, long)
	 * @see FileChannel#read(ByteBuffer, long)
	 */
	int read(ByteBuffer dst, long pos) throws IOException;

	/**
	 * Fully read a sequence of bytes from this channel into the given buffer, starting at
	 * the given block position and filling {@link ByteBuffer#remaining() remaining} bytes
	 * in the buffer.
	 * @param dst the buffer into which bytes are to be transferred
	 * @param pos the position within the block at which the transfer is to begin
	 * @throws EOFException if an attempt is made to read past the end of the block
	 * @throws IOException on I/O error
	 */
	default void readFully(ByteBuffer dst, long pos) throws IOException {
		do {
			int count = read(dst, pos);
			if (count <= 0) {
				throw new EOFException();
			}
			pos += count;
		}
		while (dst.hasRemaining());
	}

	/**
	 * Return this {@link DataBlock} as an {@link InputStream}.
	 * @return an {@link InputStream} to read the data block content
	 * @throws IOException on IO error
	 */
	default InputStream asInputStream() throws IOException {
		return new DataBlockInputStream(this);
	}

}
