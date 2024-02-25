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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@link InputStream} backed by a {@link DataBlock}.
 *
 * @author Phillip Webb
 */
class DataBlockInputStream extends InputStream {

	private final DataBlock dataBlock;

	private long pos;

	private long remaining;

	private volatile boolean closed;

	/**
	 * Constructs a new DataBlockInputStream with the specified DataBlock.
	 * @param dataBlock the DataBlock to be used by the input stream
	 * @throws IOException if an I/O error occurs
	 */
	DataBlockInputStream(DataBlock dataBlock) throws IOException {
		this.dataBlock = dataBlock;
		this.remaining = dataBlock.size();
	}

	/**
	 * Reads a single byte of data from the input stream.
	 * @return the byte read, or -1 if the end of the stream is reached
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
	}

	/**
	 * Reads up to len bytes of data from the data block into the specified byte array.
	 * @param b the byte array to read the data into
	 * @param off the starting offset in the byte array
	 * @param len the maximum number of bytes to read
	 * @return the total number of bytes read into the byte array, or -1 if there is no
	 * more data because the end of the data block has been reached
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		ByteBuffer dst = ByteBuffer.wrap(b, off, len);
		int count = this.dataBlock.read(dst, this.pos);
		if (count > 0) {
			this.pos += count;
			this.remaining -= count;
		}
		return count;
	}

	/**
	 * Skips over and discards a specified number of bytes from the input stream.
	 * @param n the number of bytes to be skipped
	 * @return the actual number of bytes skipped
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public long skip(long n) throws IOException {
		long count = (n > 0) ? maxForwardSkip(n) : maxBackwardSkip(n);
		this.pos += count;
		this.remaining -= count;
		return count;
	}

	/**
	 * Returns the maximum number of bytes that can be skipped forward in the data block.
	 * @param n the number of bytes to skip forward
	 * @return the maximum number of bytes that can be skipped forward without causing an
	 * overflow or exceeding the remaining bytes in the data block
	 */
	private long maxForwardSkip(long n) {
		boolean willCauseOverflow = (this.pos + n) < 0;
		return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
	}

	/**
	 * Returns the maximum number of bytes that can be skipped backwards from the current
	 * position.
	 * @param n the number of bytes to skip backwards
	 * @return the maximum number of bytes that can be skipped backwards
	 */
	private long maxBackwardSkip(long n) {
		return Math.max(-this.pos, n);
	}

	/**
	 * Returns the number of bytes that can be read from this DataBlockInputStream without
	 * blocking. If the stream is closed, it returns 0. If the remaining bytes is less
	 * than Integer.MAX_VALUE, it returns the remaining bytes. If the remaining bytes is
	 * greater than or equal to Integer.MAX_VALUE, it returns Integer.MAX_VALUE.
	 * @return the number of bytes that can be read from this DataBlockInputStream without
	 * blocking
	 */
	@Override
	public int available() {
		if (this.closed) {
			return 0;
		}
		return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
	}

	/**
	 * Ensures that the input stream is open.
	 * @throws IOException if the input stream is closed
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("InputStream closed");
		}
	}

	/**
	 * Closes the DataBlockInputStream.
	 * @throws IOException if an I/O error occurs while closing the stream
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		if (this.dataBlock instanceof Closeable closeable) {
			closeable.close();
		}
	}

}
