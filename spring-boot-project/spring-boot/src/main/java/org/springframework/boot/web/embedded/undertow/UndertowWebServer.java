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

package org.springframework.boot.web.embedded.undertow;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.GracefulShutdownHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.channels.BoundChannel;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control an Undertow web server. Usually this
 * class should be created using the {@link UndertowReactiveWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Brian Clozel
 * @since 2.0.0
 */
public class UndertowWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(UndertowWebServer.class);

	private final AtomicReference<GracefulShutdownCallback> gracefulShutdownCallback = new AtomicReference<>();

	private final Object monitor = new Object();

	private final Undertow.Builder builder;

	private final Iterable<HttpHandlerFactory> httpHandlerFactories;

	private final boolean autoStart;

	private Undertow undertow;

	private volatile boolean started = false;

	private volatile GracefulShutdownHandler gracefulShutdown;

	private volatile List<Closeable> closeables;

	/**
	 * Create a new {@link UndertowWebServer} instance.
	 * @param builder the builder
	 * @param autoStart if the server should be started
	 */
	public UndertowWebServer(Undertow.Builder builder, boolean autoStart) {
		this(builder, Collections.singleton(new CloseableHttpHandlerFactory(null)), autoStart);
	}

	/**
	 * Create a new {@link UndertowWebServer} instance.
	 * @param builder the builder
	 * @param httpHandlerFactories the handler factories
	 * @param autoStart if the server should be started
	 * @since 2.3.0
	 */
	public UndertowWebServer(Undertow.Builder builder, Iterable<HttpHandlerFactory> httpHandlerFactories,
			boolean autoStart) {
		this.builder = builder;
		this.httpHandlerFactories = httpHandlerFactories;
		this.autoStart = autoStart;
	}

	@Override
	public void start() throws WebServerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			try {
				if (!this.autoStart) {
					return;
				}
				if (this.undertow == null) {
					this.undertow = createUndertowServer();
				}
				this.undertow.start();
				this.started = true;
				String message = getStartLogMessage();
				logger.info(message);
			}
			catch (Exception ex) {
				try {
					PortInUseException.ifPortBindingException(ex, (bindException) -> {
						List<Port> failedPorts = getConfiguredPorts();
						failedPorts.removeAll(getActualPorts());
						if (failedPorts.size() == 1) {
							throw new PortInUseException(failedPorts.get(0).getNumber());
						}
					});
					throw new WebServerException("Unable to start embedded Undertow", ex);
				}
				finally {
					stopSilently();
				}
			}
		}
	}

	private void stopSilently() {
		try {
			if (this.undertow != null) {
				this.undertow.stop();
				this.closeables.forEach(this::closeSilently);
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	private void closeSilently(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (Exception ex) {
		}
	}

	private Undertow createUndertowServer() {
		this.closeables = new ArrayList<>();
		this.gracefulShutdown = null;
		HttpHandler handler = createHttpHandler();
		this.builder.setHandler(handler);
		return this.builder.build();
	}

	protected HttpHandler createHttpHandler() {
		HttpHandler handler = null;
		for (HttpHandlerFactory factory : this.httpHandlerFactories) {
			handler = factory.getHandler(handler);
			if (handler instanceof Closeable) {
				this.closeables.add((Closeable) handler);
			}
			if (handler instanceof GracefulShutdownHandler) {
				Assert.isNull(this.gracefulShutdown, "Only a single GracefulShutdownHandler can be defined");
				this.gracefulShutdown = (GracefulShutdownHandler) handler;
			}
		}
		return handler;
	}

	private String getPortsDescription() {
		List<UndertowWebServer.Port> ports = getActualPorts();
		if (!ports.isEmpty()) {
			return StringUtils.collectionToDelimitedString(ports, " ");
		}
		return "unknown";
	}

	private List<Port> getActualPorts() {
		List<Port> ports = new ArrayList<>();
		try {
			if (!this.autoStart) {
				ports.add(new Port(-1, "unknown"));
			}
			else {
				for (BoundChannel channel : extractChannels()) {
					ports.add(getPortFromChannel(channel));
				}
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<BoundChannel> extractChannels() {
		Field channelsField = ReflectionUtils.findField(Undertow.class, "channels");
		ReflectionUtils.makeAccessible(channelsField);
		return (List<BoundChannel>) ReflectionUtils.getField(channelsField, this.undertow);
	}

	private UndertowWebServer.Port getPortFromChannel(BoundChannel channel) {
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress) {
			Field sslField = ReflectionUtils.findField(channel.getClass(), "ssl");
			String protocol = (sslField != null) ? "https" : "http";
			return new UndertowWebServer.Port(((InetSocketAddress) socketAddress).getPort(), protocol);
		}
		return null;
	}

	private List<UndertowWebServer.Port> getConfiguredPorts() {
		List<Port> ports = new ArrayList<>();
		for (Object listener : extractListeners()) {
			try {
				Port port = getPortFromListener(listener);
				if (port.getNumber() != 0) {
					ports.add(port);
				}
			}
			catch (Exception ex) {
				// Continue
			}
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<Object> extractListeners() {
		Field listenersField = ReflectionUtils.findField(Undertow.class, "listeners");
		ReflectionUtils.makeAccessible(listenersField);
		return (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
	}

	private UndertowWebServer.Port getPortFromListener(Object listener) {
		Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
		ReflectionUtils.makeAccessible(typeField);
		String protocol = ReflectionUtils.getField(typeField, listener).toString();
		Field portField = ReflectionUtils.findField(listener.getClass(), "port");
		ReflectionUtils.makeAccessible(portField);
		int port = (Integer) ReflectionUtils.getField(portField, listener);
		return new UndertowWebServer.Port(port, protocol);
	}

	@Override
	public void stop() throws WebServerException {
		synchronized (this.monitor) {
			if (!this.started) {
				return;
			}
			this.started = false;
			if (this.gracefulShutdown != null) {
				notifyGracefulCallback(false);
			}
			try {
				this.undertow.stop();
				for (Closeable closeable : this.closeables) {
					closeable.close();
				}
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop undertow", ex);
			}
		}
	}

	@Override
	public int getPort() {
		List<Port> ports = getActualPorts();
		if (ports.isEmpty()) {
			return -1;
		}
		return ports.get(0).getNumber();
	}

	@Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		this.gracefulShutdownCallback.set(callback);
		this.gracefulShutdown.shutdown();
		this.gracefulShutdown.addShutdownListener((success) -> notifyGracefulCallback(success));
	}

	private void notifyGracefulCallback(boolean success) {
		GracefulShutdownCallback callback = this.gracefulShutdownCallback.getAndSet(null);
		if (callback != null) {
			if (success) {
				logger.info("Graceful shutdown complete");
				callback.shutdownComplete(GracefulShutdownResult.IDLE);
			}
			else {
				logger.info("Graceful shutdown aborted with one or more requests still active");
				callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
			}
		}
	}

	protected String getStartLogMessage() {
		return "Undertow started on port(s) " + getPortsDescription();
	}

	/**
	 * An active Undertow port.
	 */
	private static final class Port {

		private final int number;

		private final String protocol;

		private Port(int number, String protocol) {
			this.number = number;
			this.protocol = protocol;
		}

		int getNumber() {
			return this.number;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			UndertowWebServer.Port other = (UndertowWebServer.Port) obj;
			return this.number == other.number;
		}

		@Override
		public int hashCode() {
			return this.number;
		}

		@Override
		public String toString() {
			return this.number + " (" + this.protocol + ")";
		}

	}

	/**
	 * {@link HttpHandlerFactory} to wrap a closable.
	 */
	private static final class CloseableHttpHandlerFactory implements HttpHandlerFactory {

		private final Closeable closeable;

		private CloseableHttpHandlerFactory(Closeable closeable) {
			this.closeable = closeable;
		}

		@Override
		public HttpHandler getHandler(HttpHandler next) {
			if (this.closeable == null) {
				return next;
			}
			return new CloseableHttpHandler() {

				@Override
				public void handleRequest(HttpServerExchange exchange) throws Exception {
					next.handleRequest(exchange);
				}

				@Override
				public void close() throws IOException {
					CloseableHttpHandlerFactory.this.closeable.close();
				}

			};
		}

	}

	/**
	 * {@link Closeable} {@link HttpHandler}.
	 */
	private interface CloseableHttpHandler extends HttpHandler, Closeable {

	}

}
