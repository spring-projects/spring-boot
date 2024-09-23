/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} that delegates lazily to another {@link InputStream}.
 *
 * @author Phillip Webb
 */
abstract class LazyDelegatingInputStream extends InputStream {

	private volatile InputStream in;

	@Override
	public int read() throws IOException {
		return in().read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return in().read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in().read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return in().skip(n);
	}

	@Override
	public int available() throws IOException {
		return in().available();
	}

	@Override
	public boolean markSupported() {
		try {
			return in().markSupported();
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public synchronized void mark(int readLimit) {
		try {
			in().mark(readLimit);
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		in().reset();
	}

	private InputStream in() throws IOException {
		InputStream in = this.in;
		if (in == null) {
			synchronized (this) {
				in = this.in;
				if (in == null) {
					in = getDelegateInputStream();
					this.in = in;
				}
			}
		}
		return in;
	}

	@Override
	public void close() throws IOException {
		InputStream in = this.in;
		if (in != null) {
			synchronized (this) {
				in = this.in;
				if (in != null) {
					in.close();
				}
			}
		}
	}

	protected abstract InputStream getDelegateInputStream() throws IOException;

}
