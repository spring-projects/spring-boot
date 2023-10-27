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

	DataBlockInputStream(DataBlock dataBlock) throws IOException {
		this.dataBlock = dataBlock;
		this.remaining = dataBlock.size();
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
	}

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

	@Override
	public long skip(long n) throws IOException {
		long count = (n > 0) ? maxForwardSkip(n) : maxBackwardSkip(n);
		this.pos += count;
		this.remaining -= count;
		return count;
	}

	private long maxForwardSkip(long n) {
		boolean willCauseOverflow = (this.pos + n) < 0;
		return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
	}

	private long maxBackwardSkip(long n) {
		return Math.max(-this.pos, n);
	}

	@Override
	public int available() {
		if (this.closed) {
			return 0;
		}
		return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
	}

	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("InputStream closed");
		}
	}

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
