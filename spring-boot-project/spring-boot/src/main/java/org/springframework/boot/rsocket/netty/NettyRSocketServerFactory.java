/*
 * Copyright 2012-2019 the original author or authors.
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

	private List<ServerRSocketFactoryProcessor> serverProcessors = new ArrayList<>();

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
	 * Set {@link ServerRSocketFactoryProcessor}s that should be applied to the RSocket
	 * server builder. Calling this method will replace any existing customizers.
	 * @param serverProcessors server processors to apply before the server starts
	 */
	public void setServerProcessors(Collection<? extends ServerRSocketFactoryProcessor> serverProcessors) {
		Assert.notNull(serverProcessors, "ServerProcessors must not be null");
		this.serverProcessors = new ArrayList<>(serverProcessors);
	}

	/**
	 * Add {@link ServerRSocketFactoryProcessor}s that should applied while building the
	 * server.
	 * @param serverProcessors server processors to apply before the server starts
	 */
	public void addServerProcessors(ServerRSocketFactoryProcessor... serverProcessors) {
		Assert.notNull(serverProcessors, "ServerProcessors must not be null");
		this.serverProcessors.addAll(Arrays.asList(serverProcessors));
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
		RSocketFactory.ServerRSocketFactory factory = RSocketFactory.receive();
		for (ServerRSocketFactoryProcessor processor : this.serverProcessors) {
			factory = processor.process(factory);
		}
		Mono<CloseableChannel> starter = factory.acceptor(socketAcceptor).transport(transport).start();
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
