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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/**
 * A {@link Socket} implementation for Unix domain sockets.
 *
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class UnixDomainSocket extends AbstractSocket {

	/**
	 * Create a new {@link Socket} for the given path.
	 * @param path the path to the domain socket
	 * @return a {@link Socket} instance
	 * @throws IOException if the socket cannot be opened
	 */
	public static Socket get(String path) throws IOException {
		return new UnixDomainSocket(path);
	}

	private final SocketAddress socketAddress;

	private final SocketChannel socketChannel;

	private UnixDomainSocket(String path) throws IOException {
		this.socketAddress = UnixDomainSocketAddress.of(path);
		this.socketChannel = SocketChannel.open(this.socketAddress);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}
		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}
		if (isInputShutdown()) {
			throw new SocketException("Socket input is shutdown");
		}

		return Channels.newInputStream(this.socketChannel);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}
		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}
		if (isOutputShutdown()) {
			throw new SocketException("Socket output is shutdown");
		}

		return Channels.newOutputStream(this.socketChannel);
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return this.socketAddress;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return this.socketAddress;
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.socketChannel.close();
	}

}
