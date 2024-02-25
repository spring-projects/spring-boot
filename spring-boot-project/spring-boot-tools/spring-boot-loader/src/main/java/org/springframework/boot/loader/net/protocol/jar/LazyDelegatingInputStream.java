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

	/**
     * Reads a single byte of data from the input stream.
     *
     * @return the byte read, or -1 if the end of the stream is reached
     * @throws IOException if an I/O error occurs
     */
    @Override
	public int read() throws IOException {
		return in().read();
	}

	/**
     * Reads up to {@code b.length} bytes of data from this input stream into an array of bytes.
     * This method delegates the call to the underlying input stream.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public int read(byte[] b) throws IOException {
		return in().read(b);
	}

	/**
     * Reads up to len bytes of data from the input stream into an array of bytes.
     * 
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the buffer at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in().read(b, off, len);
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
		return in().skip(n);
	}

	/**
     * Returns the number of bytes that can be read from this input stream without blocking.
     * This method delegates the call to the underlying input stream's available() method.
     *
     * @return the number of bytes that can be read from this input stream without blocking
     * @throws IOException if an I/O error occurs
     */
    @Override
	public int available() throws IOException {
		return in().available();
	}

	/**
     * Returns a boolean indicating whether or not this input stream supports the mark and reset methods.
     *
     * @return true if this input stream supports the mark and reset methods, false otherwise.
     */
    @Override
	public boolean markSupported() {
		try {
			return in().markSupported();
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
     * Marks the current position in this input stream. A subsequent call to the reset method repositions this stream at the last marked position.
     * 
     * @param readlimit the maximum limit of bytes that can be read before the mark position becomes invalid
     */
    @Override
	public synchronized void mark(int readlimit) {
		try {
			in().mark(readlimit);
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	/**
     * Resets the input stream to its initial state.
     * 
     * @throws IOException if an I/O error occurs while resetting the input stream
     */
    @Override
	public synchronized void reset() throws IOException {
		in().reset();
	}

	/**
     * Returns the input stream for this LazyDelegatingInputStream.
     * If the input stream has not been initialized yet, it will be lazily initialized
     * by calling the getDelegateInputStream() method.
     * 
     * @return the input stream for this LazyDelegatingInputStream
     * @throws IOException if an I/O error occurs while initializing the input stream
     */
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

	/**
     * Closes the input stream.
     * 
     * @throws IOException if an I/O error occurs while closing the input stream
     */
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

	/**
     * Returns the delegate input stream.
     *
     * @return the delegate input stream
     * @throws IOException if an I/O error occurs
     */
    protected abstract InputStream getDelegateInputStream() throws IOException;

}
