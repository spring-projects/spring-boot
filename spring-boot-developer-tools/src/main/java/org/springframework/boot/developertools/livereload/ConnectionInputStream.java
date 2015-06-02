/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.developertools.livereload;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} for a server connection.
 *
 * @author Phillip Webb
 */
class ConnectionInputStream extends FilterInputStream {

	private static final String HEADER_END = "\r\n\r\n";

	private static final int BUFFER_SIZE = 4096;

	public ConnectionInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Read the HTTP header from the {@link InputStream}. Note: This method doesn't expect
	 * any HTTP content after the header since the initial request is usually just a
	 * WebSocket upgrade.
	 * @return the HTTP header
	 * @throws IOException
	 */
	public String readHeader() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		StringBuffer content = new StringBuffer(BUFFER_SIZE);
		while (content.indexOf(HEADER_END) == -1) {
			int amountRead = checkedRead(buffer, 0, BUFFER_SIZE);
			content.append(new String(buffer, 0, amountRead));
		}
		return content.substring(0, content.indexOf(HEADER_END)).toString();
	}

	/**
	 * Repeatedly read the underlying {@link InputStream} until the requested number of
	 * bytes have been loaded.
	 * @param buffer the destination buffer
	 * @param offset the buffer offset
	 * @param length the amount of data to read
	 * @throws IOException
	 */
	public void readFully(byte[] buffer, int offset, int length) throws IOException {
		while (length > 0) {
			int amountRead = checkedRead(buffer, offset, length);
			offset += amountRead;
			length -= amountRead;
		}
	}

	/**
	 * Read a single byte from the stream (checking that the end of the stream hasn't been
	 * reached.
	 * @return the content
	 * @throws IOException
	 */
	public int checkedRead() throws IOException {
		int b = read();
		if (b == -1) {
			throw new IOException("End of stream");
		}
		return (b & 0xff);
	}

	/**
	 * Read a a number of bytes from the stream (checking that the end of the stream
	 * hasn't been reached)
	 * @param buffer the destination buffer
	 * @param offset the buffer offset
	 * @param length the length to read
	 * @return the amount of data read
	 * @throws IOException
	 */
	public int checkedRead(byte[] buffer, int offset, int length) throws IOException {
		int amountRead = read(buffer, offset, length);
		if (amountRead == -1) {
			throw new IOException("End of stream");
		}
		return amountRead;
	}

}
