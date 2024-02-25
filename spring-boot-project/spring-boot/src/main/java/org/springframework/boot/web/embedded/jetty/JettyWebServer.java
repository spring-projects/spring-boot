/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control a Jetty web server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author David Liu
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Kristine Jetzke
 * @since 2.0.0
 * @see JettyReactiveWebServerFactory
 */
public class JettyWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(JettyWebServer.class);

	private final Object monitor = new Object();

	private final Server server;

	private final boolean autoStart;

	private final GracefulShutdown gracefulShutdown;

	private Connector[] connectors;

	private volatile boolean started;

	/**
	 * Create a new {@link JettyWebServer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyWebServer(Server server) {
		this(server, true);
	}

	/**
	 * Create a new {@link JettyWebServer} instance.
	 * @param server the underlying Jetty server
	 * @param autoStart if auto-starting the server
	 */
	public JettyWebServer(Server server, boolean autoStart) {
		this.autoStart = autoStart;
		Assert.notNull(server, "Jetty Server must not be null");
		this.server = server;
		this.gracefulShutdown = createGracefulShutdown(server);
		initialize();
	}

	/**
	 * Creates a GracefulShutdown object for the given server.
	 * @param server the server for which the GracefulShutdown object is created
	 * @return the created GracefulShutdown object, or null if no StatisticsHandler is
	 * found
	 */
	private GracefulShutdown createGracefulShutdown(Server server) {
		StatisticsHandler statisticsHandler = findStatisticsHandler(server);
		if (statisticsHandler == null) {
			return null;
		}
		return new GracefulShutdown(server, statisticsHandler::getRequestsActive);
	}

	/**
	 * Finds the StatisticsHandler for the given Server.
	 * @param server the Server object for which to find the StatisticsHandler
	 * @return the StatisticsHandler found for the given Server
	 */
	private StatisticsHandler findStatisticsHandler(Server server) {
		return findStatisticsHandler(server.getHandler());
	}

	/**
	 * Finds the StatisticsHandler associated with the given Handler.
	 * @param handler the Handler to search for a StatisticsHandler
	 * @return the StatisticsHandler if found, null otherwise
	 */
	private StatisticsHandler findStatisticsHandler(Handler handler) {
		if (handler instanceof StatisticsHandler statisticsHandler) {
			return statisticsHandler;
		}
		if (handler instanceof Handler.Wrapper handlerWrapper) {
			return findStatisticsHandler(handlerWrapper.getHandler());
		}
		return null;
	}

	/**
	 * Initializes the embedded Jetty web server.
	 *
	 * This method starts the server and sets the connectors to null to prevent requests
	 * from being handled before the application context is ready. The server is started
	 * so that the ServletContext is available.
	 * @throws WebServerException if unable to start the embedded Jetty web server
	 */
	private void initialize() {
		synchronized (this.monitor) {
			try {
				// Cache the connectors and then remove them to prevent requests being
				// handled before the application context is ready.
				this.connectors = this.server.getConnectors();
				JettyWebServer.this.server.setConnectors(null);
				// Start the server so that the ServletContext is available
				this.server.start();
				this.server.setStopAtShutdown(false);
			}
			catch (Throwable ex) {
				// Ensure process isn't left running
				stopSilently();
				throw new WebServerException("Unable to start embedded Jetty web server", ex);
			}
		}
	}

	/**
	 * Stops the server silently without throwing any exceptions. If an exception occurs
	 * during the server stop process, it will be ignored.
	 */
	private void stopSilently() {
		try {
			this.server.stop();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
	 * Starts the embedded Jetty server.
	 * @throws WebServerException if an error occurs while starting the server
	 */
	@Override
	public void start() throws WebServerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			this.server.setConnectors(this.connectors);
			if (!this.autoStart) {
				return;
			}
			try {
				this.server.start();
				for (Handler handler : this.server.getHandlers()) {
					handleDeferredInitialize(handler);
				}
				Connector[] connectors = this.server.getConnectors();
				for (Connector connector : connectors) {
					try {
						connector.start();
					}
					catch (IOException ex) {
						if (connector instanceof NetworkConnector networkConnector) {
							PortInUseException.throwIfPortBindingException(ex, networkConnector::getPort);
						}
						throw ex;
					}
				}
				this.started = true;
				logger.info(getStartedLogMessage());
			}
			catch (WebServerException ex) {
				stopSilently();
				throw ex;
			}
			catch (Exception ex) {
				stopSilently();
				throw new WebServerException("Unable to start embedded Jetty server", ex);
			}
		}
	}

	/**
	 * Returns a log message indicating that Jetty has started.
	 * @return the log message indicating Jetty has started
	 */
	String getStartedLogMessage() {
		String contextPath = getContextPath();
		return "Jetty started on " + getActualPortsDescription()
				+ ((contextPath != null) ? " with context path '" + contextPath + "'" : "");
	}

	/**
	 * Returns the description of the actual ports used by the Jetty web server.
	 * @return the description of the actual ports used by the Jetty web server
	 */
	private String getActualPortsDescription() {
		StringBuilder description = new StringBuilder("port");
		Connector[] connectors = this.server.getConnectors();
		if (connectors.length != 1) {
			description.append("s");
		}
		description.append(" ");
		for (int i = 0; i < connectors.length; i++) {
			if (i != 0) {
				description.append(", ");
			}
			Connector connector = connectors[i];
			description.append(getLocalPort(connector)).append(getProtocols(connector));
		}
		return description.toString();
	}

	/**
	 * Returns the protocols supported by the given connector.
	 * @param connector the connector to retrieve the protocols from
	 * @return a string representation of the supported protocols, delimited by commas
	 */
	private String getProtocols(Connector connector) {
		List<String> protocols = connector.getProtocols();
		return " (" + StringUtils.collectionToDelimitedString(protocols, ", ") + ")";
	}

	/**
	 * Returns the context path of the server. If the server is an instance of
	 * JettyReactiveWebServerFactory, returns null. Otherwise, iterates through the
	 * server's handlers, finds the context handler for each handler, and collects the
	 * context paths into a single string, separated by spaces.
	 * @return the context path of the server, or null if the server is an instance of
	 * JettyReactiveWebServerFactory
	 */
	private String getContextPath() {
		if (JettyReactiveWebServerFactory.class.equals(this.server.getAttribute(WebServerFactory.class.getName()))) {
			return null;
		}
		return this.server.getHandlers()
			.stream()
			.map(this::findContextHandler)
			.filter(Objects::nonNull)
			.map(ContextHandler::getContextPath)
			.collect(Collectors.joining(" "));
	}

	/**
	 * Finds the ContextHandler associated with the given Handler.
	 *
	 * This method iterates through the chain of Handler.Wrapper instances until it finds
	 * a ContextHandler. It returns the first ContextHandler found or null if no
	 * ContextHandler is found.
	 * @param handler The Handler for which to find the associated ContextHandler.
	 * @return The ContextHandler associated with the given Handler, or null if not found.
	 */
	private ContextHandler findContextHandler(Handler handler) {
		while (handler instanceof Handler.Wrapper handlerWrapper) {
			if (handler instanceof ContextHandler contextHandler) {
				return contextHandler;
			}
			handler = handlerWrapper.getHandler();
		}
		return null;
	}

	/**
	 * Handles deferred initialization for a list of handlers.
	 * @param handlers the list of handlers to handle deferred initialization for
	 * @throws Exception if an error occurs during deferred initialization
	 */
	private void handleDeferredInitialize(List<Handler> handlers) throws Exception {
		for (Handler handler : handlers) {
			handleDeferredInitialize(handler);
		}
	}

	/**
	 * Handles deferred initialization for the given handler.
	 * @param handler the handler to handle deferred initialization for
	 * @throws Exception if an error occurs during deferred initialization
	 */
	private void handleDeferredInitialize(Handler handler) throws Exception {
		if (handler instanceof JettyEmbeddedWebAppContext jettyEmbeddedWebAppContext) {
			jettyEmbeddedWebAppContext.deferredInitialize();
		}
		else if (handler instanceof Handler.Wrapper handlerWrapper) {
			handleDeferredInitialize(handlerWrapper.getHandler());
		}
		else if (handler instanceof Handler.Collection handlerCollection) {
			handleDeferredInitialize(handlerCollection.getHandlers());
		}
	}

	/**
	 * Stops the embedded Jetty server.
	 *
	 * This method stops the server by setting the 'started' flag to false and aborting
	 * any ongoing graceful shutdown process. It then stops all the connectors associated
	 * with the server.
	 * @throws WebServerException if unable to stop the server
	 */
	@Override
	public void stop() {
		synchronized (this.monitor) {
			this.started = false;
			if (this.gracefulShutdown != null) {
				this.gracefulShutdown.abort();
			}
			try {
				for (Connector connector : this.server.getConnectors()) {
					connector.stop();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop embedded Jetty server", ex);
			}
		}
	}

	/**
	 * Stops and destroys the embedded Jetty server.
	 * @throws WebServerException if unable to destroy the server
	 */
	@Override
	public void destroy() {
		synchronized (this.monitor) {
			try {
				this.server.stop();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to destroy embedded Jetty server", ex);
			}
		}
	}

	/**
	 * Returns the port number on which the Jetty web server is listening.
	 * @return the port number if the server is listening on a valid port, -1 otherwise
	 */
	@Override
	public int getPort() {
		Connector[] connectors = this.server.getConnectors();
		for (Connector connector : connectors) {
			int localPort = getLocalPort(connector);
			if (localPort > 0) {
				return localPort;
			}
		}
		return -1;
	}

	/**
	 * Returns the local port of the given Connector.
	 * @param connector the Connector to get the local port from
	 * @return the local port of the Connector, or 0 if the Connector is not a
	 * NetworkConnector
	 */
	private int getLocalPort(Connector connector) {
		if (connector instanceof NetworkConnector networkConnector) {
			return networkConnector.getLocalPort();
		}
		return 0;
	}

	/**
	 * Shuts down the web server gracefully.
	 * @param callback the callback to be invoked when the shutdown is complete
	 */
	@Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		this.gracefulShutdown.shutDownGracefully(callback);
	}

	/**
	 * Returns access to the underlying Jetty Server.
	 * @return the Jetty server
	 */
	public Server getServer() {
		return this.server;
	}

}
