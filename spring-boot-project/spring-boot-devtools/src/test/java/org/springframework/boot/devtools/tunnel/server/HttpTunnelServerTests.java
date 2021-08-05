/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.devtools.tunnel.payload.HttpTunnelPayload;
import org.springframework.boot.devtools.tunnel.server.HttpTunnelServer.HttpConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpTunnelServer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class HttpTunnelServerTests {

	private static final int DEFAULT_LONG_POLL_TIMEOUT = 10000;

	private static final byte[] NO_DATA = {};

	private static final String SEQ_HEADER = "x-seq";

	private HttpTunnelServer server;

	@Mock
	private TargetServerConnection serverConnection;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServerHttpRequest request;

	private ServerHttpResponse response;

	private MockServerChannel serverChannel;

	@BeforeEach
	void setup() {
		this.server = new HttpTunnelServer(this.serverConnection);
		this.servletRequest = new MockHttpServletRequest();
		this.servletRequest.setAsyncSupported(true);
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletServerHttpRequest(this.servletRequest);
		this.response = new ServletServerHttpResponse(this.servletResponse);
		this.serverChannel = new MockServerChannel();
	}

	@Test
	void serverConnectionIsRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpTunnelServer(null))
				.withMessageContaining("ServerConnection must not be null");
	}

	@Test
	void serverConnectedOnFirstRequest() throws Exception {
		verify(this.serverConnection, never()).open(anyInt());
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.server.handle(this.request, this.response);
		verify(this.serverConnection, times(1)).open(DEFAULT_LONG_POLL_TIMEOUT);
	}

	@Test
	void longPollTimeout() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.server.setLongPollTimeout(800);
		this.server.handle(this.request, this.response);
		verify(this.serverConnection, times(1)).open(800);
	}

	@Test
	void longPollTimeoutMustBePositiveValue() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.server.setLongPollTimeout(0))
				.withMessageContaining("LongPollTimeout must be a positive value");
	}

	@Test
	void initialRequestIsSentToServer() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.servletRequest.addHeader(SEQ_HEADER, "1");
		this.servletRequest.setContent("hello".getBytes());
		this.server.handle(this.request, this.response);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
		this.serverChannel.verifyReceived("hello");
	}

	@Test
	void initialRequestIsUsedForFirstServerResponse() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.servletRequest.addHeader(SEQ_HEADER, "1");
		this.servletRequest.setContent("hello".getBytes());
		this.server.handle(this.request, this.response);
		System.out.println("sending");
		this.serverChannel.send("hello");
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("hello");
		this.serverChannel.verifyReceived("hello");
	}

	@Test
	void initialRequestHasNoPayload() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.server.handle(this.request, this.response);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
		this.serverChannel.verifyReceived(NO_DATA);
	}

	@Test
	void typicalRequestResponseTraffic() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		MockHttpConnection h1 = new MockHttpConnection();
		this.server.handle(h1);
		MockHttpConnection h2 = new MockHttpConnection("hello server", 1);
		this.server.handle(h2);
		this.serverChannel.verifyReceived("hello server");
		this.serverChannel.send("hello client");
		h1.verifyReceived("hello client", 1);
		MockHttpConnection h3 = new MockHttpConnection("1+1", 2);
		this.server.handle(h3);
		this.serverChannel.send("=2");
		h2.verifyReceived("=2", 2);
		MockHttpConnection h4 = new MockHttpConnection("1+2", 3);
		this.server.handle(h4);
		this.serverChannel.send("=3");
		h3.verifyReceived("=3", 3);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
	}

	@Test
	void clientIsAwareOfServerClose() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		MockHttpConnection h1 = new MockHttpConnection("1", 1);
		this.server.handle(h1);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
		assertThat(h1.getServletResponse().getStatus()).isEqualTo(410);
	}

	@Test
	void clientCanCloseServer() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		MockHttpConnection h1 = new MockHttpConnection();
		this.server.handle(h1);
		MockHttpConnection h2 = new MockHttpConnection("DISCONNECT", 1);
		h2.getServletRequest().addHeader("Content-Type", "application/x-disconnect");
		this.server.handle(h2);
		this.server.getServerThread().join();
		assertThat(h1.getServletResponse().getStatus()).isEqualTo(410);
		assertThat(this.serverChannel.isOpen()).isFalse();
	}

	@Test
	void neverMoreThanTwoHttpConnections() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		MockHttpConnection h1 = new MockHttpConnection();
		this.server.handle(h1);
		MockHttpConnection h2 = new MockHttpConnection("1", 2);
		this.server.handle(h2);
		MockHttpConnection h3 = new MockHttpConnection("2", 3);
		this.server.handle(h3);
		h1.waitForResponse();
		assertThat(h1.getServletResponse().getStatus()).isEqualTo(429);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
	}

	@Test
	void requestReceivedOutOfOrder() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		MockHttpConnection h1 = new MockHttpConnection();
		MockHttpConnection h2 = new MockHttpConnection("1+2", 1);
		MockHttpConnection h3 = new MockHttpConnection("+3", 2);
		this.server.handle(h1);
		this.server.handle(h3);
		this.server.handle(h2);
		this.serverChannel.verifyReceived("1+2+3");
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
	}

	@Test
	void httpConnectionsAreClosedAfterLongPollTimeout() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.server.setDisconnectTimeout(1000);
		this.server.setLongPollTimeout(100);
		MockHttpConnection h1 = new MockHttpConnection();
		this.server.handle(h1);
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(h1.getServletResponse()::getStatus,
				(status) -> status == 204);
		MockHttpConnection h2 = new MockHttpConnection();
		this.server.handle(h2);
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(h2.getServletResponse()::getStatus,
				(status) -> status == 204);
		this.serverChannel.disconnect();
		this.server.getServerThread().join();
	}

	@Test
	void disconnectTimeout() throws Exception {
		givenServerConnectionOpenWillAnswerWithServerChannel();
		this.server.setDisconnectTimeout(100);
		this.server.setLongPollTimeout(100);
		MockHttpConnection h1 = new MockHttpConnection();
		this.server.handle(h1);
		this.serverChannel.send("hello");
		this.server.getServerThread().join();
		assertThat(this.serverChannel.isOpen()).isFalse();
	}

	@Test
	void disconnectTimeoutMustBePositive() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.server.setDisconnectTimeout(0))
				.withMessageContaining("DisconnectTimeout must be a positive value");
	}

	@Test
	void httpConnectionRespondWithPayload() throws Exception {
		HttpConnection connection = new HttpConnection(this.request, this.response);
		connection.waitForResponse();
		connection.respond(new HttpTunnelPayload(1, ByteBuffer.wrap("hello".getBytes())));
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("hello");
		assertThat(this.servletResponse.getHeader(SEQ_HEADER)).isEqualTo("1");
	}

	@Test
	void httpConnectionRespondWithStatus() throws Exception {
		HttpConnection connection = new HttpConnection(this.request, this.response);
		connection.waitForResponse();
		connection.respond(HttpStatus.I_AM_A_TEAPOT);
		assertThat(this.servletResponse.getStatus()).isEqualTo(418);
		assertThat(this.servletResponse.getContentLength()).isEqualTo(0);
	}

	@Test
	void httpConnectionAsync() throws Exception {
		ServerHttpAsyncRequestControl async = mock(ServerHttpAsyncRequestControl.class);
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		given(request.getAsyncRequestControl(this.response)).willReturn(async);
		HttpConnection connection = new HttpConnection(request, this.response);
		connection.waitForResponse();
		verify(async).start();
		connection.respond(HttpStatus.NO_CONTENT);
		verify(async).complete();
	}

	@Test
	void httpConnectionNonAsync() throws Exception {
		testHttpConnectionNonAsync(0);
		testHttpConnectionNonAsync(100);
	}

	private void testHttpConnectionNonAsync(long sleepBeforeResponse) throws IOException, InterruptedException {
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		given(request.getAsyncRequestControl(this.response)).willThrow(new IllegalArgumentException());
		final HttpConnection connection = new HttpConnection(request, this.response);
		final AtomicBoolean responded = new AtomicBoolean();
		Thread connectionThread = new Thread(() -> {
			connection.waitForResponse();
			responded.set(true);
		});
		connectionThread.start();
		assertThat(responded.get()).isFalse();
		Thread.sleep(sleepBeforeResponse);
		connection.respond(HttpStatus.NO_CONTENT);
		connectionThread.join();
		assertThat(responded.get()).isTrue();
	}

	@Test
	void httpConnectionRunning() throws Exception {
		HttpConnection connection = new HttpConnection(this.request, this.response);
		assertThat(connection.isOlderThan(100)).isFalse();
		Thread.sleep(200);
		assertThat(connection.isOlderThan(100)).isTrue();
	}

	private void givenServerConnectionOpenWillAnswerWithServerChannel() throws IOException {
		given(this.serverConnection.open(anyInt())).willAnswer((invocation) -> {
			MockServerChannel channel = HttpTunnelServerTests.this.serverChannel;
			channel.setTimeout(invocation.getArgument(0));
			return channel;
		});
	}

	/**
	 * Mock {@link ByteChannel} used to simulate the server connection.
	 */
	static class MockServerChannel implements ByteChannel {

		private static final ByteBuffer DISCONNECT = ByteBuffer.wrap(NO_DATA);

		private int timeout;

		private BlockingDeque<ByteBuffer> outgoing = new LinkedBlockingDeque<>();

		private ByteArrayOutputStream written = new ByteArrayOutputStream();

		private AtomicBoolean open = new AtomicBoolean(true);

		void setTimeout(int timeout) {
			this.timeout = timeout;
		}

		void send(String content) {
			send(content.getBytes());
		}

		void send(byte[] bytes) {
			this.outgoing.addLast(ByteBuffer.wrap(bytes));
		}

		void disconnect() {
			this.outgoing.addLast(DISCONNECT);
		}

		void verifyReceived(String expected) {
			verifyReceived(expected.getBytes());
		}

		void verifyReceived(byte[] expected) {
			synchronized (this.written) {
				assertThat(this.written.toByteArray()).isEqualTo(expected);
				this.written.reset();
			}
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			try {
				ByteBuffer bytes = this.outgoing.pollFirst(this.timeout, TimeUnit.MILLISECONDS);
				if (bytes == null) {
					throw new SocketTimeoutException();
				}
				if (bytes == DISCONNECT) {
					this.open.set(false);
					return -1;
				}
				int initialRemaining = dst.remaining();
				bytes.limit(Math.min(bytes.limit(), initialRemaining));
				dst.put(bytes);
				bytes.limit(bytes.capacity());
				return initialRemaining - dst.remaining();
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			int remaining = src.remaining();
			synchronized (this.written) {
				Channels.newChannel(this.written).write(src);
			}
			return remaining;
		}

		@Override
		public boolean isOpen() {
			return this.open.get();
		}

		@Override
		public void close() {
			this.open.set(false);
		}

	}

	/**
	 * Mock {@link HttpConnection}.
	 */
	static class MockHttpConnection extends HttpConnection {

		MockHttpConnection() {
			super(new ServletServerHttpRequest(new MockHttpServletRequest()),
					new ServletServerHttpResponse(new MockHttpServletResponse()));
		}

		MockHttpConnection(String content, int seq) {
			this();
			MockHttpServletRequest request = getServletRequest();
			request.setContent(content.getBytes());
			request.addHeader(SEQ_HEADER, String.valueOf(seq));
		}

		@Override
		protected ServerHttpAsyncRequestControl startAsync() {
			getServletRequest().setAsyncSupported(true);
			return super.startAsync();
		}

		@Override
		protected void complete() {
			super.complete();
			getServletResponse().setCommitted(true);
		}

		MockHttpServletRequest getServletRequest() {
			return (MockHttpServletRequest) ((ServletServerHttpRequest) getRequest()).getServletRequest();
		}

		MockHttpServletResponse getServletResponse() {
			return (MockHttpServletResponse) ((ServletServerHttpResponse) getResponse()).getServletResponse();
		}

		void verifyReceived(String expectedContent, int expectedSeq) throws Exception {
			waitForServletResponse();
			MockHttpServletResponse resp = getServletResponse();
			assertThat(resp.getContentAsString()).isEqualTo(expectedContent);
			assertThat(resp.getHeader(SEQ_HEADER)).isEqualTo(String.valueOf(expectedSeq));
		}

		void waitForServletResponse() throws InterruptedException {
			while (!getServletResponse().isCommitted()) {
				Thread.sleep(10);
			}
		}

	}

}
