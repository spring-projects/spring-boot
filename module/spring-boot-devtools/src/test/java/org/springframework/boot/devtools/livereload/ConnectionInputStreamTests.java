/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.livereload;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link ConnectionInputStream}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class ConnectionInputStreamTests {

	private static final byte[] NO_BYTES = {};

	@Test
	void readHeader() throws Exception {
		String header = "";
		for (int i = 0; i < 100; i++) {
			header += "x-something-" + i + ": xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
		}
		String data = header + "\r\n\r\ncontent\r\n";
		ConnectionInputStream inputStream = new ConnectionInputStream(new ByteArrayInputStream(data.getBytes()));
		assertThat(inputStream.readHeader()).isEqualTo(header);
	}

	@Test
	void readHeaderThrowsWhenHeaderIsMalformed() {
		byte[] header = new byte[10000];
		new Random().nextBytes(header);
		ConnectionInputStream inputStream = new ConnectionInputStream(new ByteArrayInputStream(header));
		assertThatIOException().isThrownBy(inputStream::readHeader).withMessage("Malformed header");
	}

	@Test
	void readFully() throws Exception {
		byte[] bytes = "the data that we want to read fully".getBytes();
		LimitedInputStream source = new LimitedInputStream(new ByteArrayInputStream(bytes), 2);
		ConnectionInputStream inputStream = new ConnectionInputStream(source);
		byte[] buffer = new byte[bytes.length];
		inputStream.readFully(buffer, 0, buffer.length);
		assertThat(buffer).isEqualTo(bytes);
	}

	@Test
	void checkedRead() {
		ConnectionInputStream inputStream = new ConnectionInputStream(new ByteArrayInputStream(NO_BYTES));
		assertThatIOException().isThrownBy(inputStream::checkedRead).withMessageContaining("End of stream");
	}

	@Test
	void checkedReadArray() {
		byte[] buffer = new byte[100];
		ConnectionInputStream inputStream = new ConnectionInputStream(new ByteArrayInputStream(NO_BYTES));
		assertThatIOException().isThrownBy(() -> inputStream.checkedRead(buffer, 0, buffer.length))
			.withMessageContaining("End of stream");
	}

	static class LimitedInputStream extends FilterInputStream {

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
