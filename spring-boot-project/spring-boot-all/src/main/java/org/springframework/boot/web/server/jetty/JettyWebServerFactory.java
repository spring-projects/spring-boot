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

package org.springframework.boot.web.server.jetty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for factories that produce a {@link JettyWebServer}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class JettyWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableJettyWebServerFactory {

	private int acceptors = -1;

	private ThreadPool threadPool;

	private int selectors = -1;

	private List<Configuration> configurations = new ArrayList<>();

	private boolean useForwardHeaders;

	private Set<JettyServerCustomizer> jettyServerCustomizers = new LinkedHashSet<>();

	private int maxConnections = -1;

	public JettyWebServerFactory() {

	}

	public JettyWebServerFactory(int port) {
		super(port);
	}

	public int getAcceptors() {
		return this.acceptors;
	}

	@Override
	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	public int getSelectors() {
		return this.selectors;
	}

	@Override
	public void setSelectors(int selectors) {
		this.selectors = selectors;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	@Override
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * Returns a mutable collection of Jetty {@link JettyServerCustomizer}s that will be
	 * applied to the {@link Server} before it is created.
	 * @return the {@link JettyServerCustomizer}s
	 */
	public Collection<JettyServerCustomizer> getServerCustomizers() {
		return this.jettyServerCustomizers;
	}

	/**
	 * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
	 * before it is started. Calling this method will replace any existing customizers.
	 * @param customizers the Jetty customizers to apply
	 */
	public void setServerCustomizers(Collection<? extends JettyServerCustomizer> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		this.jettyServerCustomizers = new LinkedHashSet<>(customizers);
	}

	@Override
	public void addServerCustomizers(JettyServerCustomizer... customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		this.jettyServerCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
	 * Returns a mutable collection of Jetty {@link Configuration}s that will be applied
	 * to the {@link WebAppContext} before the server is created.
	 * @return the Jetty {@link Configuration}s
	 */
	public Collection<Configuration> getConfigurations() {
		return this.configurations;
	}

	/**
	 * Sets Jetty {@link Configuration}s that will be applied to the {@link WebAppContext}
	 * before the server is created. Calling this method will replace any existing
	 * configurations.
	 * @param configurations the Jetty configurations to apply
	 */
	public void setConfigurations(Collection<? extends Configuration> configurations) {
		Assert.notNull(configurations, "'configurations' must not be null");
		this.configurations = new ArrayList<>(configurations);
	}

	/**
	 * Add {@link Configuration}s that will be applied to the {@link WebAppContext} before
	 * the server is started.
	 * @param configurations the configurations to add
	 */
	public void addConfigurations(Configuration... configurations) {
		Assert.notNull(configurations, "'configurations' must not be null");
		this.configurations.addAll(Arrays.asList(configurations));
	}

	/**
	 * Returns a Jetty {@link ThreadPool} that should be used by the {@link Server}.
	 * @return a Jetty {@link ThreadPool} or {@code null}
	 */
	public ThreadPool getThreadPool() {
		return this.threadPool;
	}

	@Override
	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	public boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	protected AbstractConnector createConnector(InetSocketAddress address, Server server) {
		return this.createConnector(address, server, null, null, null);
	}

	protected AbstractConnector createConnector(InetSocketAddress address, Server server, Executor executor,
			Scheduler scheduler, ByteBufferPool pool) {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		List<ConnectionFactory> connectionFactories = new ArrayList<>();
		connectionFactories.add(new HttpConnectionFactory(httpConfiguration));
		if (getHttp2() != null && getHttp2().isEnabled()) {
			connectionFactories.add(new HTTP2CServerConnectionFactory(httpConfiguration));
		}
		ServerConnector connector = new ServerConnector(server, executor, scheduler, pool, this.getAcceptors(),
				this.getSelectors(), connectionFactories.toArray(new ConnectionFactory[0]));

		connector.setHost(address.getHostString());
		connector.setPort(address.getPort());
		return connector;
	}

	protected void customizeSsl(Server server, InetSocketAddress address) {
		Assert.state(getSsl().getServerNameBundles().isEmpty(), "Server name SSL bundles are not supported with Jetty");
		new SslServerCustomizer(getHttp2(), address, getSsl().getClientAuth(), getSslBundle()).customize(server);
	}

	protected Handler addHandlerWrappers(Handler handler) {
		if (getCompression() != null && getCompression().getEnabled()) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
		}
		if (StringUtils.hasText(getServerHeader())) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createServerHeaderHandlerWrapper(getServerHeader()));
		}
		return handler;
	}

	protected Handler applyWrapper(Handler handler, Handler.Wrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

}
