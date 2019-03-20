/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionInputStream}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class ConnectionInputStreamTests {

	private static final byte[] NO_BYTES = {};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void readHeader() throws Exception {
		String header = "";
		for (int i = 0; i < 100; i++) {
			header += "x-something-" + i
					+ ": xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
		}
		String data = header + "\r\n\r\n" + "content\r\n";
		ConnectionInputStream inputStream = new ConnectionInputStream(
				new ByteArrayInputStream(data.getBytes()));
		assertThat(inputStream.readHeader()).isEqualTo(header);
	}

	@Test
	public void readFully() throws Exception {
		byte[] bytes = "the data that we want to read fully".getBytes();
		LimitedInputStream source = new LimitedInputStream(
				new ByteArrayInputStream(bytes), 2);
		ConnectionInputStream inputStream = new ConnectionInputStream(source);
		byte[] buffer = new byte[bytes.length];
		inputStream.readFully(buffer, 0, buffer.length);
		assertThat(buffer).isEqualTo(bytes);
	}

	@Test
	public void checkedRead() throws Exception {
		ConnectionInputStream inputStream = new ConnectionInputStream(
				new ByteArrayInputStream(NO_BYTES));
		this.thrown.expect(IOException.class);
		this.thrown.expectMessage("End of stream");
		inputStream.checkedRead();
	}

	@Test
	public void checkedReadArray() throws Exception {
		ConnectionInputStream inputStream = new ConnectionInputStream(
				new ByteArrayInputStream(NO_BYTES));
		this.thrown.expect(IOException.class);
		this.thrown.expectMessage("End of stream");
		byte[] buffer = new byte[100];
		inputStream.checkedRead(buffer, 0, buffer.length);
	}

	private static class LimitedInputStream extends FilterInputStream {

		private final int max;

		protected LimitedInputStream(InputStream in, int max) {
			super(in);
			this.max = max;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return super.read(b, off, Math.min(len, this.max));
		}

	}

}
