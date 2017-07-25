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

package org.springframework.boot.devtools.tunnel.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.tunnel.payload.HttpTunnelPayload;
import org.springframework.boot.devtools.tunnel.payload.HttpTunnelPayloadForwarder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * {@link TunnelConnection} implementation that uses HTTP to transfer data.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see TunnelClient
 * @see org.springframework.boot.devtools.tunnel.server.HttpTunnelServer
 */
public class HttpTunnelConnection implements TunnelConnection {

	private static final Log logger = LogFactory.getLog(HttpTunnelConnection.class);

	private final URI uri;

	private final ClientHttpRequestFactory requestFactory;

	private final Executor executor;

	/**
	 * Create a new {@link HttpTunnelConnection} instance.
	 * @param url the URL to connect to
	 * @param requestFactory the HTTP request factory
	 */
	public HttpTunnelConnection(String url, ClientHttpRequestFactory requestFactory) {
		this(url, requestFactory, null);
	}

	/**
	 * Create a new {@link HttpTunnelConnection} instance.
	 * @param url the URL to connect to
	 * @param requestFactory the HTTP request factory
	 * @param executor the executor used to handle connections
	 */
	protected HttpTunnelConnection(String url, ClientHttpRequestFactory requestFactory,
			Executor executor) {
		Assert.hasLength(url, "URL must not be empty");
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		try {
			this.uri = new URL(url).toURI();
		}
		catch (URISyntaxException | MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL '" + url + "'");
		}
		this.requestFactory = requestFactory;
		this.executor = (executor == null
				? Executors.newCachedThreadPool(new TunnelThreadFactory()) : executor);
	}

	@Override
	public TunnelChannel open(WritableByteChannel incomingChannel, Closeable closeable)
			throws Exception {
		logger.trace("Opening HTTP tunnel to " + this.uri);
		return new TunnelChannel(incomingChannel, closeable);
	}

	protected final ClientHttpRequest createRequest(boolean hasPayload)
			throws IOException {
		HttpMethod method = (hasPayload ? HttpMethod.POST : HttpMethod.GET);
		return this.requestFactory.createRequest(this.uri, method);
	}

	/**
	 * A {@link WritableByteChannel} used to transfer traffic.
	 */
	protected class TunnelChannel implements WritableByteChannel {

		private final HttpTunnelPayloadForwarder forwarder;

		private final Closeable closeable;

		private boolean open = true;

		private AtomicLong requestSeq = new AtomicLong();

		public TunnelChannel(WritableByteChannel incomingChannel, Closeable closeable) {
			this.forwarder = new HttpTunnelPayloadForwarder(incomingChannel);
			this.closeable = closeable;
			openNewConnection(null);
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		@Override
		public void close() throws IOException {
			if (this.open) {
				this.open = false;
				this.closeable.close();
			}
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			int size = src.remaining();
			if (size > 0) {
				openNewConnection(
						new HttpTunnelPayload(this.requestSeq.incrementAndGet(), src));
			}
			return size;
		}

		private void openNewConnection(final HttpTunnelPayload payload) {
			HttpTunnelConnection.this.executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						sendAndReceive(payload);
					}
					catch (IOException ex) {
						if (ex instanceof ConnectException) {
							logger.warn("Failed to connect to remote application at "
									+ HttpTunnelConnection.this.uri);
						}
						else {
							logger.trace("Unexpected connection error", ex);
						}
						closeQuietly();
					}
				}

				private void closeQuietly() {
					try {
						close();
					}
					catch (IOException ex) {
						// Ignore
					}
				}

			});
		}

		private void sendAndReceive(HttpTunnelPayload payload) throws IOException {
			ClientHttpRequest request = createRequest(payload != null);
			if (payload != null) {
				payload.logIncoming();
				payload.assignTo(request);
			}
			handleResponse(request.execute());
		}

		private void handleResponse(ClientHttpResponse response) throws IOException {
			if (response.getStatusCode() == HttpStatus.GONE) {
				close();
				return;
			}
			if (response.getStatusCode() == HttpStatus.OK) {
				HttpTunnelPayload payload = HttpTunnelPayload.get(response);
				if (payload != null) {
					this.forwarder.forward(payload);
				}
			}
			if (response.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
				openNewConnection(null);
			}
		}

	}

	/**
	 * {@link ThreadFactory} used to create the tunnel thread.
	 */
	private static class TunnelThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "HTTP Tunnel Connection");
			thread.setDaemon(true);
			return thread;
		}

	}

}
