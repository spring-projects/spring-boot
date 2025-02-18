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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * Resources created managed and cleaned by a {@link NestedUrlConnection} instance and
 * suitable for registration with a {@link Cleaner}.
 *
 * @author Phillip Webb
 */
class NestedUrlConnectionResources implements Runnable {

	private final NestedLocation location;

	private volatile ZipContent zipContent;

	private volatile long size = -1;

	private volatile InputStream inputStream;

	NestedUrlConnectionResources(NestedLocation location) {
		this.location = location;
	}

	NestedLocation getLocation() {
		return this.location;
	}

	void connect() throws IOException {
		synchronized (this) {
			if (this.zipContent == null) {
				this.zipContent = ZipContent.open(this.location.path(), this.location.nestedEntryName());
				try {
					connectData();
				}
				catch (IOException | RuntimeException ex) {
					this.zipContent.close();
					this.zipContent = null;
					throw ex;
				}
			}
		}
	}

	private void connectData() throws IOException {
		CloseableDataBlock data = this.zipContent.openRawZipData();
		try {
			this.size = data.size();
			this.inputStream = data.asInputStream();
		}
		catch (IOException | RuntimeException ex) {
			data.close();
		}
	}

	InputStream getInputStream() throws IOException {
		synchronized (this) {
			if (this.inputStream == null) {
				throw new IOException("Nested location not found " + this.location);
			}
			return this.inputStream;
		}
	}

	long getContentLength() {
		return this.size;
	}

	@Override
	public void run() {
		releaseAll();
	}

	private void releaseAll() {
		synchronized (this) {
			if (this.zipContent != null) {
				IOException exceptionChain = null;
				try {
					this.inputStream.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
				try {
					this.zipContent.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
				this.size = -1;
				if (exceptionChain != null) {
					throw new UncheckedIOException(exceptionChain);
				}
			}
		}
	}

	private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
		if (exceptionChain != null) {
			exceptionChain.addSuppressed(ex);
			return exceptionChain;
		}
		return ex;
	}

}
