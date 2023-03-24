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

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public boolean isBound() {
		return true;
	}

	@Override
	public void shutdownInput() throws IOException {
		throw new UnsupportedSocketOperationException();
	}

	@Override
	public void shutdownOutput() throws IOException {
		throw new UnsupportedSocketOperationException();
	}

	@Override
	public InetAddress getInetAddress() {
		return null;
	}

	@Override
	public InetAddress getLocalAddress() {
		return null;
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return null;
	}

	private static class UnsupportedSocketOperationException extends UnsupportedOperationException {

		UnsupportedSocketOperationException() {
			super("Unsupported socket operation");
		}

	}

}
