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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.JettyHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link JettyWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class JettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory
		implements ConfigurableJettyWebServerFactory {

	private static final Log logger = LogFactory
			.getLog(JettyReactiveWebServerFactory.class);

	/**
	 * The number of acceptor threads to use.
	 */
	private int acceptors = -1;

	/**
	 * The number of selector threads to use.
	 */
	private int selectors = -1;

	private boolean useForwardHeaders;

	private List<JettyServerCustomizer> jettyServerCustomizers = new ArrayList<>();

	private ThreadPool threadPool;

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
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	@Override
	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		JettyHttpHandlerAdapter servlet = new JettyHttpHandlerAdapter(httpHandler);
		Server server = createJettyServer(servlet);
		return new JettyWebServer(server, getPort() >= 0);
	}

	@Override
	public void addServerCustomizers(JettyServerCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.jettyServerCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
	 * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
	 * before it is started. Calling this method will replace any existing customizers.
	 * @param customizers the Jetty customizers to apply
	 */
	public void setServerCustomizers(
			Collection<? extends JettyServerCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.jettyServerCustomizers = new ArrayList<>(customizers);
	}

	/**
	 * Returns a mutable collection of Jetty {@link JettyServerCustomizer}s that will be
	 * applied to the {@link Server} before it is created.
	 * @return the Jetty customizers
	 */
	public Collection<JettyServerCustomizer> getServerCustomizers() {
		return this.jettyServerCustomizers;
	}

	@Override
	public void setSelectors(int selectors) {
		this.selectors = selectors;
	}

	protected Server createJettyServer(JettyHttpHandlerAdapter servlet) {
		int port = (getPort() >= 0 ? getPort() : 0);
		InetSocketAddress address = new InetSocketAddress(getAddress(), port);
		Server server = new Server(getThreadPool());
		server.addConnector(createConnector(address, server));
		ServletHolder servletHolder = new ServletHolder(servlet);
		servletHolder.setAsyncSupported(true);
		ServletContextHandler contextHandler = new ServletContextHandler(server, "",
				false, false);
		contextHandler.addServlet(servletHolder, "/");
		server.setHandler(addHandlerWrappers(contextHandler));
		JettyReactiveWebServerFactory.logger
				.info("Server initialized with port: " + port);
		if (getSsl() != null && getSsl().isEnabled()) {
			customizeSsl(server, address);
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.useForwardHeaders) {
			new ForwardHeadersCustomizer().customize(server);
		}
		return server;
	}

	private AbstractConnector createConnector(InetSocketAddress address, Server server) {
		ServerConnector connector = new ServerConnector(server, this.acceptors,
				this.selectors);
		connector.setHost(address.getHostString());
		connector.setPort(address.getPort());
		for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
			if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
				((HttpConfiguration.ConnectionFactory) connectionFactory)
						.getHttpConfiguration().setSendServerVersion(false);
			}
		}
		return connector;
	}

	private Handler addHandlerWrappers(Handler handler) {
		if (getCompression() != null && getCompression().getEnabled()) {
			handler = applyWrapper(handler,
					JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
		}
		if (StringUtils.hasText(getServerHeader())) {
			handler = applyWrapper(handler, JettyHandlerWrappers
					.createServerHeaderHandlerWrapper(getServerHeader()));
		}
		return handler;
	}

	private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

	private void customizeSsl(Server server, InetSocketAddress address) {
		new SslServerCustomizer(address, getSsl(), getSslStoreProvider(), getHttp2())
				.customize(server);
	}

	/**
	 * Returns a Jetty {@link ThreadPool} that should be used by the {@link Server}.
	 * @return a Jetty {@link ThreadPool} or {@code null}
	 */
	public ThreadPool getThreadPool() {
		return this.threadPool;
	}

	/**
	 * Set a Jetty {@link ThreadPool} that should be used by the {@link Server}. If set to
	 * {@code null} (default), the {@link Server} creates a {@link ThreadPool} implicitly.
	 * @param threadPool a Jetty ThreadPool to be used
	 */
	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

}
