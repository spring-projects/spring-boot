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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;
import java.util.Arrays;
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
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
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

	private GracefulShutdown createGracefulShutdown(Server server) {
		StatisticsHandler statisticsHandler = findStatisticsHandler(server);
		if (statisticsHandler == null) {
			return null;
		}
		return new GracefulShutdown(server, statisticsHandler::getRequestsActive);
	}

	private StatisticsHandler findStatisticsHandler(Server server) {
		return findStatisticsHandler(server.getHandler());
	}

	private StatisticsHandler findStatisticsHandler(Handler handler) {
		if (handler instanceof StatisticsHandler) {
			return (StatisticsHandler) handler;
		}
		if (handler instanceof HandlerWrapper) {
			return findStatisticsHandler(((HandlerWrapper) handler).getHandler());
		}
		return null;
	}

	private void initialize() {
		synchronized (this.monitor) {
			try {
				// Cache the connectors and then remove them to prevent requests being
				// handled before the application context is ready.
				this.connectors = this.server.getConnectors();
				this.server.addBean(new AbstractLifeCycle() {

					@Override
					protected void doStart() throws Exception {
						for (Connector connector : JettyWebServer.this.connectors) {
							Assert.state(connector.isStopped(),
									() -> "Connector " + connector + " has been started prematurely");
						}
						JettyWebServer.this.server.setConnectors(null);
					}

				});
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

	private void stopSilently() {
		try {
			this.server.stop();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

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
						if (connector instanceof NetworkConnector) {
							PortInUseException.throwIfPortBindingException(ex,
									() -> ((NetworkConnector) connector).getPort());
						}
						throw ex;
					}
				}
				this.started = true;
				logger.info("Jetty started on port(s) " + getActualPortsDescription() + " with context path '"
						+ getContextPath() + "'");
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

	private String getActualPortsDescription() {
		StringBuilder ports = new StringBuilder();
		for (Connector connector : this.server.getConnectors()) {
			if (ports.length() != 0) {
				ports.append(", ");
			}
			ports.append(getLocalPort(connector)).append(getProtocols(connector));
		}
		return ports.toString();
	}

	private String getProtocols(Connector connector) {
		List<String> protocols = connector.getProtocols();
		return " (" + StringUtils.collectionToDelimitedString(protocols, ", ") + ")";
	}

	private String getContextPath() {
		return Arrays.stream(this.server.getHandlers()).map(this::findContextHandler).filter(Objects::nonNull)
				.map(ContextHandler::getContextPath).collect(Collectors.joining(" "));
	}

	private ContextHandler findContextHandler(Handler handler) {
		while (handler instanceof HandlerWrapper) {
			if (handler instanceof ContextHandler) {
				return (ContextHandler) handler;
			}
			handler = ((HandlerWrapper) handler).getHandler();
		}
		return null;
	}

	private void handleDeferredInitialize(Handler... handlers) throws Exception {
		for (Handler handler : handlers) {
			if (handler instanceof JettyEmbeddedWebAppContext) {
				((JettyEmbeddedWebAppContext) handler).deferredInitialize();
			}
			else if (handler instanceof HandlerWrapper) {
				handleDeferredInitialize(((HandlerWrapper) handler).getHandler());
			}
			else if (handler instanceof HandlerCollection) {
				handleDeferredInitialize(((HandlerCollection) handler).getHandlers());
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.monitor) {
			this.started = false;
			if (this.gracefulShutdown != null) {
				this.gracefulShutdown.abort();
			}
			try {
				this.server.stop();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop embedded Jetty server", ex);
			}
		}
	}

	@Override
	public int getPort() {
		Connector[] connectors = this.server.getConnectors();
		for (Connector connector : connectors) {
			Integer localPort = getLocalPort(connector);
			if (localPort != null && localPort > 0) {
				return localPort;
			}
		}
		return -1;
	}

	private Integer getLocalPort(Connector connector) {
		try {
			if (connector instanceof NetworkConnector) {
				return ((NetworkConnector) connector).getLocalPort();
			}
		}
		catch (Exception ex) {
		}
		try {
			// Jetty 9 internals are different, but the method name is the same
			return (Integer) ReflectionUtils
					.invokeMethod(ReflectionUtils.findMethod(connector.getClass(), "getLocalPort"), connector);
		}
		catch (Exception ex) {
			logger.info("could not determine port (" + ex.getMessage() + ")");
		}
		return 0;
	}

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
