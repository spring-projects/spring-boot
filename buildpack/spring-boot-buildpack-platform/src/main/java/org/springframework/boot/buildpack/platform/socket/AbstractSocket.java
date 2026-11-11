/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.jspecify.annotations.Nullable;

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
	public @Nullable InetAddress getInetAddress() {
		return null;
	}

	@Override
	public @Nullable InetAddress getLocalAddress() {
		return null;
	}

	@Override
	public @Nullable SocketAddress getLocalSocketAddress() {
		return null;
	}

	@Override
	public @Nullable SocketAddress getRemoteSocketAddress() {
		return null;
	}

	private static class UnsupportedSocketOperationException extends UnsupportedOperationException {

		UnsupportedSocketOperationException() {
			super("Unsupported socket operation");
		}

	}

}
