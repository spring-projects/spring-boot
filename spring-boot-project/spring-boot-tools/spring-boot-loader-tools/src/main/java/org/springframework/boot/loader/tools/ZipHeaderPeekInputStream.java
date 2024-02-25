/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * {@link InputStream} that can peek ahead at zip header bytes.
 *
 * @author Phillip Webb
 */
class ZipHeaderPeekInputStream extends FilterInputStream {

	private static final byte[] ZIP_HEADER = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

	private final byte[] header;

	private final int headerLength;

	private int position;

	private ByteArrayInputStream headerStream;

	/**
	 * Constructs a new ZipHeaderPeekInputStream with the specified InputStream.
	 * @param in the InputStream to read from
	 * @throws IOException if an I/O error occurs while reading the InputStream
	 */
	protected ZipHeaderPeekInputStream(InputStream in) throws IOException {
		super(in);
		this.header = new byte[4];
		this.headerLength = in.read(this.header);
		this.headerStream = new ByteArrayInputStream(this.header, 0, this.headerLength);
	}

	/**
	 * Reads the next byte of data from the input stream. If the header stream is not
	 * null, it reads from the header stream. If the header stream is null or the end of
	 * the header stream is reached, it reads from the super class input stream.
	 * @return the next byte of data, or -1 if the end of the stream is reached
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read() throws IOException {
		int read = (this.headerStream != null) ? this.headerStream.read() : -1;
		if (read != -1) {
			this.position++;
			if (this.position >= this.headerLength) {
				this.headerStream = null;
			}
			return read;
		}
		return super.read();
	}

	/**
	 * Reads up to {@code b.length} bytes of data from this input stream into an array of
	 * bytes. This method blocks until at least one byte of input is available.
	 * @param b the buffer into which the data is read
	 * @return the total number of bytes read into the buffer, or -1 if there is no more
	 * data because the end of the stream has been reached
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Reads up to len bytes of data from the input stream into an array of bytes.
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in the buffer at which the data is written.
	 * @param len the maximum number of bytes to read.
	 * @return the total number of bytes read into the buffer, or -1 if there is no more
	 * data because the end of the stream has been reached.
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = (this.headerStream != null) ? this.headerStream.read(b, off, len) : -1;
		if (read <= 0) {
			return readRemainder(b, off, len);
		}
		this.position += read;
		if (read < len) {
			int remainderRead = readRemainder(b, off + read, len - read);
			if (remainderRead > 0) {
				read += remainderRead;
			}
		}
		if (this.position >= this.headerLength) {
			this.headerStream = null;
		}
		return read;
	}

	/**
	 * Checks if the input stream has a ZIP header.
	 * @return true if the input stream has a ZIP header, false otherwise.
	 */
	boolean hasZipHeader() {
		return Arrays.equals(this.header, ZIP_HEADER);
	}

	/**
	 * Reads the remaining bytes from the input stream into the specified byte array.
	 * @param b the byte array to read the data into
	 * @param off the starting offset in the byte array
	 * @param len the maximum number of bytes to read
	 * @return the total number of bytes read into the byte array, or -1 if there is no
	 * more data because the end of the stream has been reached
	 * @throws IOException if an I/O error occurs while reading from the input stream
	 */
	private int readRemainder(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read > 0) {
			this.position += read;
		}
		return read;
	}

}
