/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.tunnel.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

/**
 * The client side component of a socket tunnel. Starts a {@link ServerSocket} of the
 * specified port for local clients to connect to.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class TunnelClient implements SmartInitializingSingleton {

	private static final int BUFFER_SIZE = 1024 * 100;

	private static final Log logger = LogFactory.getLog(TunnelClient.class);

	private final TunnelClientListeners listeners = new TunnelClientListeners();

	private final Object monitor = new Object();

	private final int listenPort;

	private final TunnelConnection tunnelConnection;

	private ServerThread serverThread;

	public TunnelClient(int listenPort, TunnelConnection tunnelConnection) {
		Assert.isTrue(listenPort > 0, "ListenPort must be positive");
		Assert.notNull(tunnelConnection, "TunnelConnection must not be null");
		this.listenPort = listenPort;
		this.tunnelConnection = tunnelConnection;
	}

	@Override
	public void afterSingletonsInstantiated() {
		synchronized (this.monitor) {
			if (this.serverThread == null) {
				try {
					start();
				}
				catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
			}
		}
	}

	/**
	 * Start the client and accept incoming connections on the port.
	 * @throws IOException in case of I/O errors
	 */
	public void start() throws IOException {
		synchronized (this.monitor) {
			Assert.state(this.serverThread == null, "Server already started");
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(this.listenPort));
			logger.trace(
					"Listening for TCP traffic to tunnel on port " + this.listenPort);
			this.serverThread = new ServerThread(serverSocketChannel);
			this.serverThread.start();
		}
	}

	/**
	 * Stop the client, disconnecting any servers.
	 * @throws IOException in case of I/O errors
	 */
	public void stop() throws IOException {
		synchronized (this.monitor) {
			if (this.serverThread != null) {
				logger.trace("Closing tunnel client on port " + this.listenPort);
				this.serverThread.close();
				try {
					this.serverThread.join(2000);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				this.serverThread = null;
			}
		}
	}

	protected final ServerThread getServerThread() {
		synchronized (this.monitor) {
			return this.serverThread;
		}
	}

	public void addListener(TunnelClientListener listener) {
		this.listeners.addListener(listener);
	}

	public void removeListener(TunnelClientListener listener) {
		this.listeners.removeListener(listener);
	}

	/**
	 * The main server thread.
	 */
	protected class ServerThread extends Thread {

		private final ServerSocketChannel serverSocketChannel;

		private boolean acceptConnections = true;

		public ServerThread(ServerSocketChannel serverSocketChannel) {
			this.serverSocketChannel = serverSocketChannel;
			setName("Tunnel Server");
			setDaemon(true);
		}

		public void close() throws IOException {
			this.serverSocketChannel.close();
			this.acceptConnections = false;
			interrupt();
		}

		@Override
		public void run() {
			try {
				while (this.acceptConnections) {
					SocketChannel socket = this.serverSocketChannel.accept();
					try {
						handleConnection(socket);
					}
					catch (AsynchronousCloseException ex) {
						// Connection has been closed. Keep the server running
					}
					finally {
						socket.close();
					}
				}
			}
			catch (Exception ex) {
				logger.trace("Unexpected exception from tunnel client", ex);
			}
		}

		private void handleConnection(SocketChannel socketChannel) throws Exception {
			Closeable closeable = new SocketCloseable(socketChannel);
			WritableByteChannel outputChannel = TunnelClient.this.tunnelConnection
					.open(socketChannel, closeable);
			TunnelClient.this.listeners.fireOpenEvent(socketChannel);
			try {
				logger.trace("Accepted connection to tunnel client from "
						+ socketChannel.socket().getRemoteSocketAddress());
				while (true) {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					int amountRead = socketChannel.read(buffer);
					if (amountRead == -1) {
						outputChannel.close();
						return;
					}
					if (amountRead > 0) {
						buffer.flip();
						outputChannel.write(buffer);
					}
				}
			}
			finally {
				outputChannel.close();
			}
		}

		protected void stopAcceptingConnections() {
			this.acceptConnections = false;
		}
	}

	/**
	 * {@link Closeable} used to close a {@link SocketChannel} and fire an event.
	 */
	private class SocketCloseable implements Closeable {

		private final SocketChannel socketChannel;

		private boolean closed = false;

		SocketCloseable(SocketChannel socketChannel) {
			this.socketChannel = socketChannel;
		}

		@Override
		public void close() throws IOException {
			if (!this.closed) {
				this.socketChannel.close();
				TunnelClient.this.listeners.fireCloseEvent(this.socketChannel);
				this.closed = true;
			}
		}
	}
}
