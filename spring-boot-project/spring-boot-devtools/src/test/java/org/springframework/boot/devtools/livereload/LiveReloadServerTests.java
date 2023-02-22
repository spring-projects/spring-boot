/*
 * Copyright 2012-2023 the original author or authors.
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.WebSocketContainer;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
		List<String> messages = await().atMost(Duration.ofSeconds(10))
			.until(handler::getMessages, (msgs) -> msgs.size() == 2);
		assertThat(messages.get(0)).contains("http://livereload.com/protocols/official-7");
		assertThat(messages.get(1)).contains("command\":\"reload\"");
	}

	@Test // gh-26813
	void triggerReloadWithUppercaseHeaders() throws Exception {
		LiveReloadWebSocketHandler handler = connect(UppercaseWebSocketClient::new);
		this.server.triggerReload();
		List<String> messages = await().atMost(Duration.ofSeconds(10))
			.until(handler::getMessages, (msgs) -> msgs.size() == 2);
		assertThat(messages.get(0)).contains("http://livereload.com/protocols/official-7");
		assertThat(messages.get(1)).contains("command\":\"reload\"");
	}

	@Test
	void pingPong() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.sendMessage(new PingMessage());
		await().atMost(Duration.ofSeconds(10)).until(handler::getPongCount, is(1));
	}

	@Test
	void clientClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.close();
		awaitClosedException();
		assertThat(this.server.getClosedExceptions()).isNotEmpty();
	}

	private void awaitClosedException() {
		Awaitility.waitAtMost(Duration.ofSeconds(10)).until(this.server::getClosedExceptions, is(not(empty())));
	}

	@Test
	void serverClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		this.server.stop();
		CloseStatus closeStatus = await().atMost(Duration.ofSeconds(10))
			.until(handler::getCloseStatus, Objects::nonNull);
		assertThat(closeStatus.getCode()).isEqualTo(1006);
	}

	private LiveReloadWebSocketHandler connect() throws Exception {
		return connect(StandardWebSocketClient::new);
	}

	private LiveReloadWebSocketHandler connect(Function<WebSocketContainer, WebSocketClient> clientFactory)
			throws Exception {
		WsWebSocketContainer webSocketContainer = new WsWebSocketContainer();
		WebSocketClient client = clientFactory.apply(webSocketContainer);
		LiveReloadWebSocketHandler handler = new LiveReloadWebSocketHandler();
		client.execute(handler, "ws://localhost:" + this.port + "/livereload");
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

	class LiveReloadWebSocketHandler extends TextWebSocketHandler {

		private volatile WebSocketSession session;

		private final CountDownLatch helloLatch = new CountDownLatch(2);

		private final List<String> messages = new CopyOnWriteArrayList<>();

		private final AtomicInteger pongCount = new AtomicInteger();

		private volatile CloseStatus closeStatus;

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.session = session;
			session.sendMessage(new TextMessage(HANDSHAKE));
			this.helloLatch.countDown();
		}

		void awaitHello() throws InterruptedException {
			this.helloLatch.await(1, TimeUnit.MINUTES);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			String payload = message.getPayload();
			this.messages.add(payload);
			if (payload.contains("hello")) {
				this.helloLatch.countDown();
			}
		}

		@Override
		protected void handlePongMessage(WebSocketSession session, PongMessage message) {
			this.pongCount.incrementAndGet();
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
			return this.pongCount.get();
		}

		CloseStatus getCloseStatus() {
			return this.closeStatus;
		}

	}

	static class UppercaseWebSocketClient extends StandardWebSocketClient {

		private final WebSocketContainer webSocketContainer;

		UppercaseWebSocketClient(WebSocketContainer webSocketContainer) {
			super(webSocketContainer);
			this.webSocketContainer = webSocketContainer;
		}

		@Override
		protected CompletableFuture<WebSocketSession> executeInternal(WebSocketHandler webSocketHandler,
				HttpHeaders headers, URI uri, List<String> protocols, List<WebSocketExtension> extensions,
				Map<String, Object> attributes) {
			InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), uri.getPort());
			InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
			StandardWebSocketSession session = new StandardWebSocketSession(headers, attributes, localAddress,
					remoteAddress);
			Stream<Extension> adaptedExtensions = extensions.stream().map(WebSocketToStandardExtensionAdapter::new);
			ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
				.configurator(new UppercaseWebSocketClientConfigurator(headers))
				.preferredSubprotocols(protocols)
				.extensions(adaptedExtensions.toList())
				.build();
			endpointConfig.getUserProperties().putAll(getUserProperties());
			Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);
			Callable<WebSocketSession> connectTask = () -> {
				this.webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
				return session;
			};
			return getTaskExecutor().submitCompletable(connectTask);
		}

		private InetAddress getLocalHost() {
			try {
				return InetAddress.getLocalHost();
			}
			catch (UnknownHostException ex) {
				return InetAddress.getLoopbackAddress();
			}
		}

	}

	private static class UppercaseWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders headers;

		UppercaseWebSocketClientConfigurator(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			Map<String, List<String>> uppercaseRequestHeaders = new LinkedHashMap<>();
			requestHeaders.forEach((key, value) -> uppercaseRequestHeaders.put(key.toUpperCase(), value));
			requestHeaders.clear();
			requestHeaders.putAll(uppercaseRequestHeaders);
			requestHeaders.putAll(this.headers);
		}

		@Override
		public void afterResponse(HandshakeResponse response) {
		}

	}

}
