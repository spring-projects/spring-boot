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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TunnelClient}.
 *
 * @author Phillip Webb
 */
public class TunnelClientTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private int listenPort = SocketUtils.findAvailableTcpPort();

	private MockTunnelConnection tunnelConnection = new MockTunnelConnection();

	@Test
	public void listenPortMustBePositive() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ListenPort must be positive");
		new TunnelClient(0, this.tunnelConnection);
	}

	@Test
	public void tunnelConnectionMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TunnelConnection must not be null");
		new TunnelClient(1, null);
	}

	@Test
	public void typicalTraffic() throws Exception {
		TunnelClient client = new TunnelClient(this.listenPort, this.tunnelConnection);
		client.start();
		SocketChannel channel = SocketChannel
				.open(new InetSocketAddress(this.listenPort));
		channel.write(ByteBuffer.wrap("hello".getBytes()));
		ByteBuffer buffer = ByteBuffer.allocate(5);
		channel.read(buffer);
		channel.close();
		this.tunnelConnection.verifyWritten("hello");
		assertThat(new String(buffer.array())).isEqualTo("olleh");
	}

	@Test
	public void socketChannelClosedTriggersTunnelClose() throws Exception {
		TunnelClient client = new TunnelClient(this.listenPort, this.tunnelConnection);
		client.start();
		SocketChannel channel = SocketChannel
				.open(new InetSocketAddress(this.listenPort));
		Thread.sleep(200);
		channel.close();
		client.getServerThread().stopAcceptingConnections();
		client.getServerThread().join(2000);
		assertThat(this.tunnelConnection.getOpenedTimes()).isEqualTo(1);
		assertThat(this.tunnelConnection.isOpen()).isFalse();
	}

	@Test
	public void stopTriggersTunnelClose() throws Exception {
		TunnelClient client = new TunnelClient(this.listenPort, this.tunnelConnection);
		client.start();
		SocketChannel channel = SocketChannel
				.open(new InetSocketAddress(this.listenPort));
		Thread.sleep(200);
		client.stop();
		assertThat(this.tunnelConnection.getOpenedTimes()).isEqualTo(1);
		assertThat(this.tunnelConnection.isOpen()).isFalse();
		assertThat(channel.read(ByteBuffer.allocate(1))).isEqualTo(-1);
	}

	@Test
	public void addListener() throws Exception {
		TunnelClient client = new TunnelClient(this.listenPort, this.tunnelConnection);
		TunnelClientListener listener = mock(TunnelClientListener.class);
		client.addListener(listener);
		client.start();
		SocketChannel channel = SocketChannel
				.open(new InetSocketAddress(this.listenPort));
		Thread.sleep(200);
		channel.close();
		client.getServerThread().stopAcceptingConnections();
		client.getServerThread().join(2000);
		verify(listener).onOpen(any(SocketChannel.class));
		verify(listener).onClose(any(SocketChannel.class));
	}

	private static class MockTunnelConnection implements TunnelConnection {

		private final ByteArrayOutputStream written = new ByteArrayOutputStream();

		private boolean open;

		private int openedTimes;

		@Override
		public WritableByteChannel open(WritableByteChannel incomingChannel,
				Closeable closeable) throws Exception {
			this.openedTimes++;
			this.open = true;
			return new TunnelChannel(incomingChannel, closeable);
		}

		public void verifyWritten(String expected) {
			verifyWritten(expected.getBytes());
		}

		public void verifyWritten(byte[] expected) {
			synchronized (this.written) {
				assertThat(this.written.toByteArray()).isEqualTo(expected);
				this.written.reset();
			}
		}

		public boolean isOpen() {
			return this.open;
		}

		public int getOpenedTimes() {
			return this.openedTimes;
		}

		private class TunnelChannel implements WritableByteChannel {

			private final WritableByteChannel incomingChannel;

			private final Closeable closeable;

			TunnelChannel(WritableByteChannel incomingChannel, Closeable closeable) {
				this.incomingChannel = incomingChannel;
				this.closeable = closeable;
			}

			@Override
			public boolean isOpen() {
				return MockTunnelConnection.this.open;
			}

			@Override
			public void close() throws IOException {
				MockTunnelConnection.this.open = false;
				this.closeable.close();
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				int remaining = src.remaining();
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				Channels.newChannel(stream).write(src);
				byte[] bytes = stream.toByteArray();
				synchronized (MockTunnelConnection.this.written) {
					MockTunnelConnection.this.written.write(bytes);
				}
				byte[] reversed = new byte[bytes.length];
				for (int i = 0; i < reversed.length; i++) {
					reversed[i] = bytes[bytes.length - 1 - i];
				}
				this.incomingChannel.write(ByteBuffer.wrap(reversed));
				return remaining;
			}

		}

	}

}
