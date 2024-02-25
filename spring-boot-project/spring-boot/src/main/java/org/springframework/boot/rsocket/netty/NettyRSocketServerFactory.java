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

package org.springframework.boot.rsocket.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;
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

	/**
	 * Sets the port number for the server.
	 * @param port the port number to set
	 */
	@Override
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Sets the fragment size for the RSocket server.
	 * @param fragmentSize the fragment size to be set
	 */
	@Override
	public void setFragmentSize(DataSize fragmentSize) {
		this.fragmentSize = fragmentSize;
	}

	/**
	 * Sets the address of the server.
	 * @param address the InetAddress representing the server address
	 */
	@Override
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Sets the transport for the RSocket server.
	 * @param transport the transport to be set
	 */
	@Override
	public void setTransport(RSocketServer.Transport transport) {
		this.transport = transport;
	}

	/**
	 * Sets the SSL configuration for the Netty RSocket server.
	 * @param ssl the SSL configuration to be set
	 */
	@Override
	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	/**
	 * Sets the SSL bundles for the Netty RSocket server.
	 * @param sslBundles the SSL bundles to be set
	 */
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
		Assert.notNull(rSocketServerCustomizers, "RSocketServerCustomizers must not be null");
		this.rSocketServerCustomizers = new ArrayList<>(rSocketServerCustomizers);
	}

	/**
	 * Add {@link RSocketServerCustomizer}s that should be called to configure the
	 * {@link io.rsocket.core.RSocketServer}.
	 * @param rSocketServerCustomizers customizers to apply before the server starts
	 * @since 2.2.7
	 */
	public void addRSocketServerCustomizers(RSocketServerCustomizer... rSocketServerCustomizers) {
		Assert.notNull(rSocketServerCustomizers, "RSocketServerCustomizers must not be null");
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

	/**
	 * Creates a NettyRSocketServer instance with the given SocketAcceptor.
	 * @param socketAcceptor the SocketAcceptor to be used by the server
	 * @return a NettyRSocketServer instance
	 */
	@Override
	public NettyRSocketServer create(SocketAcceptor socketAcceptor) {
		ServerTransport<CloseableChannel> transport = createTransport();
		io.rsocket.core.RSocketServer server = io.rsocket.core.RSocketServer.create(socketAcceptor);
		configureServer(server);
		Mono<CloseableChannel> starter = server.bind(transport);
		return new NettyRSocketServer(starter, this.lifecycleTimeout);
	}

	/**
	 * Configures the RSocket server with the provided server instance.
	 * @param server the RSocket server instance to be configured
	 */
	private void configureServer(io.rsocket.core.RSocketServer server) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.fragmentSize).asInt(DataSize::toBytes).to(server::fragment);
		this.rSocketServerCustomizers.forEach((customizer) -> customizer.customize(server));
	}

	/**
	 * Creates a transport for the server based on the configured transport type.
	 * @return the created transport
	 */
	private ServerTransport<CloseableChannel> createTransport() {
		if (this.transport == RSocketServer.Transport.WEBSOCKET) {
			return createWebSocketTransport();
		}
		return createTcpTransport();
	}

	/**
	 * Creates a WebSocket transport for the server.
	 * @return the WebSocket transport
	 */
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

	/**
	 * Customizes the SSL configuration of the given {@link HttpServer} instance.
	 * @param httpServer the {@link HttpServer} instance to customize
	 * @return the customized {@link HttpServer} instance
	 */
	private HttpServer customizeSslConfiguration(HttpServer httpServer) {
		return new SslServerCustomizer(null, this.ssl.getClientAuth(), getSslBundle()).apply(httpServer);
	}

	/**
	 * Creates a TCP transport for the server.
	 * @return the created TCP transport
	 */
	private ServerTransport<CloseableChannel> createTcpTransport() {
		TcpServer tcpServer = TcpServer.create();
		if (this.resourceFactory != null) {
			tcpServer = tcpServer.runOn(this.resourceFactory.getLoopResources());
		}
		if (Ssl.isEnabled(this.ssl)) {
			tcpServer = new TcpSslServerCustomizer(this.ssl.getClientAuth(), getSslBundle()).apply(tcpServer);
		}
		return TcpServerTransport.create(tcpServer.bindAddress(this::getListenAddress));
	}

	/**
	 * Retrieves the SSL bundle for the Netty RSocket server.
	 * @return the SSL bundle for the server
	 */
	private SslBundle getSslBundle() {
		return WebServerSslBundle.get(this.ssl, this.sslBundles);
	}

	/**
	 * Returns the listen address for the Netty RSocket server.
	 * @return the listen address as an InetSocketAddress
	 */
	private InetSocketAddress getListenAddress() {
		if (this.address != null) {
			return new InetSocketAddress(this.address.getHostAddress(), this.port);
		}
		return new InetSocketAddress(this.port);
	}

	/**
	 * TcpSslServerCustomizer class.
	 */
	private static final class TcpSslServerCustomizer
			extends org.springframework.boot.web.embedded.netty.SslServerCustomizer {

		private final SslBundle sslBundle;

		/**
		 * Constructs a new TcpSslServerCustomizer with the specified client
		 * authentication mode and SSL bundle.
		 * @param clientAuth the client authentication mode to be used
		 * @param sslBundle the SSL bundle containing the SSL certificate, private key,
		 * and truststore
		 */
		private TcpSslServerCustomizer(Ssl.ClientAuth clientAuth, SslBundle sslBundle) {
			super(null, clientAuth, sslBundle);
			this.sslBundle = sslBundle;
		}

		/**
		 * Applies SSL configuration to the given TCP server.
		 * @param server the TCP server to apply SSL configuration to
		 * @return the TCP server with SSL configuration applied
		 */
		private TcpServer apply(TcpServer server) {
			AbstractProtocolSslContextSpec<?> sslContextSpec = createSslContextSpec(this.sslBundle);
			return server.secure((spec) -> spec.sslContext(sslContextSpec));
		}

	}

}
