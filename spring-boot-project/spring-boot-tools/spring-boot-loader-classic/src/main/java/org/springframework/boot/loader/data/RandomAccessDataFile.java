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

package org.springframework.boot.loader.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * {@link RandomAccessData} implementation backed by a {@link RandomAccessFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class RandomAccessDataFile implements RandomAccessData {

	private final FileAccess fileAccess;

	private final long offset;

	private final long length;

	/**
	 * Create a new {@link RandomAccessDataFile} backed by the specified file.
	 * @param file the underlying file
	 * @throws IllegalArgumentException if the file is null or does not exist
	 */
	public RandomAccessDataFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		this.fileAccess = new FileAccess(file);
		this.offset = 0L;
		this.length = file.length();
	}

	/**
	 * Private constructor used to create a {@link #getSubsection(long, long) subsection}.
	 * @param fileAccess provides access to the underlying file
	 * @param offset the offset of the section
	 * @param length the length of the section
	 */
	private RandomAccessDataFile(FileAccess fileAccess, long offset, long length) {
		this.fileAccess = fileAccess;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * Returns the underlying File.
	 * @return the underlying file
	 */
	public File getFile() {
		return this.fileAccess.file;
	}

	/**
     * Returns an input stream for reading the data from the RandomAccessDataFile.
     *
     * @return an input stream for reading the data from the RandomAccessDataFile.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public InputStream getInputStream() throws IOException {
		return new DataInputStream();
	}

	/**
     * Returns a subsection of the random access data.
     *
     * @param offset the starting offset of the subsection
     * @param length the length of the subsection
     * @return a new RandomAccessData object representing the subsection
     * @throws IndexOutOfBoundsException if the offset or length is negative, or if the sum of offset and length exceeds the length of the data
     */
    @Override
	public RandomAccessData getSubsection(long offset, long length) {
		if (offset < 0 || length < 0 || offset + length > this.length) {
			throw new IndexOutOfBoundsException();
		}
		return new RandomAccessDataFile(this.fileAccess, this.offset + offset, length);
	}

	/**
     * Reads the entire content of the file and returns it as a byte array.
     *
     * @return the content of the file as a byte array
     * @throws IOException if an I/O error occurs while reading the file
     */
    @Override
	public byte[] read() throws IOException {
		return read(0, this.length);
	}

	/**
     * Reads a specified length of bytes from the file starting at the given offset.
     * 
     * @param offset the starting offset in the file
     * @param length the number of bytes to read
     * @return a byte array containing the read bytes
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if the offset is greater than the file length
     * @throws EOFException if the offset plus length is greater than the file length
     */
    @Override
	public byte[] read(long offset, long length) throws IOException {
		if (offset > this.length) {
			throw new IndexOutOfBoundsException();
		}
		if (offset + length > this.length) {
			throw new EOFException();
		}
		byte[] bytes = new byte[(int) length];
		read(bytes, offset, 0, bytes.length);
		return bytes;
	}

	/**
     * Reads a byte from the specified position in the file.
     *
     * @param position the position from which to read the byte
     * @return the byte value at the specified position, or -1 if the position is beyond the file length
     * @throws IOException if an I/O error occurs while reading the byte
     */
    private int readByte(long position) throws IOException {
		if (position >= this.length) {
			return -1;
		}
		return this.fileAccess.readByte(this.offset + position);
	}

	/**
     * Reads bytes from the file at the specified position.
     *
     * @param bytes    the byte array to read the data into
     * @param position the position in the file to start reading from
     * @param offset   the offset in the byte array to start writing the data into
     * @param length   the number of bytes to read
     * @return the number of bytes read, or -1 if the position is beyond the end of the file
     * @throws IOException if an I/O error occurs while reading the file
     */
    private int read(byte[] bytes, long position, int offset, int length) throws IOException {
		if (position > this.length) {
			return -1;
		}
		return this.fileAccess.read(bytes, this.offset + position, offset, length);
	}

	/**
     * Returns the size of the RandomAccessDataFile.
     *
     * @return the size of the RandomAccessDataFile
     */
    @Override
	public long getSize() {
		return this.length;
	}

	/**
     * Closes the RandomAccessDataFile and releases any system resources associated with it.
     * 
     * @throws IOException if an I/O error occurs while closing the file
     */
    public void close() throws IOException {
		this.fileAccess.close();
	}

	/**
	 * {@link InputStream} implementation for the {@link RandomAccessDataFile}.
	 */
	private final class DataInputStream extends InputStream {

		private int position;

		/**
         * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.
         * If no byte is available because the end of the stream has been reached, the value -1 is returned.
         *
         * @return the next byte of data, or -1 if the end of the stream has been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
		public int read() throws IOException {
			int read = RandomAccessDataFile.this.readByte(this.position);
			if (read > -1) {
				moveOn(1);
			}
			return read;
		}

		/**
         * Reads up to {@code b.length} bytes of data from this input stream into an array of bytes.
         * This method blocks until some input is available.
         * 
         * @param b the buffer into which the data is read.
         * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
         * @throws IOException if an I/O error occurs.
         * @throws NullPointerException if {@code b} is {@code null}.
         */
        @Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, (b != null) ? b.length : 0);
		}

		/**
         * Reads up to len bytes of data from the input stream into an array of bytes.
         * 
         * @param b   the buffer into which the data is read.
         * @param off the start offset in array b at which the data is written.
         * @param len the maximum number of bytes to read.
         * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
         * @throws IOException              if an I/O error occurs.
         * @throws NullPointerException     if the byte array b is null.
         * @throws IndexOutOfBoundsException if off is negative, len is negative, or len is greater than b.length - off.
         */
        @Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException("Bytes must not be null");
			}
			return doRead(b, off, len);
		}

		/**
		 * Perform the actual read.
		 * @param b the bytes to read or {@code null} when reading a single byte
		 * @param off the offset of the byte array
		 * @param len the length of data to read
		 * @return the number of bytes read into {@code b} or the actual read byte if
		 * {@code b} is {@code null}. Returns -1 when the end of the stream is reached
		 * @throws IOException in case of I/O errors
		 */
		int doRead(byte[] b, int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}
			int cappedLen = cap(len);
			if (cappedLen <= 0) {
				return -1;
			}
			return (int) moveOn(RandomAccessDataFile.this.read(b, this.position, off, cappedLen));
		}

		/**
         * Skips over and discards the specified number of bytes from the input stream.
         * 
         * @param n the number of bytes to be skipped
         * @return the actual number of bytes skipped
         * @throws IOException if an I/O error occurs
         */
        @Override
		public long skip(long n) throws IOException {
			return (n <= 0) ? 0 : moveOn(cap(n));
		}

		/**
         * Returns the number of bytes that can be read from this input stream without blocking.
         * 
         * @return the number of bytes available to be read from this input stream
         * @throws IOException if an I/O error occurs
         */
        @Override
		public int available() throws IOException {
			return (int) RandomAccessDataFile.this.length - this.position;
		}

		/**
		 * Cap the specified value such that it cannot exceed the number of bytes
		 * remaining.
		 * @param n the value to cap
		 * @return the capped value
		 */
		private int cap(long n) {
			return (int) Math.min(RandomAccessDataFile.this.length - this.position, n);
		}

		/**
		 * Move the stream position forwards the specified amount.
		 * @param amount the amount to move
		 * @return the amount moved
		 */
		private long moveOn(int amount) {
			this.position += amount;
			return amount;
		}

	}

	/**
     * FileAccess class.
     */
    private static final class FileAccess {

		private final Object monitor = new Object();

		private final File file;

		private RandomAccessFile randomAccessFile;

		/**
         * Constructs a new FileAccess object with the specified file.
         * 
         * @param file the file to be accessed
         */
        private FileAccess(File file) {
			this.file = file;
			openIfNecessary();
		}

		/**
         * Reads bytes from the file at the specified position.
         *
         * @param bytes    the byte array to read the data into
         * @param position the position in the file to start reading from
         * @param offset   the offset in the byte array to start writing the data into
         * @param length   the maximum number of bytes to read
         * @return the total number of bytes read into the byte array, or -1 if there is no more data because the end of the file has been reached
         * @throws IOException if an I/O error occurs
         */
        private int read(byte[] bytes, long position, int offset, int length) throws IOException {
			synchronized (this.monitor) {
				openIfNecessary();
				this.randomAccessFile.seek(position);
				return this.randomAccessFile.read(bytes, offset, length);
			}
		}

		/**
         * Opens the file if necessary.
         * 
         * If the randomAccessFile is null, it attempts to open the file in read mode using RandomAccessFile.
         * If the file does not exist, it throws an IllegalArgumentException.
         * 
         * @throws IllegalArgumentException if the file does not exist
         */
        private void openIfNecessary() {
			if (this.randomAccessFile == null) {
				try {
					this.randomAccessFile = new RandomAccessFile(this.file, "r");
				}
				catch (FileNotFoundException ex) {
					throw new IllegalArgumentException(
							String.format("File %s must exist", this.file.getAbsolutePath()));
				}
			}
		}

		/**
         * Closes the random access file.
         * 
         * @throws IOException if an I/O error occurs while closing the file
         */
        private void close() throws IOException {
			synchronized (this.monitor) {
				if (this.randomAccessFile != null) {
					this.randomAccessFile.close();
					this.randomAccessFile = null;
				}
			}
		}

		/**
         * Reads a byte from the file at the specified position.
         *
         * @param position the position in the file to read the byte from
         * @return the byte read from the file
         * @throws IOException if an I/O error occurs
         */
        private int readByte(long position) throws IOException {
			synchronized (this.monitor) {
				openIfNecessary();
				this.randomAccessFile.seek(position);
				return this.randomAccessFile.read();
			}
		}

	}

}
