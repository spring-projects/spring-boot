/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.tunnel.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Socket based {@link TargetServerConnection}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class SocketTargetServerConnection implements TargetServerConnection {

	private static final Log logger = LogFactory
			.getLog(SocketTargetServerConnection.class);

	private final PortProvider portProvider;

	/**
	 * Create a new {@link SocketTargetServerConnection}.
	 * @param portProvider the port provider
	 */
	public SocketTargetServerConnection(PortProvider portProvider) {
		Assert.notNull(portProvider, "PortProvider must not be null");
		this.portProvider = portProvider;
	}

	@Override
	public ByteChannel open(int socketTimeout) throws IOException {
		SocketAddress address = new InetSocketAddress(this.portProvider.getPort());
		logger.trace("Opening tunnel connection to target server on " + address);
		SocketChannel channel = SocketChannel.open(address);
		channel.socket().setSoTimeout(socketTimeout);
		return new TimeoutAwareChannel(channel);
	}

	/**
	 * Wrapper to expose the {@link SocketChannel} in such a way that
	 * {@code SocketTimeoutExceptions} are still thrown from read methods.
	 */
	private static class TimeoutAwareChannel implements ByteChannel {

		private final SocketChannel socketChannel;

		private final ReadableByteChannel readChannel;

		TimeoutAwareChannel(SocketChannel socketChannel) throws IOException {
			this.socketChannel = socketChannel;
			this.readChannel = Channels
					.newChannel(socketChannel.socket().getInputStream());
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			return this.readChannel.read(dst);
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			return this.socketChannel.write(src);
		}

		@Override
		public boolean isOpen() {
			return this.socketChannel.isOpen();
		}

		@Override
		public void close() throws IOException {
			this.socketChannel.close();
		}

	}

}
