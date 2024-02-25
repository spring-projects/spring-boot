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

package org.springframework.boot.loader.jar;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * {@link InflaterInputStream} that supports the writing of an extra "dummy" byte (which
 * is required when using an {@link Inflater} with {@code nowrap}) and returns accurate
 * available() results.
 *
 * @author Phillip Webb
 */
abstract class ZipInflaterInputStream extends InflaterInputStream {

	private int available;

	private boolean extraBytesWritten;

	/**
     * Constructs a new ZipInflaterInputStream with the specified input stream, inflater, and size.
     * 
     * @param inputStream the input stream to be read from
     * @param inflater the inflater to be used for decompression
     * @param size the size of the input stream
     */
    ZipInflaterInputStream(InputStream inputStream, Inflater inflater, int size) {
		super(inputStream, inflater, getInflaterBufferSize(size));
		this.available = size;
	}

	/**
     * Returns the buffer size for the inflater based on the given size.
     * 
     * @param size the size of the buffer
     * @return the buffer size for the inflater
     */
    private static int getInflaterBufferSize(long size) {
		size += 2; // inflater likes some space
		size = (size > 65536) ? 8192 : size;
		size = (size <= 0) ? 4096 : size;
		return (int) size;
	}

	/**
     * Returns the number of bytes that can be read from the current input stream without blocking.
     * 
     * @return the number of bytes that can be read from the current input stream without blocking,
     *         or -1 if the number of bytes is unknown.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public int available() throws IOException {
		return (this.available >= 0) ? this.available : super.available();
	}

	/**
     * Reads up to len bytes of data from the input stream into an array of bytes.
     * 
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array b.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = super.read(b, off, len);
		if (result != -1) {
			this.available -= result;
		}
		return result;
	}

	/**
     * Fills the input buffer with more data to be decompressed.
     * 
     * @throws IOException if an I/O error occurs.
     */
    @Override
	protected void fill() throws IOException {
		try {
			super.fill();
		}
		catch (EOFException ex) {
			if (this.extraBytesWritten) {
				throw ex;
			}
			this.len = 1;
			this.buf[0] = 0x0;
			this.extraBytesWritten = true;
			this.inf.setInput(this.buf, 0, this.len);
		}
	}

}
