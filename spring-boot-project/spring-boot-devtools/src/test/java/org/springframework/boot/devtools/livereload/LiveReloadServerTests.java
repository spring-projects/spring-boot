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

package org.springframework.boot.devtools.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link LiveReloadServer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class LiveReloadServerTests {

	private static final String HANDSHAKE = "{command: 'hello', "
			+ "protocols: ['http://livereload.com/protocols/official-7']}";

	private int port;

	private MonitoredLiveReloadServer server;

	@BeforeEach
	void setUp() throws Exception {
		this.server = new MonitoredLiveReloadServer(0);
		this.port = this.server.start();
	}

	@AfterEach
	void tearDown() throws Exception {
		this.server.stop();
	}

	@Test
	@Disabled
	void servesLivereloadJs() throws Exception {
		RestTemplate template = new RestTemplate();
		URI uri = new URI("http://localhost:" + this.port + "/livereload.js");
		String script = template.getForObject(uri, String.class);
		assertThat(script).contains("livereload.com/protocols/official-7");
	}

	@Test
	void triggerReload() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		this.server.triggerReload();
		Thread.sleep(200);
		this.server.stop();
		assertThat(handler.getMessages().get(0)).contains("http://livereload.com/protocols/official-7");
		assertThat(handler.getMessages().get(1)).contains("command\":\"reload\"");
	}

	@Test
	void pingPong() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.sendMessage(new PingMessage());
		Thread.sleep(200);
		assertThat(handler.getPongCount()).isEqualTo(1);
		this.server.stop();
	}

	@Test
	void clientClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.close();
		awaitClosedException();
		assertThat(this.server.getClosedExceptions().size()).isGreaterThan(0);
	}

	private void awaitClosedException() throws InterruptedException {
		Awaitility.waitAtMost(Duration.ofSeconds(10)).until(this.server::getClosedExceptions, is(not(empty())));
	}

	@Test
	void serverClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		this.server.stop();
		Thread.sleep(200);
		assertThat(handler.getCloseStatus().getCode()).isEqualTo(1006);
	}

	private LiveReloadWebSocketHandler connect() throws Exception {
		WebSocketClient client = new StandardWebSocketClient(new WsWebSocketContainer());
		LiveReloadWebSocketHandler handler = new LiveReloadWebSocketHandler();
		client.doHandshake(handler, "ws://localhost:" + this.port + "/livereload");
		handler.awaitHello();
		return handler;
	}

	/**
	 * {@link LiveReloadServer} with additional monitoring.
	 */
	static class MonitoredLiveReloadServer extends LiveReloadServer {

		private final List<ConnectionClosedException> closedExceptions = new ArrayList<>();

		private final Object monitor = new Object();

		MonitoredLiveReloadServer(int port) {
			super(port);
		}

		@Override
		protected Connection createConnection(java.net.Socket socket, InputStream inputStream,
				OutputStream outputStream) throws IOException {
			return new MonitoredConnection(socket, inputStream, outputStream);
		}

		List<ConnectionClosedException> getClosedExceptions() {
			synchronized (this.monitor) {
				return new ArrayList<>(this.closedExceptions);
			}
		}

		private class MonitoredConnection extends Connection {

			MonitoredConnection(java.net.Socket socket, InputStream inputStream, OutputStream outputStream)
					throws IOException {
				super(socket, inputStream, outputStream);
			}

			@Override
			public void run() throws Exception {
				try {
					super.run();
				}
				catch (ConnectionClosedException ex) {
					synchronized (MonitoredLiveReloadServer.this.monitor) {
						MonitoredLiveReloadServer.this.closedExceptions.add(ex);
					}
					throw ex;
				}
			}

		}

	}

	static class LiveReloadWebSocketHandler extends TextWebSocketHandler {

		private WebSocketSession session;

		private final CountDownLatch helloLatch = new CountDownLatch(2);

		private final List<String> messages = new ArrayList<>();

		private int pongCount;

		private CloseStatus closeStatus;

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.session = session;
			session.sendMessage(new TextMessage(HANDSHAKE));
			this.helloLatch.countDown();
		}

		void awaitHello() throws InterruptedException {
			this.helloLatch.await(1, TimeUnit.MINUTES);
			Thread.sleep(200);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			if (message.getPayload().contains("hello")) {
				this.helloLatch.countDown();
			}
			this.messages.add(message.getPayload());
		}

		@Override
		protected void handlePongMessage(WebSocketSession session, PongMessage message) {
			this.pongCount++;
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			this.closeStatus = status;
		}

		void sendMessage(WebSocketMessage<?> message) throws IOException {
			this.session.sendMessage(message);
		}

		void close() throws IOException {
			this.session.close();
		}

		List<String> getMessages() {
			return this.messages;
		}

		int getPongCount() {
			return this.pongCount;
		}

		CloseStatus getCloseStatus() {
			return this.closeStatus;
		}

	}

}
