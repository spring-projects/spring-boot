/*
 * Copyright 2012-present the original author or authors.
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

	ConnectionOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}

	void writeHttp(InputStream content, String contentType) throws IOException {
		byte[] bytes = FileCopyUtils.copyToByteArray(content);
		writeHeaders("HTTP/1.1 200 OK", "Content-Type: " + contentType, "Content-Length: " + bytes.length,
				"Connection: close");
		write(bytes);
		flush();
	}

	void writeHeaders(String... headers) throws IOException {
		StringBuilder response = new StringBuilder();
		for (String header : headers) {
			response.append(header).append("\r\n");
		}
		response.append("\r\n");
		write(response.toString().getBytes());
	}

}
