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

package org.springframework.boot.buildpack.platform.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.socket.FileDescriptor.Handle;

/**
 * A {@link Socket} implementation for Linux of BSD domain sockets.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public abstract class DomainSocket extends AbstractSocket {

	private static final int SHUT_RD = 0;

	private static final int SHUT_WR = 1;

	protected static final int PF_LOCAL = 1;

	protected static final byte AF_LOCAL = 1;

	protected static final int SOCK_STREAM = 1;

	private final FileDescriptor fileDescriptor;

	private final InputStream inputStream;

	private final OutputStream outputStream;

	static {
		Native.register(Platform.C_LIBRARY_NAME);
	}

	/**
     * Constructs a new DomainSocket object with the specified path.
     * 
     * @param path the path to the domain socket
     * @throws IOException if an I/O error occurs while opening the domain socket
     */
    DomainSocket(String path) throws IOException {
		try {
			this.fileDescriptor = open(path);
			this.inputStream = new DomainSocketInputStream();
			this.outputStream = new DomainSocketOutputStream();
		}
		catch (LastErrorException ex) {
			throw new IOException(ex);
		}
	}

	/**
     * Opens a domain socket connection to the specified path.
     * 
     * @param path the path of the domain socket
     * @return a FileDescriptor representing the opened domain socket connection
     * @throws RuntimeException if an error occurs while opening the domain socket connection
     */
    private FileDescriptor open(String path) {
		int handle = socket(PF_LOCAL, SOCK_STREAM, 0);
		try {
			connect(path, handle);
			return new FileDescriptor(handle, this::close);
		}
		catch (RuntimeException ex) {
			close(handle);
			throw ex;
		}
	}

	/**
     * Reads data from the specified ByteBuffer.
     * 
     * @param buffer the ByteBuffer to read data into
     * @return the number of bytes read, or -1 if the handle is closed
     * @throws IOException if an I/O error occurs
     */
    private int read(ByteBuffer buffer) throws IOException {
		try (Handle handle = this.fileDescriptor.acquire()) {
			if (handle.isClosed()) {
				return -1;
			}
			try {
				return read(handle.intValue(), buffer, buffer.remaining());
			}
			catch (LastErrorException ex) {
				throw new IOException(ex);
			}
		}
	}

	/**
     * Writes the contents of the given ByteBuffer to the domain socket.
     * 
     * @param buffer the ByteBuffer containing the data to be written
     * @throws IOException if an I/O error occurs while writing to the domain socket
     */
    public void write(ByteBuffer buffer) throws IOException {
		try (Handle handle = this.fileDescriptor.acquire()) {
			if (!handle.isClosed()) {
				try {
					write(handle.intValue(), buffer, buffer.remaining());
				}
				catch (LastErrorException ex) {
					throw new IOException(ex);
				}
			}
		}
	}

	/**
     * Returns the input stream associated with this DomainSocket.
     *
     * @return the input stream associated with this DomainSocket
     */
    @Override
	public InputStream getInputStream() {
		return this.inputStream;
	}

	/**
     * Returns the output stream associated with this DomainSocket.
     *
     * @return the output stream associated with this DomainSocket
     */
    @Override
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	/**
     * Closes the DomainSocket and releases any system resources associated with it.
     * This method overrides the close() method in the superclass.
     * 
     * @throws IOException if an I/O error occurs while closing the DomainSocket
     */
    @Override
	public void close() throws IOException {
		super.close();
		try {
			this.fileDescriptor.close();
		}
		catch (LastErrorException ex) {
			throw new IOException(ex);
		}
	}

	/**
     * Connects to a domain socket using the specified path and handle.
     *
     * @param path   the path of the domain socket to connect to
     * @param handle the handle to use for the connection
     */
    protected abstract void connect(String path, int handle);

	/**
     * Creates a new socket with the specified domain, type, and protocol.
     *
     * @param domain    the domain of the socket (e.g., AF_UNIX, AF_INET)
     * @param type      the type of the socket (e.g., SOCK_STREAM, SOCK_DGRAM)
     * @param protocol  the protocol to be used by the socket (e.g., IPPROTO_TCP, IPPROTO_UDP)
     * @return          the file descriptor of the newly created socket
     * @throws LastErrorException if an error occurs while creating the socket
     */
    private native int socket(int domain, int type, int protocol) throws LastErrorException;

	/**
     * Reads data from the specified file descriptor into the provided ByteBuffer.
     * 
     * @param fd the file descriptor to read from
     * @param buffer the ByteBuffer to store the read data
     * @param count the maximum number of bytes to read
     * @return the number of bytes read
     * @throws LastErrorException if an error occurs during the read operation
     */
    private native int read(int fd, ByteBuffer buffer, int count) throws LastErrorException;

	/**
     * Writes data from the specified ByteBuffer to the specified file descriptor.
     *
     * @param fd the file descriptor to write to
     * @param buffer the ByteBuffer containing the data to be written
     * @param count the number of bytes to write
     * @return the number of bytes written
     * @throws LastErrorException if an error occurs during the write operation
     */
    private native int write(int fd, ByteBuffer buffer, int count) throws LastErrorException;

	/**
     * Closes the specified file descriptor.
     *
     * @param fd the file descriptor to be closed
     * @return the result of the close operation
     * @throws LastErrorException if an error occurs while closing the file descriptor
     */
    private native int close(int fd) throws LastErrorException;

	/**
	 * Return a new {@link DomainSocket} for the given path.
	 * @param path the path to the domain socket
	 * @return a {@link DomainSocket} instance
	 * @throws IOException if the socket cannot be opened
	 */
	public static DomainSocket get(String path) throws IOException {
		if (Platform.isMac() || isBsdPlatform()) {
			return new BsdDomainSocket(path);
		}
		return new LinuxDomainSocket(path);
	}

	/**
     * Checks if the current platform is a BSD platform.
     * 
     * @return true if the platform is FreeBSD, kFreeBSD, NetBSD, or OpenBSD; false otherwise.
     */
    private static boolean isBsdPlatform() {
		return Platform.isFreeBSD() || Platform.iskFreeBSD() || Platform.isNetBSD() || Platform.isOpenBSD();
	}

	/**
	 * {@link InputStream} returned from the {@link DomainSocket}.
	 */
	private final class DomainSocketInputStream extends InputStream {

		/**
         * Reads a single byte of data from the underlying DomainSocket.
         * 
         * @return the byte read, or -1 if the end of the stream is reached
         * @throws IOException if an I/O error occurs
         */
        @Override
		public int read() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			int amountRead = DomainSocket.this.read(buffer);
			return (amountRead != 1) ? -1 : buffer.get() & 0xFF;
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
			if (len == 0) {
				return 0;
			}
			int amountRead = DomainSocket.this.read(ByteBuffer.wrap(b, off, len));
			return (amountRead > 0) ? amountRead : -1;
		}

	}

	/**
	 * {@link OutputStream} returned from the {@link DomainSocket}.
	 */
	private final class DomainSocketOutputStream extends OutputStream {

		/**
         * Writes a byte to the output stream.
         * 
         * @param b the byte to be written
         * @throws IOException if an I/O error occurs
         */
        @Override
		public void write(int b) throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			buffer.put(0, (byte) (b & 0xFF));
			DomainSocket.this.write(buffer);
		}

		/**
         * Writes a portion of an array of bytes to the output stream.
         * 
         * @param b the byte array from which the data is written
         * @param off the starting offset in the byte array
         * @param len the number of bytes to write
         * @throws IOException if an I/O error occurs
         */
        @Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len != 0) {
				DomainSocket.this.write(ByteBuffer.wrap(b, off, len));
			}
		}

	}

}
