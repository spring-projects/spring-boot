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

package org.springframework.boot.rsocket.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.SslProvider.GenericSslContextSpec;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.server.ConfigurableRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.embedded.netty.SslServerCustomizer;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.Ssl.ServerNameSslBundle;
import org.springframework.boot.web.server.WebServerSslBundle;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;

/**
 * {@link RSocketServerFactory} that can be used to create {@link RSocketServer}s backed
 * by Netty.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @author Scott Frederick
 * @since 2.2.0
 */
public class NettyRSocketServerFactory implements RSocketServerFactory, ConfigurableRSocketServerFactory {

	private int port = 9898;

	private DataSize fragmentSize;

	private InetAddress address;

	private RSocketServer.Transport transport = RSocketServer.Transport.TCP;

	private ReactorResourceFactory resourceFactory;

	private Duration lifecycleTimeout;

	private List<RSocketServerCustomizer> rSocketServerCustomizers = new ArrayList<>();

	private Ssl ssl;

	private SslBundles sslBundles;

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void setFragmentSize(DataSize fragmentSize) {
		this.fragmentSize = fragmentSize;
	}

	@Override
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	@Override
	public void setTransport(RSocketServer.Transport transport) {
		this.transport = transport;
	}

	@Override
	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	@Override
	public void setSslBundles(SslBundles sslBundles) {
		this.sslBundles = sslBundles;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 */
	public void setResourceFactory(ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	/**
	 * Set {@link RSocketServerCustomizer}s that should be called to configure the
	 * {@link io.rsocket.core.RSocketServer} while building the server. Calling this
	 * method will replace any existing customizers.
	 * @param rSocketServerCustomizers customizers to apply before the server starts
	 * @since 2.2.7
	 */
	public void setRSocketServerCustomizers(Collection<? extends RSocketServerCustomizer> rSocketServerCustomizers) {
		Assert.notNull(rSocketServerCustomizers, "'rSocketServerCustomizers' must not be null");
		this.rSocketServerCustomizers = new ArrayList<>(rSocketServerCustomizers);
	}

	/**
	 * Add {@link RSocketServerCustomizer}s that should be called to configure the
	 * {@link io.rsocket.core.RSocketServer}.
	 * @param rSocketServerCustomizers customizers to apply before the server starts
	 * @since 2.2.7
	 */
	public void addRSocketServerCustomizers(RSocketServerCustomizer... rSocketServerCustomizers) {
		Assert.notNull(rSocketServerCustomizers, "'rSocketServerCustomizers' must not be null");
		this.rSocketServerCustomizers.addAll(Arrays.asList(rSocketServerCustomizers));
	}

	/**
	 * Set the maximum amount of time that should be waited when starting or stopping the
	 * server.
	 * @param lifecycleTimeout the lifecycle timeout
	 */
	public void setLifecycleTimeout(Duration lifecycleTimeout) {
		this.lifecycleTimeout = lifecycleTimeout;
	}

	@Override
	public NettyRSocketServer create(SocketAcceptor socketAcceptor) {
		ServerTransport<CloseableChannel> transport = createTransport();
		io.rsocket.core.RSocketServer server = io.rsocket.core.RSocketServer.create(socketAcceptor);
		configureServer(server);
		Mono<CloseableChannel> starter = server.bind(transport);
		return new NettyRSocketServer(starter, this.lifecycleTimeout);
	}

	private void configureServer(io.rsocket.core.RSocketServer server) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.fragmentSize).asInt(DataSize::toBytes).to(server::fragment);
		this.rSocketServerCustomizers.forEach((customizer) -> customizer.customize(server));
	}

	private ServerTransport<CloseableChannel> createTransport() {
		if (this.transport == RSocketServer.Transport.WEBSOCKET) {
			return createWebSocketTransport();
		}
		return createTcpTransport();
	}

	private ServerTransport<CloseableChannel> createWebSocketTransport() {
		HttpServer httpServer = HttpServer.create();
		if (this.resourceFactory != null) {
			httpServer = httpServer.runOn(this.resourceFactory.getLoopResources());
		}
		if (Ssl.isEnabled(this.ssl)) {
			httpServer = customizeSslConfiguration(httpServer);
		}
		return WebsocketServerTransport.create(httpServer.bindAddress(this::getListenAddress));
	}

	private HttpServer customizeSslConfiguration(HttpServer httpServer) {
		return new SslServerCustomizer(null, this.ssl.getClientAuth(), getSslBundle(), getServerNameSslBundles())
			.apply(httpServer);
	}

	private ServerTransport<CloseableChannel> createTcpTransport() {
		TcpServer tcpServer = TcpServer.create();
		if (this.resourceFactory != null) {
			tcpServer = tcpServer.runOn(this.resourceFactory.getLoopResources());
		}
		if (Ssl.isEnabled(this.ssl)) {
			tcpServer = new TcpSslServerCustomizer(this.ssl.getClientAuth(), getSslBundle(), getServerNameSslBundles())
				.apply(tcpServer);
		}
		return TcpServerTransport.create(tcpServer.bindAddress(this::getListenAddress));
	}

	private SslBundle getSslBundle() {
		return WebServerSslBundle.get(this.ssl, this.sslBundles);
	}

	protected final Map<String, SslBundle> getServerNameSslBundles() {
		return this.ssl.getServerNameBundles()
			.stream()
			.collect(Collectors.toMap(Ssl.ServerNameSslBundle::serverName, this::getBundle));
	}

	private SslBundle getBundle(ServerNameSslBundle serverNameSslBundle) {
		return this.sslBundles.getBundle(serverNameSslBundle.bundle());
	}

	private InetSocketAddress getListenAddress() {
		if (this.address != null) {
			return new InetSocketAddress(this.address.getHostAddress(), this.port);
		}
		return new InetSocketAddress(this.port);
	}

	private static final class TcpSslServerCustomizer
			extends org.springframework.boot.web.embedded.netty.SslServerCustomizer {

		private final SslBundle sslBundle;

		private TcpSslServerCustomizer(ClientAuth clientAuth, SslBundle sslBundle,
				Map<String, SslBundle> serverNameSslBundles) {
			super(null, clientAuth, sslBundle, serverNameSslBundles);
			this.sslBundle = sslBundle;
		}

		private TcpServer apply(TcpServer server) {
			GenericSslContextSpec<?> sslContextSpec = createSslContextSpec(this.sslBundle);
			return server.secure((spec) -> spec.sslContext(sslContextSpec));
		}

	}

}
