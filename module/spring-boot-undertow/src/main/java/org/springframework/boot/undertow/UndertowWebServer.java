/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.undertow;

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
import org.jspecify.annotations.Nullable;
import org.xnio.channels.BoundChannel;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
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
 * class should be created using a {@link ConfigurableUndertowWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Brian Clozel
 * @since 4.0.0
 */
public class UndertowWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(UndertowWebServer.class);

	private final AtomicReference<@Nullable GracefulShutdownCallback> gracefulShutdownCallback = new AtomicReference<>();

	private final Object monitor = new Object();

	private final Undertow.Builder builder;

	private final Iterable<HttpHandlerFactory> httpHandlerFactories;

	private final boolean autoStart;

	private @Nullable Undertow undertow;

	private volatile boolean started = false;

	private volatile @Nullable GracefulShutdownHandler gracefulShutdown;

	private volatile @Nullable List<Closeable> closeables;

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
	 * @since 4.0.0
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
				String message = getStartedLogMessage();
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
					destroySilently();
				}
			}
		}
	}

	private void destroySilently() {
		try {
			if (this.undertow != null) {
				this.undertow.stop();
				Assert.state(this.closeables != null, "'closeables' must not be null");
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
			// Ignore
		}
	}

	private Undertow createUndertowServer() {
		this.closeables = new ArrayList<>();
		this.gracefulShutdown = null;
		HttpHandler handler = createHttpHandler();
		this.builder.setHandler(handler);
		return this.builder.build();
	}

	protected @Nullable HttpHandler createHttpHandler() {
		HttpHandler handler = null;
		for (HttpHandlerFactory factory : this.httpHandlerFactories) {
			handler = factory.getHandler(handler);
			if (handler instanceof Closeable closeable) {
				Assert.state(this.closeables != null, "'closeables' must not be null");
				this.closeables.add(closeable);
			}
			if (handler instanceof GracefulShutdownHandler shutdownHandler) {
				Assert.state(this.gracefulShutdown == null, "Only a single GracefulShutdownHandler can be defined");
				this.gracefulShutdown = shutdownHandler;
			}
		}
		return handler;
	}

	private String getPortsDescription() {
		StringBuilder description = new StringBuilder();
		List<UndertowWebServer.Port> ports = getActualPorts();
		description.append("port");
		if (ports.size() != 1) {
			description.append("s");
		}
		description.append(" ");
		if (!ports.isEmpty()) {
			description.append(StringUtils.collectionToDelimitedString(ports, ", "));
		}
		else {
			description.append("unknown");
		}
		return description.toString();
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
		Assert.state(channelsField != null, "'channelsField' must not be null");
		ReflectionUtils.makeAccessible(channelsField);
		List<BoundChannel> channels = (List<BoundChannel>) ReflectionUtils.getField(channelsField, this.undertow);
		Assert.state(channels != null, "'channels' must not be null");
		return channels;
	}

	private UndertowWebServer.@Nullable Port getPortFromChannel(BoundChannel channel) {
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
			Field sslField = ReflectionUtils.findField(channel.getClass(), "ssl");
			String protocol = (sslField != null) ? "https" : "http";
			return new UndertowWebServer.Port(inetSocketAddress.getPort(), protocol);
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
		Assert.state(listenersField != null, "'listenersField' must not be null");
		ReflectionUtils.makeAccessible(listenersField);
		List<Object> listeners = (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
		Assert.state(listeners != null, "'listeners' must not be null");
		return listeners;
	}

	private UndertowWebServer.Port getPortFromListener(Object listener) {
		Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
		Assert.state(typeField != null, "'typeField' must not be null");
		String protocol = getProtocol(listener, typeField);
		Field portField = ReflectionUtils.findField(listener.getClass(), "port");
		Assert.state(portField != null, "'portField' must not be null");
		int port = getPort(listener, portField);
		return new UndertowWebServer.Port(port, protocol);
	}

	private static Integer getPort(Object listener, Field portField) {
		ReflectionUtils.makeAccessible(portField);
		Integer value = (Integer) ReflectionUtils.getField(portField, listener);
		Assert.state(value != null, "'value' must not be null");
		return value;
	}

	private String getProtocol(Object listener, Field typeField) {
		ReflectionUtils.makeAccessible(typeField);
		Object value = ReflectionUtils.getField(typeField, listener);
		Assert.state(value != null, "'value' must not be null");
		return value.toString();
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
				if (this.undertow != null) {
					this.undertow.stop();
					Assert.state(this.closeables != null, "'closeables' must not be null");
					for (Closeable closeable : this.closeables) {
						closeable.close();
					}
				}
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop Undertow", ex);
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

	/**
	 * Returns the {@link Undertow Undertow server}. Returns {@code null} until the server
	 * has been started.
	 * @return the Undertow server or {@code null} if the server hasn't been started yet
	 * @since 4.0.0
	 */
	public @Nullable Undertow getUndertow() {
		return this.undertow;
	}

	/**
	 * Initiates a graceful shutdown of the Undertow web server. Handling of new requests
	 * is prevented and the given {@code callback} is invoked at the end of the attempt.
	 * The attempt can be explicitly ended by invoking {@link #stop}.
	 * <p>
	 * Once shutdown has been initiated Undertow will return an {@code HTTP 503} response
	 * for any new or existing connections.
	 */
	@Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		this.gracefulShutdownCallback.set(callback);
		this.gracefulShutdown.shutdown();
		this.gracefulShutdown.addShutdownListener(this::notifyGracefulCallback);
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

	protected String getStartedLogMessage() {
		return "Undertow started on " + getPortsDescription();
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

		private final @Nullable Closeable closeable;

		private CloseableHttpHandlerFactory(@Nullable Closeable closeable) {
			this.closeable = closeable;
		}

		@Override
		public @Nullable HttpHandler getHandler(@Nullable HttpHandler next) {
			Closeable closeable = this.closeable;
			if (closeable == null) {
				return next;
			}
			return new CloseableHttpHandler() {

				@Override
				public void handleRequest(HttpServerExchange exchange) throws Exception {
					if (next != null) {
						next.handleRequest(exchange);
					}
				}

				@Override
				public void close() throws IOException {
					closeable.close();
				}

			};
		}

	}

	/**
	 * {@link Closeable} {@link HttpHandler}.
	 */
	private interface CloseableHttpHandler extends HttpHandler, Closeable {

	}

	/**
	 * {@link RuntimeHintsRegistrar} that allows Undertow's configured and actual ports to
	 * be retrieved at runtime in a native image.
	 */
	static class UndertowWebServerRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerTypeIfPresent(classLoader, "io.undertow.Undertow",
						(hint) -> hint.withField("listeners").withField("channels"));
			hints.reflection()
				.registerTypeIfPresent(classLoader, "io.undertow.Undertow$ListenerConfig",
						(hint) -> hint.withField("type").withField("port"));
			hints.reflection()
				.registerTypeIfPresent(classLoader, "io.undertow.protocols.ssl.UndertowAcceptingSslChannel",
						(hint) -> hint.withField("ssl"));
		}

	}

}
