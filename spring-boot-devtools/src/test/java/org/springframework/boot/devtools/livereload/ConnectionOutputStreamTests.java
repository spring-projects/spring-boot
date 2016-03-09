/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConnectionOutputStream}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class ConnectionOutputStreamTests {

	@Test
	public void write() throws Exception {
		OutputStream out = mock(OutputStream.class);
		ConnectionOutputStream outputStream = new ConnectionOutputStream(out);
		byte[] b = new byte[100];
		outputStream.write(b, 1, 2);
		verify(out).write(b, 1, 2);
	}

	@Test
	public void writeHttp() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConnectionOutputStream outputStream = new ConnectionOutputStream(out);
		outputStream.writeHttp(new ByteArrayInputStream("hi".getBytes()), "x-type");
		String expected = "";
		expected += "HTTP/1.1 200 OK\r\n";
		expected += "Content-Type: x-type\r\n";
		expected += "Content-Length: 2\r\n";
		expected += "Connection: close\r\n\r\n";
		expected += "hi";
		assertThat(out.toString()).isEqualTo(expected);
	}

	@Test
	public void writeHeaders() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConnectionOutputStream outputStream = new ConnectionOutputStream(out);
		outputStream.writeHeaders("A: a", "B: b");
		outputStream.flush();
		String expected = "";
		expected += "A: a\r\n";
		expected += "B: b\r\n\r\n";
		assertThat(out.toString()).isEqualTo(expected);
	}

}
