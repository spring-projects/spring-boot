/*
 * Copyright 2012-2015 the original author or authors.
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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.devtools.tunnel.server.SocketTargetServerConnection;
import org.springframework.boot.devtools.tunnel.server.StaticPortProvider;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link SocketTargetServerConnection}.
 *
 * @author Phillip Webb
 */
public class SocketTargetServerConnectionTests {

	private static final int DEFAULT_TIMEOUT = 1000;

	private int port;

	private MockServer server;

	private SocketTargetServerConnection connection;

	@Before
	public void setup() throws IOException {
		this.port = SocketUtils.findAvailableTcpPort();
		this.server = new MockServer(this.port);
		StaticPortProvider portProvider = new StaticPortProvider(this.port);
		this.connection = new SocketTargetServerConnection(portProvider);
	}

	@Test
	public void readData() throws Exception {
		this.server.willSend("hello".getBytes());
		this.server.start();
		ByteChannel channel = this.connection.open(DEFAULT_TIMEOUT);
		ByteBuffer buffer = ByteBuffer.allocate(5);
		channel.read(buffer);
		assertThat(buffer.array(), equalTo("hello".getBytes()));
	}

	@Test
	public void writeData() throws Exception {
		this.server.expect("hello".getBytes());
		this.server.start();
		ByteChannel channel = this.connection.open(DEFAULT_TIMEOUT);
		ByteBuffer buffer = ByteBuffer.wrap("hello".getBytes());
		channel.write(buffer);
		this.server.closeAndVerify();
	}

	@Test
	public void timeout() throws Exception {
		this.server.delay(1000);
		this.server.start();
		ByteChannel channel = this.connection.open(10);
		long startTime = System.currentTimeMillis();
		try {
			channel.read(ByteBuffer.allocate(5));
			fail("No socket timeout thrown");
		}
		catch (SocketTimeoutException ex) {
			// Expected
			long runTime = System.currentTimeMillis() - startTime;
			assertThat(runTime, greaterThanOrEqualTo(10L));
			assertThat(runTime, lessThan(10000L));
		}
	}

	private static class MockServer {

		private ServerSocketChannel serverSocket;

		private byte[] send;

		private byte[] expect;

		private int delay;

		private ByteBuffer actualRead;

		private ServerThread thread;

		public MockServer(int port) throws IOException {
			this.serverSocket = ServerSocketChannel.open();
			this.serverSocket.bind(new InetSocketAddress(port));
		}

		public void delay(int delay) {
			this.delay = delay;
		}

		public void willSend(byte[] send) {
			this.send = send;
		}

		public void expect(byte[] expect) {
			this.expect = expect;
		}

		public void start() {
			this.thread = new ServerThread();
			this.thread.start();
		}

		public void closeAndVerify() throws InterruptedException {
			close();
			assertThat(this.actualRead.array(), equalTo(this.expect));
		}

		public void close() throws InterruptedException {
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
						ByteBuffer buffer = ByteBuffer
								.allocate(MockServer.this.expect.length);
						while (buffer.hasRemaining()) {
							channel.read(buffer);
						}
						MockServer.this.actualRead = buffer;
					}
					channel.close();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					fail();
				}
			}

		}

	}

}
