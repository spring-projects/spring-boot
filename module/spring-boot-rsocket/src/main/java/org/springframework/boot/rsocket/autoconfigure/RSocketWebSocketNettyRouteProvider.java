/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.autoconfigure;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.WebsocketServerSpec;
import reactor.netty.http.server.WebsocketServerSpec.Builder;

import org.springframework.boot.reactor.netty.NettyRouteProvider;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;

/**
 * {@link NettyRouteProvider} that configures an RSocket Websocket endpoint.
 *
 * @author Brian Clozel
 * @author Leo Li
 */
class RSocketWebSocketNettyRouteProvider implements NettyRouteProvider {

	private final String mappingPath;

	private final SocketAcceptor socketAcceptor;

	private final List<RSocketServerCustomizer> customizers;

	private final Consumer<Builder> serverSpecCustomizer;

	RSocketWebSocketNettyRouteProvider(String mappingPath, SocketAcceptor socketAcceptor,
			Consumer<Builder> serverSpecCustomizer, Stream<RSocketServerCustomizer> customizers) {
		this.mappingPath = mappingPath;
		this.socketAcceptor = socketAcceptor;
		this.serverSpecCustomizer = serverSpecCustomizer;
		this.customizers = customizers.toList();
	}

	@Override
	public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
		RSocketServer server = RSocketServer.create(this.socketAcceptor);
		this.customizers.forEach((customizer) -> customizer.customize(server));
		ServerTransport.ConnectionAcceptor connectionAcceptor = server.asConnectionAcceptor();
		return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(connectionAcceptor),
				createWebsocketServerSpec());
	}

	private WebsocketServerSpec createWebsocketServerSpec() {
		WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder();
		this.serverSpecCustomizer.accept(builder);
		return builder.build();
	}

}
