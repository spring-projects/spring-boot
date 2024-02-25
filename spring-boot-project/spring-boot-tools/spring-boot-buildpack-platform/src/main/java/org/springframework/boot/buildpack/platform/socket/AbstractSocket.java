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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Abstract base class for custom socket implementation.
 *
 * @author Phillip Webb
 */
class AbstractSocket extends Socket {

	/**
	 * Connects this socket to the specified endpoint.
	 * @param endpoint the endpoint to connect to
	 * @throws IOException if an I/O error occurs when connecting
	 */
	@Override
	public void connect(SocketAddress endpoint) throws IOException {
	}

	/**
	 * Connects this socket to the specified endpoint with the specified timeout.
	 * @param endpoint the endpoint to connect to
	 * @param timeout the timeout value in milliseconds
	 * @throws IOException if an I/O error occurs while connecting
	 */
	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
	}

	/**
	 * Returns a boolean value indicating whether the socket is currently connected.
	 * @return true if the socket is connected, false otherwise
	 */
	@Override
	public boolean isConnected() {
		return true;
	}

	/**
	 * Returns a boolean value indicating whether the socket is bound to a local address.
	 * @return {@code true} if the socket is bound to a local address, {@code false}
	 * otherwise.
	 */
	@Override
	public boolean isBound() {
		return true;
	}

	/**
	 * Closes the input stream for this socket.
	 * @throws IOException if an I/O error occurs when closing the input stream
	 * @throws UnsupportedSocketOperationException if the operation is not supported by
	 * the socket implementation
	 */
	@Override
	public void shutdownInput() throws IOException {
		throw new UnsupportedSocketOperationException();
	}

	/**
	 * Closes the output stream of this socket.
	 * <p>
	 * This method is not supported and will always throw an
	 * {@link UnsupportedSocketOperationException}.
	 * </p>
	 * @throws IOException if an I/O error occurs while closing the output stream
	 * @throws UnsupportedSocketOperationException if the operation is not supported
	 */
	@Override
	public void shutdownOutput() throws IOException {
		throw new UnsupportedSocketOperationException();
	}

	/**
	 * Returns the IP address of the remote endpoint to which this socket is connected.
	 * @return the IP address of the remote endpoint, or {@code null} if the socket is not
	 * connected
	 */
	@Override
	public InetAddress getInetAddress() {
		return null;
	}

	/**
	 * Returns the local IP address to which this socket is bound.
	 * @return the local IP address to which this socket is bound, or {@code null} if the
	 * socket is not bound
	 */
	@Override
	public InetAddress getLocalAddress() {
		return null;
	}

	/**
	 * Returns the local socket address to which this socket is bound, or {@code null} if
	 * it is not bound yet.
	 * @return the local socket address to which this socket is bound, or {@code null} if
	 * it is not bound yet.
	 */
	@Override
	public SocketAddress getLocalSocketAddress() {
		return null;
	}

	/**
	 * Returns the remote socket address to which this socket is connected.
	 * @return the remote socket address, or {@code null} if the socket is not connected
	 */
	@Override
	public SocketAddress getRemoteSocketAddress() {
		return null;
	}

	/**
	 * UnsupportedSocketOperationException class.
	 */
	private static class UnsupportedSocketOperationException extends UnsupportedOperationException {

		/**
		 * Constructs a new UnsupportedSocketOperationException with the default message
		 * "Unsupported socket operation".
		 */
		UnsupportedSocketOperationException() {
			super("Unsupported socket operation");
		}

	}

}
