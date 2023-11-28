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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;
import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.DataBlock;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * {@link SeekableByteChannel} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedByteChannel implements SeekableByteChannel {

	private long position;

	private final Resources resources;

	private final Cleanable cleanup;

	private final long size;

	private volatile boolean closed;

	NestedByteChannel(Path path, String nestedEntryName) throws IOException {
		this(path, nestedEntryName, Cleaner.instance);
	}

	NestedByteChannel(Path path, String nestedEntryName, Cleaner cleaner) throws IOException {
		this.resources = new Resources(path, nestedEntryName);
		this.cleanup = cleaner.register(this, this.resources);
		this.size = this.resources.getData().size();
	}

	@Override
	public boolean isOpen() {
		return !this.closed;
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		try {
			this.cleanup.clean();
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		assertNotClosed();
		int total = 0;
		while (dst.remaining() > 0) {
			int count = this.resources.getData().read(dst, this.position);
			if (count <= 0) {
				return (total != 0) ? 0 : count;
			}
			total += count;
			this.position += count;
		}
		return total;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new NonWritableChannelException();
	}

	@Override
	public long position() throws IOException {
		assertNotClosed();
		return this.position;
	}

	@Override
	public SeekableByteChannel position(long position) throws IOException {
		assertNotClosed();
		if (position < 0 || position >= this.size) {
			throw new IllegalArgumentException("Position must be in bounds");
		}
		this.position = position;
		return this;
	}

	@Override
	public long size() throws IOException {
		assertNotClosed();
		return this.size;
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new NonWritableChannelException();
	}

	private void assertNotClosed() throws ClosedChannelException {
		if (this.closed) {
			throw new ClosedChannelException();
		}
	}

	/**
	 * Resources used by the channel and suitable for registration with a {@link Cleaner}.
	 */
	static class Resources implements Runnable {

		private final ZipContent zipContent;

		private final CloseableDataBlock data;

		Resources(Path path, String nestedEntryName) throws IOException {
			this.zipContent = ZipContent.open(path, nestedEntryName);
			this.data = this.zipContent.openRawZipData();
		}

		DataBlock getData() {
			return this.data;
		}

		@Override
		public void run() {
			releaseAll();
		}

		private void releaseAll() {
			IOException exception = null;
			try {
				this.data.close();
			}
			catch (IOException ex) {
				exception = ex;
			}
			try {
				this.zipContent.close();
			}
			catch (IOException ex) {
				if (exception != null) {
					ex.addSuppressed(exception);
				}
				exception = ex;
			}
			if (exception != null) {
				throw new UncheckedIOException(exception);
			}
		}

	}

}
