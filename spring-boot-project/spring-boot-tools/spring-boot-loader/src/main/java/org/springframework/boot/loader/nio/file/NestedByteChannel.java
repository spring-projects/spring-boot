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

	/**
     * Constructs a new NestedByteChannel object with the specified path and nested entry name.
     * 
     * @param path the path to the file containing the nested entry
     * @param nestedEntryName the name of the nested entry within the file
     * @throws IOException if an I/O error occurs
     */
    NestedByteChannel(Path path, String nestedEntryName) throws IOException {
		this(path, nestedEntryName, Cleaner.instance);
	}

	/**
     * Constructs a new NestedByteChannel with the specified path, nested entry name, and cleaner.
     * 
     * @param path the path to the file containing the nested entry
     * @param nestedEntryName the name of the nested entry within the file
     * @param cleaner the cleaner to register for cleanup
     * @throws IOException if an I/O error occurs while accessing the file
     */
    NestedByteChannel(Path path, String nestedEntryName, Cleaner cleaner) throws IOException {
		this.resources = new Resources(path, nestedEntryName);
		this.cleanup = cleaner.register(this, this.resources);
		this.size = this.resources.getData().size();
	}

	/**
     * Returns a boolean value indicating whether the NestedByteChannel is open or closed.
     *
     * @return {@code true} if the NestedByteChannel is open, {@code false} otherwise.
     */
    @Override
	public boolean isOpen() {
		return !this.closed;
	}

	/**
     * Closes the NestedByteChannel.
     * 
     * @throws IOException if an I/O error occurs while closing the channel
     */
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

	/**
     * Reads bytes from the channel into the given ByteBuffer.
     * 
     * @param dst the ByteBuffer to read bytes into
     * @return the total number of bytes read, or 0 if the end of the channel has been reached
     * @throws IOException if an I/O error occurs
     * @throws ClosedChannelException if the channel is closed
     */
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

	/**
     * Writes a sequence of bytes from the given buffer into this channel.
     * 
     * @param src the buffer containing the bytes to be written
     * @return the number of bytes written, which is always 0
     * @throws NonWritableChannelException if this channel is not open for writing
     * @throws IOException if an I/O error occurs
     */
    @Override
	public int write(ByteBuffer src) throws IOException {
		throw new NonWritableChannelException();
	}

	/**
     * Returns the current position within the channel.
     *
     * @return the current position within the channel
     * @throws IOException if an I/O error occurs
     */
    @Override
	public long position() throws IOException {
		assertNotClosed();
		return this.position;
	}

	/**
     * Sets the position of the channel to the specified position.
     * 
     * @param position the new position of the channel
     * @return the channel with the updated position
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the position is out of bounds
     */
    @Override
	public SeekableByteChannel position(long position) throws IOException {
		assertNotClosed();
		if (position < 0 || position >= this.size) {
			throw new IllegalArgumentException("Position must be in bounds");
		}
		this.position = position;
		return this;
	}

	/**
     * Returns the size of the channel.
     *
     * @return the size of the channel
     * @throws IOException if an I/O error occurs
     * @throws ClosedChannelException if the channel is closed
     */
    @Override
	public long size() throws IOException {
		assertNotClosed();
		return this.size;
	}

	/**
     * Truncates the size of the channel to the specified size.
     *
     * @param size the new size of the channel
     * @return a SeekableByteChannel object representing the truncated channel
     * @throws IOException if an I/O error occurs
     * @throws NonWritableChannelException if the channel is not writable
     */
    @Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new NonWritableChannelException();
	}

	/**
     * Checks if the channel is closed.
     *
     * @throws ClosedChannelException if the channel is closed
     */
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

		/**
         * Constructs a new Resources object with the specified path and nested entry name.
         * 
         * @param path the path to the zip file
         * @param nestedEntryName the name of the nested entry within the zip file
         * @throws IOException if an I/O error occurs while opening the zip file or accessing the nested entry
         */
        Resources(Path path, String nestedEntryName) throws IOException {
			this.zipContent = ZipContent.open(path, nestedEntryName);
			this.data = this.zipContent.openRawZipData();
		}

		/**
         * Retrieves the data stored in the DataBlock object.
         *
         * @return the data stored in the DataBlock object
         */
        DataBlock getData() {
			return this.data;
		}

		/**
         * Releases all resources.
         */
        @Override
		public void run() {
			releaseAll();
		}

		/**
         * Releases all resources held by the Resources class.
         * This method closes the data and zipContent streams.
         * If an IOException occurs while closing the streams, it is caught and stored in the exception variable.
         * If multiple IOExceptions occur, they are added as suppressed exceptions to the original exception.
         * If an exception occurred, it is thrown as an UncheckedIOException.
         */
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
