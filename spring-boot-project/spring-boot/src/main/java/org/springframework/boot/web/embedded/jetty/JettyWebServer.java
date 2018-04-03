/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import java.net.BindException;
import java.util.Arrays;
import java.util.List;
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
import org.eclipse.jetty.util.component.AbstractLifeCycle;

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
		initialize();
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
							Assert.state(connector.isStopped(), () -> "Connector "
									+ connector + " has been started prematurely");
						}
						JettyWebServer.this.server.setConnectors(null);
					}

				});
				// Start the server so that the ServletContext is available
				this.server.start();
				this.server.setStopAtShutdown(false);
			}
			catch (Exception ex) {
				// Ensure process isn't left running
				stopSilently();
				throw new WebServerException("Unable to start embedded Jetty web server",
						ex);
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
					catch (BindException ex) {
						if (connector instanceof NetworkConnector) {
							throw new PortInUseException(
									((NetworkConnector) connector).getPort());
						}
						throw ex;
					}
				}
				this.started = true;
				JettyWebServer.logger
						.info("Jetty started on port(s) " + getActualPortsDescription()
								+ " with context path '" + getContextPath() + "'");
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
			ports.append(ports.length() == 0 ? "" : ", ");
			ports.append(getLocalPort(connector) + getProtocols(connector));
		}
		return ports.toString();
	}

	private Integer getLocalPort(Connector connector) {
		try {
			// Jetty 9 internals are different, but the method name is the same
			return (Integer) ReflectionUtils.invokeMethod(
					ReflectionUtils.findMethod(connector.getClass(), "getLocalPort"),
					connector);
		}
		catch (Exception ex) {
			JettyWebServer.logger
					.info("could not determine port ( " + ex.getMessage() + ")");
			return 0;
		}
	}

	private String getProtocols(Connector connector) {
		List<String> protocols = connector.getProtocols();
		return " (" + StringUtils.collectionToDelimitedString(protocols, ", ") + ")";
	}

	private String getContextPath() {
		return Arrays.stream(this.server.getHandlers())
				.filter(ContextHandler.class::isInstance).map(ContextHandler.class::cast)
				.map(ContextHandler::getContextPath).collect(Collectors.joining(" "));
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
			// Probably only one...
			return getLocalPort(connector);
		}
		return 0;
	}

	/**
	 * Returns access to the underlying Jetty Server.
	 * @return the Jetty server
	 */
	public Server getServer() {
		return this.server;
	}

}
