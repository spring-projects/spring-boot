/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Jetty server.
 * Usually this class should be created using the
 * {@link JettyEmbeddedServletContainerFactory} and not directly.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @see JettyEmbeddedServletContainerFactory
 */
public class JettyEmbeddedServletContainer implements EmbeddedServletContainer {

	private final Log logger = LogFactory.getLog(JettyEmbeddedServletContainer.class);

	private final Server server;

	private final boolean autoStart;

	private Connector[] connectors;

	/**
	 * Create a new {@link JettyEmbeddedServletContainer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyEmbeddedServletContainer(Server server) {
		this(server, true);
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyEmbeddedServletContainer(Server server, boolean autoStart) {
		this.autoStart = autoStart;
		Assert.notNull(server, "Jetty Server must not be null");
		this.server = server;
		initialize();
	}

	private synchronized void initialize() {
		try {
			// Cache and clear the connectors to prevent requests being handled before
			// the application context is ready
			this.connectors = this.server.getConnectors();
			this.server.setConnectors(null);

			// Start the server so that the ServletContext is available
			this.server.start();
		}
		catch (Exception ex) {
			try {
				// Ensure process isn't left running
				this.server.stop();
			}
			catch (Exception e) {
			}
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Jetty servlet container", ex);
		}
	}

	@Override
	public void start() throws EmbeddedServletContainerException {
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
				connector.start();
				this.logger.info("Jetty started on port: " + getLocalPort(connector));
			}
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Jetty servlet container", ex);
		}
	}

	private void handleDeferredInitialize(Handler handler) throws Exception {
		if (handler instanceof JettyEmbeddedWebAppContext) {
			((JettyEmbeddedWebAppContext) handler).deferredInitialize();
		}
		else if (handler instanceof HandlerWrapper) {
			handleDeferredInitialize(((HandlerWrapper) handler).getHandler());
		}
	}

	private Integer getLocalPort(Connector connector) {
		try {
			// Jetty 9 internals are different, but the method name is the same
			return (Integer) ReflectionUtils.invokeMethod(
					ReflectionUtils.findMethod(connector.getClass(), "getLocalPort"),
					connector);
		}
		catch (Exception ex) {
			this.logger.info("could not determine port ( " + ex.getMessage() + ")");
			return 0;
		}
	}

	@Override
	public synchronized void stop() {
		try {
			this.server.stop();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to stop embedded Jetty servlet container", ex);
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
	 */
	public Server getServer() {
		return this.server;
	}

}
