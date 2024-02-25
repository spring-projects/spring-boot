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

package org.springframework.boot.autoconfigure.rsocket;

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

import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;

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

	/**
     * Constructs a new RSocketWebSocketNettyRouteProvider with the specified parameters.
     *
     * @param mappingPath the mapping path for the WebSocket route
     * @param socketAcceptor the socket acceptor for handling incoming connections
     * @param serverSpecCustomizer a consumer to customize the server spec
     * @param customizers a stream of RSocketServerCustomizer to customize the RSocket server
     */
    RSocketWebSocketNettyRouteProvider(String mappingPath, SocketAcceptor socketAcceptor,
			Consumer<Builder> serverSpecCustomizer, Stream<RSocketServerCustomizer> customizers) {
		this.mappingPath = mappingPath;
		this.socketAcceptor = socketAcceptor;
		this.serverSpecCustomizer = serverSpecCustomizer;
		this.customizers = customizers.toList();
	}

	/**
     * Applies the RSocket server configuration to the provided HttpServerRoutes.
     * 
     * @param httpServerRoutes the HttpServerRoutes to apply the RSocket server configuration to
     * @return the modified HttpServerRoutes with the RSocket server configuration applied
     */
    @Override
	public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
		RSocketServer server = RSocketServer.create(this.socketAcceptor);
		this.customizers.forEach((customizer) -> customizer.customize(server));
		ServerTransport.ConnectionAcceptor connectionAcceptor = server.asConnectionAcceptor();
		return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(connectionAcceptor),
				createWebsocketServerSpec());
	}

	/**
     * Creates a WebsocketServerSpec object with the specified serverSpecCustomizer.
     * 
     * @return the created WebsocketServerSpec object
     */
    private WebsocketServerSpec createWebsocketServerSpec() {
		WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder();
		this.serverSpecCustomizer.accept(builder);
		return builder.build();
	}

}
