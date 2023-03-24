/*
 * Copyright 2012-2020 the original author or authors.
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

	private FileDescriptor open(String path) {
		int handle = socket(PF_LOCAL, SOCK_STREAM, 0);
		connect(path, handle);
		return new FileDescriptor(handle, this::close);
	}

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

	@Override
	public InputStream getInputStream() {
		return this.inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

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

	protected abstract void connect(String path, int handle);

	private native int socket(int domain, int type, int protocol) throws LastErrorException;

	private native int read(int fd, ByteBuffer buffer, int count) throws LastErrorException;

	private native int write(int fd, ByteBuffer buffer, int count) throws LastErrorException;

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

	private static boolean isBsdPlatform() {
		return Platform.isFreeBSD() || Platform.iskFreeBSD() || Platform.isNetBSD() || Platform.isOpenBSD();
	}

	/**
	 * {@link InputStream} returned from the {@link DomainSocket}.
	 */
	private class DomainSocketInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			int amountRead = DomainSocket.this.read(buffer);
			return (amountRead != 1) ? -1 : buffer.get() & 0xFF;
		}

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
	private class DomainSocketOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			buffer.put(0, (byte) (b & 0xFF));
			DomainSocket.this.write(buffer);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len != 0) {
				DomainSocket.this.write(ByteBuffer.wrap(b, off, len));
			}
		}

	}

}
