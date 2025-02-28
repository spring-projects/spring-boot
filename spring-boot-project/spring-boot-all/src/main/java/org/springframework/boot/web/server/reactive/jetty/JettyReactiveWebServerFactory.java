/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.server.reactive.jetty;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;

import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.jetty.ForwardHeadersCustomizer;
import org.springframework.boot.web.server.jetty.JettyServerCustomizer;
import org.springframework.boot.web.server.jetty.JettyWebServer;
import org.springframework.boot.web.server.jetty.JettyWebServerFactory;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.boot.web.server.servlet.jetty.JettyServletWebServerFactory;
import org.springframework.http.client.reactive.JettyResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.JettyHttpHandlerAdapter;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link JettyWebServer}s.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class JettyReactiveWebServerFactory extends JettyWebServerFactory
		implements ConfigurableJettyWebServerFactory, ConfigurableReactiveWebServerFactory {

	private static final Log logger = LogFactory.getLog(JettyReactiveWebServerFactory.class);

	private JettyResourceFactory resourceFactory;

	/**
	 * Create a new {@link JettyServletWebServerFactory} instance.
	 */
	public JettyReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link JettyServletWebServerFactory} that listens for requests using
	 * the specified port.
	 * @param port the port to listen on
	 */
	public JettyReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		JettyHttpHandlerAdapter servlet = new JettyHttpHandlerAdapter(httpHandler);
		Server server = createJettyServer(servlet);
		return new JettyWebServer(server, getPort() >= 0);
	}

	/**
	 * Set the {@link JettyResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 */
	public void setResourceFactory(JettyResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	protected JettyResourceFactory getResourceFactory() {
		return this.resourceFactory;
	}

	protected Server createJettyServer(JettyHttpHandlerAdapter servlet) {
		int port = Math.max(getPort(), 0);
		InetSocketAddress address = new InetSocketAddress(getAddress(), port);
		Server server = new Server(getThreadPool());
		if (this.resourceFactory == null) {
			server.addConnector(createConnector(address, server));
		}
		else {
			server.addConnector(createConnector(address, server, this.resourceFactory.getExecutor(),
					this.resourceFactory.getScheduler(), this.resourceFactory.getByteBufferPool()));
		}
		server.setStopTimeout(0);
		ServletHolder servletHolder = new ServletHolder(servlet);
		servletHolder.setAsyncSupported(true);
		ServletContextHandler contextHandler = new ServletContextHandler("/", false, false);
		contextHandler.addServlet(servletHolder, "/");
		server.setHandler(addHandlerWrappers(contextHandler));
		logger.info("Server initialized with port: " + port);
		if (this.getMaxConnections() > -1) {
			server.addBean(new ConnectionLimit(this.getMaxConnections(), server));
		}
		if (Ssl.isEnabled(getSsl())) {
			customizeSsl(server, address);
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.isUseForwardHeaders()) {
			new ForwardHeadersCustomizer().customize(server);
		}
		if (getShutdown() == Shutdown.GRACEFUL) {
			StatisticsHandler statisticsHandler = new StatisticsHandler();
			statisticsHandler.setHandler(server.getHandler());
			server.setHandler(statisticsHandler);
		}
		return server;
	}

}
