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

package org.springframework.boot.autoconfigure.rsocket;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.WebsocketServerSpec;

import org.springframework.boot.autoconfigure.rsocket.RSocketProperties.Server.Spec;
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

	private final Spec spec;

	RSocketWebSocketNettyRouteProvider(String mappingPath, Spec spec, SocketAcceptor socketAcceptor,
			Stream<RSocketServerCustomizer> customizers) {
		this.mappingPath = mappingPath;
		this.socketAcceptor = socketAcceptor;
		this.customizers = customizers.collect(Collectors.toList());
		this.spec = spec;
	}

	@Override
	public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
		RSocketServer server = RSocketServer.create(this.socketAcceptor);
		this.customizers.forEach((customizer) -> customizer.customize(server));
		ServerTransport.ConnectionAcceptor connectionAcceptor = server.asConnectionAcceptor();
		WebsocketServerSpec.Builder build = (this.spec.getProtocols() == null) ? WebsocketServerSpec.builder()
				: WebsocketServerSpec.builder().protocols(this.spec.getProtocols());
		return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(connectionAcceptor),
				build.maxFramePayloadLength(this.spec.getMaxFramePayloadLength()).handlePing(this.spec.isHandlePing())
						.compress(this.spec.isCompress()).build());
	}

}
