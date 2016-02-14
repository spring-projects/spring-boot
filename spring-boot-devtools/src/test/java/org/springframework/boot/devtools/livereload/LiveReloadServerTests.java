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

package org.springframework.boot.devtools.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.events.JettyListenerEventDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link LiveReloadServer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LiveReloadServerTests {

	private static final String HANDSHAKE = "{command: 'hello', "
			+ "protocols: ['http://livereload.com/protocols/official-7']}";

	private static final ByteBuffer NO_DATA = ByteBuffer.allocate(0);

	private int port = SocketUtils.findAvailableTcpPort();

	private MonitoredLiveReloadServer server;

	@Before
	public void setUp() throws Exception {
		this.server = new MonitoredLiveReloadServer(this.port);
		this.server.start();
	}

	@After
	public void tearDown() throws Exception {
		this.server.stop();
	}

	@Test
	public void servesLivereloadJs() throws Exception {
		RestTemplate template = new RestTemplate();
		URI uri = new URI("http://localhost:" + this.port + "/livereload.js");
		String script = template.getForObject(uri, String.class);
		assertThat(script, containsString("livereload.com/protocols/official-7"));
	}

	@Test
	public void triggerReload() throws Exception {
		WebSocketClient client = new WebSocketClient();
		try {
			Socket socket = openSocket(client, new Socket());
			this.server.triggerReload();
			Thread.sleep(500);
			this.server.stop();
			assertThat(socket.getMessages(0),
					containsString("http://livereload.com/protocols/official-7"));
			assertThat(socket.getMessages(1), containsString("command\":\"reload\""));
		}
		finally {
			client.stop();
		}
	}

	@Test
	public void pingPong() throws Exception {
		WebSocketClient client = new WebSocketClient();
		try {
			Socket socket = new Socket();
			Driver driver = openSocket(client, new Driver(socket));
			socket.getRemote().sendPing(NO_DATA);
			Thread.sleep(200);
			this.server.stop();
			assertThat(driver.getPongCount(), equalTo(1));
		}
		finally {
			client.stop();
		}
	}

	@Test
	public void clientClose() throws Exception {
		WebSocketClient client = new WebSocketClient();
		try {
			Socket socket = openSocket(client, new Socket());
			socket.getSession().close();
		}
		finally {
			client.stop();
		}
		awaitClosedException();
		assertThat(this.server.getClosedExceptions().size(), greaterThan(0));
	}

	private void awaitClosedException() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while (this.server.getClosedExceptions().isEmpty()
				&& System.currentTimeMillis() - startTime < 10000) {
			Thread.sleep(100);
		}
	}

	@Test
	public void serverClose() throws Exception {
		WebSocketClient client = new WebSocketClient();
		try {
			Socket socket = openSocket(client, new Socket());
			Thread.sleep(200);
			this.server.stop();
			Thread.sleep(200);
			assertThat(socket.getCloseStatus(), equalTo(1006));
		}
		finally {
			client.stop();
		}
	}

	private <T> T openSocket(WebSocketClient client, T socket) throws Exception,
			URISyntaxException, InterruptedException, ExecutionException, IOException {
		client.start();
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		URI uri = new URI("ws://localhost:" + this.port + "/livereload");
		Session session = client.connect(socket, uri, request).get();
		session.getRemote().sendString(HANDSHAKE);
		Thread.sleep(200);
		return socket;
	}

	/**
	 * Useful main method for manual testing against a real browser.
	 * @param args main args
	 * @throws IOException in case of I/O errors
	 */
	public static void main(String[] args) throws IOException {
		LiveReloadServer server = new LiveReloadServer();
		server.start();
		while (true) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			server.triggerReload();
		}
	}

	private static class Driver extends JettyListenerEventDriver {

		private int pongCount;

		Driver(WebSocketListener listener) {
			super(WebSocketPolicy.newClientPolicy(), listener);
		}

		@Override
		public void onPong(ByteBuffer buffer) {
			super.onPong(buffer);
			this.pongCount++;
		}

		public int getPongCount() {
			return this.pongCount;
		}

	}

	private static class Socket extends WebSocketAdapter {

		private List<String> messages = new ArrayList<String>();

		private Integer closeStatus;

		@Override
		public void onWebSocketText(String message) {
			this.messages.add(message);
		}

		public String getMessages(int index) {
			return this.messages.get(index);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			this.closeStatus = statusCode;
		}

		public Integer getCloseStatus() {
			return this.closeStatus;
		}

	}

	/**
	 * {@link LiveReloadServer} with additional monitoring.
	 */
	private static class MonitoredLiveReloadServer extends LiveReloadServer {

		private final List<ConnectionClosedException> closedExceptions = new ArrayList<ConnectionClosedException>();

		private final Object monitor = new Object();

		MonitoredLiveReloadServer(int port) {
			super(port);
		}

		@Override
		protected Connection createConnection(java.net.Socket socket,
				InputStream inputStream, OutputStream outputStream) throws IOException {
			return new MonitoredConnection(socket, inputStream, outputStream);
		}

		public List<ConnectionClosedException> getClosedExceptions() {
			synchronized (this.monitor) {
				return new ArrayList<ConnectionClosedException>(this.closedExceptions);
			}
		}

		private class MonitoredConnection extends Connection {

			MonitoredConnection(java.net.Socket socket, InputStream inputStream,
					OutputStream outputStream) throws IOException {
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

}
