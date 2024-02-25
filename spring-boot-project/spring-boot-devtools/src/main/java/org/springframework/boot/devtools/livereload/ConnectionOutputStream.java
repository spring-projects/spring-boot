/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.livereload;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.util.FileCopyUtils;

/**
 * {@link OutputStream} for a server connection.
 *
 * @author Phillip Webb
 */
class ConnectionOutputStream extends FilterOutputStream {

	/**
     * Constructs a new ConnectionOutputStream object with the specified OutputStream.
     * 
     * @param out the OutputStream to be used for writing data
     */
    ConnectionOutputStream(OutputStream out) {
		super(out);
	}

	/**
     * Writes a specified number of bytes from the specified byte array starting at the specified offset to the underlying output stream.
     * 
     * @param b the byte array containing the data to be written
     * @param off the starting offset in the byte array
     * @param len the number of bytes to be written
     * @throws IOException if an I/O error occurs while writing to the output stream
     */
    @Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}

	/**
     * Writes the HTTP response with the provided content and content type.
     * 
     * @param content     the input stream containing the content to be written
     * @param contentType the content type of the response
     * @throws IOException if an I/O error occurs while writing the response
     */
    void writeHttp(InputStream content, String contentType) throws IOException {
		byte[] bytes = FileCopyUtils.copyToByteArray(content);
		writeHeaders("HTTP/1.1 200 OK", "Content-Type: " + contentType, "Content-Length: " + bytes.length,
				"Connection: close");
		write(bytes);
		flush();
	}

	/**
     * Writes the given headers to the output stream.
     * 
     * @param headers the headers to be written
     * @throws IOException if an I/O error occurs
     */
    void writeHeaders(String... headers) throws IOException {
		StringBuilder response = new StringBuilder();
		for (String header : headers) {
			response.append(header).append("\r\n");
		}
		response.append("\r\n");
		write(response.toString().getBytes());
	}

}
