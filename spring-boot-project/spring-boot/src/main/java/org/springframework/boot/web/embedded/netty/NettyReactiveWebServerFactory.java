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
 * @since 2.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private Set<NettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

	private final List<NettyRouteProvider> routeProviders = new ArrayList<>();

	private Duration lifecycleTimeout;

	private boolean useForwardHeaders;

	private ReactorResourceFactory resourceFactory;

	private Shutdown shutdown;

	/**
     * Constructs a new NettyReactiveWebServerFactory.
     */
    public NettyReactiveWebServerFactory() {
	}

	/**
     * Constructs a new NettyReactiveWebServerFactory with the specified port.
     *
     * @param port the port number to bind the server to
     */
    public NettyReactiveWebServerFactory(int port) {
		super(port);
	}

	/**
     * Returns a {@link WebServer} instance using Netty as the underlying server implementation.
     * 
     * @param httpHandler the {@link HttpHandler} to be used for handling HTTP requests
     * @return a {@link WebServer} instance
     */
    @Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer httpServer = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
		NettyWebServer webServer = createNettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout,
				getShutdown());
		webServer.setRouteProviders(this.routeProviders);
		return webServer;
	}

	/**
     * Creates a new NettyWebServer instance with the given parameters.
     *
     * @param httpServer        the HttpServer to be used by the NettyWebServer
     * @param handlerAdapter    the ReactorHttpHandlerAdapter to be used by the NettyWebServer
     * @param lifecycleTimeout  the duration for the server's lifecycle timeout
     * @param shutdown          the Shutdown instance to be used by the NettyWebServer
     * @return                  a new NettyWebServer instance
     */
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
		Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
		this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should be applied while building the
	 * server.
	 * @param serverCustomizers the customizers to add
	 */
	public void addServerCustomizers(NettyServerCustomizer... serverCustomizers) {
		Assert.notNull(serverCustomizers, "ServerCustomizer must not be null");
		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
	}

	/**
	 * Add {@link NettyRouteProvider}s that should be applied, in order, before the
	 * handler for the Spring application.
	 * @param routeProviders the route providers to add
	 */
	public void addRouteProviders(NettyRouteProvider... routeProviders) {
		Assert.notNull(routeProviders, "NettyRouteProvider must not be null");
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

	/**
     * Sets the shutdown hook for the Netty Reactive Web Server.
     * 
     * @param shutdown the shutdown hook to be set
     */
    @Override
	public void setShutdown(Shutdown shutdown) {
		this.shutdown = shutdown;
	}

	/**
     * Returns the shutdown object associated with this NettyReactiveWebServerFactory.
     *
     * @return the shutdown object
     */
    @Override
	public Shutdown getShutdown() {
		return this.shutdown;
	}

	/**
     * Creates an instance of HttpServer.
     * 
     * @return the created HttpServer instance
     */
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

	/**
     * Customizes the SSL configuration of the given {@link HttpServer} instance.
     * 
     * @param httpServer the {@link HttpServer} instance to customize
     * @return the customized {@link HttpServer} instance
     */
    private HttpServer customizeSslConfiguration(HttpServer httpServer) {
		SslServerCustomizer customizer = new SslServerCustomizer(getHttp2(), getSsl().getClientAuth(), getSslBundle());
		String bundleName = getSsl().getBundle();
		if (StringUtils.hasText(bundleName)) {
			getSslBundles().addBundleUpdateHandler(bundleName, customizer::updateSslBundle);
		}
		return customizer.apply(httpServer);
	}

	/**
     * Returns an array of supported HTTP protocols.
     * 
     * @return an array of supported HTTP protocols
     */
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

	/**
     * Returns the listen address for the server.
     * If the address is not null, it creates a new InetSocketAddress using the host address and port.
     * If the address is null, it creates a new InetSocketAddress using only the port.
     *
     * @return the listen address for the server
     */
    private InetSocketAddress getListenAddress() {
		if (getAddress() != null) {
			return new InetSocketAddress(getAddress().getHostAddress(), getPort());
		}
		return new InetSocketAddress(getPort());
	}

	/**
     * Applies the customizers to the given HttpServer.
     * 
     * @param server the HttpServer to apply the customizers to
     * @return the HttpServer with the customizers applied
     */
    private HttpServer applyCustomizers(HttpServer server) {
		for (NettyServerCustomizer customizer : this.serverCustomizers) {
			server = customizer.apply(server);
		}
		return server;
	}

}
