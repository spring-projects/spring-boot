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

	public ConnectionOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}

	public void writeHttp(InputStream content, String contentType) throws IOException {
		byte[] bytes = FileCopyUtils.copyToByteArray(content);
		writeHeaders("HTTP/1.1 200 OK", "Content-Type: " + contentType,
				"Content-Length: " + bytes.length, "Connection: close");
		write(bytes);
		flush();
	}

	public void writeHeaders(String... headers) throws IOException {
		StringBuilder response = new StringBuilder();
		for (String header : headers) {
			response.append(header).append("\r\n");
		}
		response.append("\r\n");
		write(response.toString().getBytes());
	}

}
