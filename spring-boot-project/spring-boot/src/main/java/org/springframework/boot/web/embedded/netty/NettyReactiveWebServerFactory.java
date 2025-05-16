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

package org.springframework.boot.web.embedded.netty;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @since 2.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private Set<NettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

	private final List<NettyRouteProvider> routeProviders = new ArrayList<>();

	private Duration lifecycleTimeout;

	private boolean useForwardHeaders;

	private ReactorResourceFactory resourceFactory;

	private Shutdown shutdown;

	public NettyReactiveWebServerFactory() {
	}

	public NettyReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer httpServer = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
		NettyWebServer webServer = createNettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout,
				getShutdown());
		webServer.setRouteProviders(this.routeProviders);
		return webServer;
	}

	NettyWebServer createNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
			Duration lifecycleTimeout, Shutdown shutdown) {
		return new NettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown, this.resourceFactory);
	}

	/**
	 * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
	 * applied to the Netty server builder.
	 * @return the customizers that will be applied
	 */
	public Collection<NettyServerCustomizer> getServerCustomizers() {
		return this.serverCustomizers;
	}

	/**
	 * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
	 * builder. Calling this method will replace any existing customizers.
	 * @param serverCustomizers the customizers to set
	 */
	public void setServerCustomizers(Collection<? extends NettyServerCustomizer> serverCustomizers) {
		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
		this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should be applied while building the
	 * server.
	 * @param serverCustomizers the customizers to add
	 */
	public void addServerCustomizers(NettyServerCustomizer... serverCustomizers) {
		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
	}

	/**
	 * Add {@link NettyRouteProvider}s that should be applied, in order, before the
	 * handler for the Spring application.
	 * @param routeProviders the route providers to add
	 */
	public void addRouteProviders(NettyRouteProvider... routeProviders) {
		Assert.notNull(routeProviders, "'routeProviders' must not be null");
		this.routeProviders.addAll(Arrays.asList(routeProviders));
	}

	/**
	 * Set the maximum amount of time that should be waited when starting or stopping the
	 * server.
	 * @param lifecycleTimeout the lifecycle timeout
	 */
	public void setLifecycleTimeout(Duration lifecycleTimeout) {
		this.lifecycleTimeout = lifecycleTimeout;
	}

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 * @since 2.1.0
	 */
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 * @since 2.1.0
	 */
	public void setResourceFactory(ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	@Override
	public void setShutdown(Shutdown shutdown) {
		this.shutdown = shutdown;
	}

	@Override
	public Shutdown getShutdown() {
		return this.shutdown;
	}

	private HttpServer createHttpServer() {
		HttpServer server = HttpServer.create().bindAddress(this::getListenAddress);
		if (Ssl.isEnabled(getSsl())) {
			server = customizeSslConfiguration(server);
		}
		if (getCompression() != null && getCompression().getEnabled()) {
			CompressionCustomizer compressionCustomizer = new CompressionCustomizer(getCompression());
			server = compressionCustomizer.apply(server);
		}
		server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
		return applyCustomizers(server);
	}

	private HttpServer customizeSslConfiguration(HttpServer httpServer) {
		SslServerCustomizer customizer = new SslServerCustomizer(getHttp2(), getSsl().getClientAuth(), getSslBundle(),
				getServerNameSslBundles());
		addBundleUpdateHandler(null, getSsl().getBundle(), customizer);
		getSsl().getServerNameBundles()
			.forEach((serverNameSslBundle) -> addBundleUpdateHandler(serverNameSslBundle.serverName(),
					serverNameSslBundle.bundle(), customizer));
		return customizer.apply(httpServer);
	}

	private void addBundleUpdateHandler(String serverName, String bundleName, SslServerCustomizer customizer) {
		if (StringUtils.hasText(bundleName)) {
			getSslBundles().addBundleUpdateHandler(bundleName,
					(sslBundle) -> customizer.updateSslBundle(serverName, sslBundle));
		}
	}

	private HttpProtocol[] listProtocols() {
		List<HttpProtocol> protocols = new ArrayList<>();
		protocols.add(HttpProtocol.HTTP11);
		if (getHttp2() != null && getHttp2().isEnabled()) {
			if (getSsl() != null && getSsl().isEnabled()) {
				protocols.add(HttpProtocol.H2);
			}
			else {
				protocols.add(HttpProtocol.H2C);
			}
		}
		return protocols.toArray(new HttpProtocol[0]);
	}

	private InetSocketAddress getListenAddress() {
		if (getAddress() != null) {
			return new InetSocketAddress(getAddress().getHostAddress(), getPort());
		}
		return new InetSocketAddress(getPort());
	}

	private HttpServer applyCustomizers(HttpServer server) {
		for (NettyServerCustomizer customizer : this.serverCustomizers) {
			server = customizer.apply(server);
		}
		return server;
	}

}
