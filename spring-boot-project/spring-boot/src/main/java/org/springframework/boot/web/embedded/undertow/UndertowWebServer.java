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

	/**
	 * Starts the embedded Undertow web server.
	 * @throws WebServerException if unable to start the server
	 */
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
					destroySilently();
				}
			}
		}
	}

	/**
	 * Stops the Undertow web server and closes any associated resources silently. If the
	 * web server is not running or if there are no associated resources, this method does
	 * nothing. Any exceptions that occur during the stopping or closing process are
	 * ignored.
	 */
	private void destroySilently() {
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

	/**
	 * Closes the given Closeable object silently, without throwing any exceptions.
	 * @param closeable the Closeable object to be closed
	 */
	private void closeSilently(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
	 * Creates and returns an instance of Undertow server.
	 * @return The created Undertow server instance.
	 */
	private Undertow createUndertowServer() {
		this.closeables = new ArrayList<>();
		this.gracefulShutdown = null;
		HttpHandler handler = createHttpHandler();
		this.builder.setHandler(handler);
		return this.builder.build();
	}

	/**
	 * Creates an HTTP handler by iterating through the registered HTTP handler factories.
	 * @return The created HTTP handler.
	 */
	protected HttpHandler createHttpHandler() {
		HttpHandler handler = null;
		for (HttpHandlerFactory factory : this.httpHandlerFactories) {
			handler = factory.getHandler(handler);
			if (handler instanceof Closeable closeable) {
				this.closeables.add(closeable);
			}
			if (handler instanceof GracefulShutdownHandler shutdownHandler) {
				Assert.isNull(this.gracefulShutdown, "Only a single GracefulShutdownHandler can be defined");
				this.gracefulShutdown = shutdownHandler;
			}
		}
		return handler;
	}

	/**
	 * Returns a description of the ports used by the UndertowWebServer.
	 * @return A string representation of the ports used by the UndertowWebServer.
	 */
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

	/**
	 * Retrieves the actual ports being used by the UndertowWebServer.
	 * @return A list of Port objects representing the actual ports being used.
	 */
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

	/**
	 * Extracts the list of bound channels from the Undertow server.
	 * @return The list of bound channels.
	 */
	@SuppressWarnings("unchecked")
	private List<BoundChannel> extractChannels() {
		Field channelsField = ReflectionUtils.findField(Undertow.class, "channels");
		ReflectionUtils.makeAccessible(channelsField);
		return (List<BoundChannel>) ReflectionUtils.getField(channelsField, this.undertow);
	}

	/**
	 * Retrieves the port and protocol from the given BoundChannel.
	 * @param channel the BoundChannel from which to retrieve the port and protocol
	 * @return the UndertowWebServer.Port object containing the port and protocol, or null
	 * if the port cannot be retrieved
	 */
	private UndertowWebServer.Port getPortFromChannel(BoundChannel channel) {
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
			Field sslField = ReflectionUtils.findField(channel.getClass(), "ssl");
			String protocol = (sslField != null) ? "https" : "http";
			return new UndertowWebServer.Port(inetSocketAddress.getPort(), protocol);
		}
		return null;
	}

	/**
	 * Retrieves the list of configured ports for the UndertowWebServer.
	 * @return The list of configured ports.
	 */
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

	/**
	 * Extracts the listeners from the Undertow instance.
	 * @return The list of listeners extracted from the Undertow instance.
	 */
	@SuppressWarnings("unchecked")
	private List<Object> extractListeners() {
		Field listenersField = ReflectionUtils.findField(Undertow.class, "listeners");
		ReflectionUtils.makeAccessible(listenersField);
		return (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
	}

	/**
	 * Retrieves the port and protocol information from the given listener object.
	 * @param listener the listener object from which to retrieve the port and protocol
	 * information
	 * @return the port and protocol information encapsulated in an UndertowWebServer.Port
	 * object
	 */
	private UndertowWebServer.Port getPortFromListener(Object listener) {
		Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
		ReflectionUtils.makeAccessible(typeField);
		String protocol = ReflectionUtils.getField(typeField, listener).toString();
		Field portField = ReflectionUtils.findField(listener.getClass(), "port");
		ReflectionUtils.makeAccessible(portField);
		int port = (Integer) ReflectionUtils.getField(portField, listener);
		return new UndertowWebServer.Port(port, protocol);
	}

	/**
	 * Stops the Undertow web server.
	 * @throws WebServerException if unable to stop the web server
	 */
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
				throw new WebServerException("Unable to stop Undertow", ex);
			}
		}
	}

	/**
	 * Returns the port number of the Undertow web server.
	 * @return the port number, or -1 if no ports are available
	 */
	@Override
	public int getPort() {
		List<Port> ports = getActualPorts();
		if (ports.isEmpty()) {
			return -1;
		}
		return ports.get(0).getNumber();
	}

	/**
	 * Shuts down the server gracefully, allowing active requests to complete before
	 * shutting down completely.
	 * @param callback the callback to be invoked when the shutdown is complete
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

	/**
	 * Notifies the graceful shutdown callback about the completion or abortion of the
	 * graceful shutdown process.
	 * @param success true if the graceful shutdown process completed successfully, false
	 * otherwise
	 */
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

	/**
	 * Returns the log message indicating the start of the Undertow web server.
	 * @return the log message indicating the start of the Undertow web server
	 */
	protected String getStartLogMessage() {
		return "Undertow started on " + getPortsDescription();
	}

	/**
	 * An active Undertow port.
	 */
	private static final class Port {

		private final int number;

		private final String protocol;

		/**
		 * Constructs a new Port object with the specified number and protocol.
		 * @param number the port number
		 * @param protocol the protocol associated with the port
		 */
		private Port(int number, String protocol) {
			this.number = number;
			this.protocol = protocol;
		}

		/**
		 * Returns the number associated with this Port.
		 * @return the number associated with this Port
		 */
		int getNumber() {
			return this.number;
		}

		/**
		 * Compares this Port object to the specified object for equality.
		 * @param obj the object to compare to
		 * @return true if the specified object is equal to this Port object, false
		 * otherwise
		 */
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

		/**
		 * Returns the hash code value for this Port object.
		 * @return the hash code value for this Port object
		 */
		@Override
		public int hashCode() {
			return this.number;
		}

		/**
		 * Returns a string representation of the Port object. The string representation
		 * includes the port number and protocol in parentheses.
		 * @return a string representation of the Port object
		 */
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

		/**
		 * Constructs a new CloseableHttpHandlerFactory with the specified Closeable
		 * object.
		 * @param closeable the Closeable object to be associated with the
		 * CloseableHttpHandlerFactory
		 */
		private CloseableHttpHandlerFactory(Closeable closeable) {
			this.closeable = closeable;
		}

		/**
		 * Returns the HttpHandler with the provided next HttpHandler. If the closeable is
		 * null, returns the next HttpHandler as is. Otherwise, returns a new
		 * CloseableHttpHandler that delegates the handling to the next HttpHandler and
		 * closes the closeable when closed.
		 * @param next the next HttpHandler to be used if the closeable is null
		 * @return the HttpHandler with the provided next HttpHandler
		 */
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

	/**
	 * {@link RuntimeHintsRegistrar} that allows Undertow's configured and actual ports to
	 * be retrieved at runtime in a native image.
	 */
	static class UndertowWebServerRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers hints for the UndertowWebServerRuntimeHints class.
		 * @param hints The RuntimeHints object to register the hints with.
		 * @param classLoader The ClassLoader to use for reflection.
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
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
