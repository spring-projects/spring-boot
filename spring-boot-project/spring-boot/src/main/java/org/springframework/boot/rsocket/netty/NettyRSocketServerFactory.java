/*
 * Copyright 2012-2020 the original author or authors.
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

import io.rsocket.RSocketFactory;
import io.rsocket.RSocketFactory.ServerRSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.rsocket.server.ConfigurableRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.util.Assert;

/**
 * {@link RSocketServerFactory} that can be used to create {@link RSocketServer}s backed
 * by Netty.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class NettyRSocketServerFactory implements RSocketServerFactory, ConfigurableRSocketServerFactory {

	private int port = 9898;

	private InetAddress address;

	private RSocketServer.Transport transport = RSocketServer.Transport.TCP;

	private ReactorResourceFactory resourceFactory;

	private Duration lifecycleTimeout;

	private List<ServerRSocketFactoryProcessor> socketFactoryProcessors = new ArrayList<>();

	private List<RSocketServerCustomizer> rSocketServerCustomizers = new ArrayList<>();

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	@Override
	public void setTransport(RSocketServer.Transport transport) {
		this.transport = transport;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 */
	public void setResourceFactory(ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	/**
	 * Set {@link ServerRSocketFactoryProcessor}s that should be called to process the
	 * {@link ServerRSocketFactory} while building the server. Calling this method will
	 * replace any existing processors.
	 * @param socketFactoryProcessors processors to apply before the server starts
	 * @deprecated in favor of {@link #setRSocketServerCustomizers(Collection)} as of
	 * 2.2.7
	 */
	@Deprecated
	public void setSocketFactoryProcessors(
			Collection<? extends ServerRSocketFactoryProcessor> socketFactoryProcessors) {
		Assert.notNull(socketFactoryProcessors, "SocketFactoryProcessors must not be null");
		this.socketFactoryProcessors = new ArrayList<>(socketFactoryProcessors);
	}

	/**
	 * Add {@link ServerRSocketFactoryProcessor}s that should be called to process the
	 * {@link ServerRSocketFactory} while building the server.
	 * @param socketFactoryProcessors processors to apply before the server starts
	 * @deprecated in favor of
	 * {@link #addRSocketServerCustomizers(RSocketServerCustomizer...)} as of 2.2.7
	 */
	@Deprecated
	public void addSocketFactoryProcessors(ServerRSocketFactoryProcessor... socketFactoryProcessors) {
		Assert.notNull(socketFactoryProcessors, "SocketFactoryProcessors must not be null");
		this.socketFactoryProcessors.addAll(Arrays.asList(socketFactoryProcessors));
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

	@Override
	@SuppressWarnings("deprecation")
	public NettyRSocketServer create(SocketAcceptor socketAcceptor) {
		ServerTransport<CloseableChannel> transport = createTransport();
		io.rsocket.core.RSocketServer server = io.rsocket.core.RSocketServer.create(socketAcceptor);
		RSocketFactory.ServerRSocketFactory factory = new ServerRSocketFactory(server);
		this.rSocketServerCustomizers.forEach((customizer) -> customizer.customize(server));
		this.socketFactoryProcessors.forEach((processor) -> processor.process(factory));
		Mono<CloseableChannel> starter = server.bind(transport);
		return new NettyRSocketServer(starter, this.lifecycleTimeout);
	}

	private ServerTransport<CloseableChannel> createTransport() {
		if (this.transport == RSocketServer.Transport.WEBSOCKET) {
			return createWebSocketTransport();
		}
		return createTcpTransport();
	}

	private ServerTransport<CloseableChannel> createWebSocketTransport() {
		if (this.resourceFactory != null) {
			HttpServer httpServer = HttpServer.create().tcpConfiguration((tcpServer) -> tcpServer
					.runOn(this.resourceFactory.getLoopResources()).addressSupplier(this::getListenAddress));
			return WebsocketServerTransport.create(httpServer);
		}
		return WebsocketServerTransport.create(getListenAddress());
	}

	private ServerTransport<CloseableChannel> createTcpTransport() {
		if (this.resourceFactory != null) {
			TcpServer tcpServer = TcpServer.create().runOn(this.resourceFactory.getLoopResources())
					.addressSupplier(this::getListenAddress);
			return TcpServerTransport.create(tcpServer);
		}
		return TcpServerTransport.create(getListenAddress());
	}

	private InetSocketAddress getListenAddress() {
		if (this.address != null) {
			return new InetSocketAddress(this.address.getHostAddress(), this.port);
		}
		return new InetSocketAddress(this.port);
	}

}
