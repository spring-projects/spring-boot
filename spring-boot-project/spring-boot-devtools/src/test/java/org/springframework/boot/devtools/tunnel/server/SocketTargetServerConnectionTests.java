/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.tunnel.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SocketTargetServerConnection}.
 *
 * @author Phillip Webb
 */
class SocketTargetServerConnectionTests {

	private static final int DEFAULT_TIMEOUT = 1000;

	private MockServer server;

	private SocketTargetServerConnection connection;

	@BeforeEach
	void setup() throws IOException {
		this.server = new MockServer();
		this.connection = new SocketTargetServerConnection(() -> this.server.getPort());
	}

	@Test
	void readData() throws Exception {
		this.server.willSend("hello".getBytes());
		this.server.start();
		ByteChannel channel = this.connection.open(DEFAULT_TIMEOUT);
		ByteBuffer buffer = ByteBuffer.allocate(5);
		channel.read(buffer);
		assertThat(buffer.array()).isEqualTo("hello".getBytes());
	}

	@Test
	void writeData() throws Exception {
		this.server.expect("hello".getBytes());
		this.server.start();
		ByteChannel channel = this.connection.open(DEFAULT_TIMEOUT);
		ByteBuffer buffer = ByteBuffer.wrap("hello".getBytes());
		channel.write(buffer);
		this.server.closeAndVerify();
	}

	@Test
	void timeout() throws Exception {
		this.server.delay(1000);
		this.server.start();
		ByteChannel channel = this.connection.open(10);
		long startTime = System.currentTimeMillis();
		assertThatExceptionOfType(SocketTimeoutException.class).isThrownBy(() -> channel.read(ByteBuffer.allocate(5)))
				.satisfies((ex) -> {
					long runTime = System.currentTimeMillis() - startTime;
					assertThat(runTime).isGreaterThanOrEqualTo(10L);
					assertThat(runTime).isLessThan(10000L);
				});
	}

	static class MockServer {

		private ServerSocketChannel serverSocket;

		private byte[] send;

		private byte[] expect;

		private int delay;

		private ByteBuffer actualRead;

		private ServerThread thread;

		MockServer() throws IOException {
			this.serverSocket = ServerSocketChannel.open();
			this.serverSocket.bind(new InetSocketAddress(0));
		}

		int getPort() {
			return this.serverSocket.socket().getLocalPort();
		}

		void delay(int delay) {
			this.delay = delay;
		}

		void willSend(byte[] send) {
			this.send = send;
		}

		void expect(byte[] expect) {
			this.expect = expect;
		}

		void start() {
			this.thread = new ServerThread();
			this.thread.start();
		}

		void closeAndVerify() throws InterruptedException {
			close();
			assertThat(this.actualRead.array()).isEqualTo(this.expect);
		}

		void close() throws InterruptedException {
			while (this.thread.isAlive()) {
				Thread.sleep(10);
			}
		}

		private class ServerThread extends Thread {

			@Override
			public void run() {
				try {
					SocketChannel channel = MockServer.this.serverSocket.accept();
					Thread.sleep(MockServer.this.delay);
					if (MockServer.this.send != null) {
						ByteBuffer buffer = ByteBuffer.wrap(MockServer.this.send);
						while (buffer.hasRemaining()) {
							channel.write(buffer);
						}
					}
					if (MockServer.this.expect != null) {
						ByteBuffer buffer = ByteBuffer.allocate(MockServer.this.expect.length);
						while (buffer.hasRemaining()) {
							channel.read(buffer);
						}
						MockServer.this.actualRead = buffer;
					}
					channel.close();
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

		}

	}

}
