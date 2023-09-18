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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * Reference counted {@link DataBlock} implementation backed by a {@link FileChannel} with
 * support for slicing.
 *
 * @author Phillip Webb
 */
class FileChannelDataBlock implements CloseableDataBlock {

	private static final DebugLogger debug = DebugLogger.get(FileChannelDataBlock.class);

	static Tracker tracker;

	private final ManagedFileChannel channel;

	private final long offset;

	private final long size;

	FileChannelDataBlock(Path path) throws IOException {
		this.channel = new ManagedFileChannel(path);
		this.offset = 0;
		this.size = Files.size(path);
	}

	FileChannelDataBlock(ManagedFileChannel channel, long offset, long size) {
		this.channel = channel;
		this.offset = offset;
		this.size = size;
	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public int read(ByteBuffer dst, long pos) throws IOException {
		if (pos < 0) {
			throw new IllegalArgumentException("Position must not be negative");
		}
		ensureOpen(ClosedChannelException::new);
		int remaining = (int) (this.size - pos);
		if (remaining <= 0) {
			return -1;
		}
		int originalDestinationLimit = -1;
		if (dst.remaining() > remaining) {
			originalDestinationLimit = dst.limit();
			dst.limit(dst.position() + remaining);
		}
		int result = this.channel.read(dst, this.offset + pos);
		if (originalDestinationLimit != -1) {
			dst.limit(originalDestinationLimit);
		}
		return result;
	}

	/**
	 * Open a connection to this block, increasing the reference count and re-opening the
	 * underlying file channel if necessary.
	 * @throws IOException on I/O error
	 */
	void open() throws IOException {
		this.channel.open();
	}

	/**
	 * Close a connection to this block, decreasing the reference count and closing the
	 * underlying file channel if necessary.
	 * @throws IOException on I/O error
	 */
	@Override
	public void close() throws IOException {
		this.channel.close();
	}

	/**
	 * Ensure that the underlying file channel is currently open.
	 * @param exceptionSupplier a supplier providing the exception to throw
	 * @param <E> the exception type
	 * @throws E if the channel is closed
	 */
	<E extends Exception> void ensureOpen(Supplier<E> exceptionSupplier) throws E {
		this.channel.ensureOpen(exceptionSupplier);
	}

	/**
	 * Return a new {@link FileChannelDataBlock} slice providing access to a subset of the
	 * data. The caller is responsible for calling {@link #open()} and {@link #close()} on
	 * the returned block.
	 * @param offset the start offset for the slice relative to this block
	 * @return a new {@link FileChannelDataBlock} instance
	 * @throws IOException on I/O error
	 */
	FileChannelDataBlock slice(long offset) throws IOException {
		return slice(offset, this.size - offset);
	}

	/**
	 * Return a new {@link FileChannelDataBlock} slice providing access to a subset of the
	 * data. The caller is responsible for calling {@link #open()} and {@link #close()} on
	 * the returned block.
	 * @param offset the start offset for the slice relative to this block
	 * @param size the size of the new slice
	 * @return a new {@link FileChannelDataBlock} instance
	 */
	FileChannelDataBlock slice(long offset, long size) {
		if (offset == 0 && size == this.size) {
			return this;
		}
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must not be negative");
		}
		if (size < 0 || offset + size > this.size) {
			throw new IllegalArgumentException("Size must not be negative and must be within bounds");
		}
		debug.log("Slicing %s at %s with size %s", this.channel, offset, size);
		return new FileChannelDataBlock(this.channel, this.offset + offset, size);
	}

	/**
	 * Manages access to underlying {@link FileChannel}.
	 */
	static class ManagedFileChannel {

		static final int BUFFER_SIZE = 1024 * 10;

		private final Path path;

		private int referenceCount;

		private FileChannel fileChannel;

		private ByteBuffer buffer;

		private long bufferPosition = -1;

		private int bufferSize;

		private final Object lock = new Object();

		ManagedFileChannel(Path path) {
			if (!Files.isRegularFile(path)) {
				throw new IllegalArgumentException(path + " must be a regular file");
			}
			this.path = path;
		}

		int read(ByteBuffer dst, long position) throws IOException {
			synchronized (this.lock) {
				if (position < this.bufferPosition || position >= this.bufferPosition + this.bufferSize) {
					this.buffer.clear();
					this.bufferSize = this.fileChannel.read(this.buffer, position);
					this.bufferPosition = position;
				}
				if (this.bufferSize <= 0) {
					return this.bufferSize;
				}
				int offset = (int) (position - this.bufferPosition);
				int length = Math.min(this.bufferSize - offset, dst.remaining());
				dst.put(dst.position(), this.buffer, offset, length);
				dst.position(dst.position() + length);
				return length;
			}
		}

		void open() throws IOException {
			synchronized (this.lock) {
				if (this.referenceCount == 0) {
					debug.log("Opening '%s'", this.path);
					this.fileChannel = FileChannel.open(this.path, StandardOpenOption.READ);
					this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
					if (tracker != null) {
						tracker.openedFileChannel(this.path, this.fileChannel);
					}
				}
				this.referenceCount++;
				debug.log("Reference count for '%s' incremented to %s", this.path, this.referenceCount);
			}
		}

		void close() throws IOException {
			synchronized (this.lock) {
				if (this.referenceCount == 0) {
					return;
				}
				this.referenceCount--;
				if (this.referenceCount == 0) {
					debug.log("Closing '%s'", this.path);
					this.buffer = null;
					this.bufferPosition = -1;
					this.bufferSize = 0;
					this.fileChannel.close();
					if (tracker != null) {
						tracker.closedFileChannel(this.path, this.fileChannel);
					}
					this.fileChannel = null;
				}
				debug.log("Reference count for '%s' decremented to %s", this.path, this.referenceCount);
			}
		}

		<E extends Exception> void ensureOpen(Supplier<E> exceptionSupplier) throws E {
			synchronized (this.lock) {
				if (this.referenceCount == 0) {
					throw exceptionSupplier.get();
				}
			}
		}

		@Override
		public String toString() {
			return this.path.toString();
		}

	}

	/**
	 * Internal tracker used to check open and closing of files in tests.
	 */
	interface Tracker {

		void openedFileChannel(Path path, FileChannel fileChannel);

		void closedFileChannel(Path path, FileChannel fileChannel);

	}

}
