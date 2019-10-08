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

package org.springframework.boot.autoconfigure.rsocket;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import reactor.netty.http.server.HttpServerRoutes;

import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;

/**
 * {@link NettyRouteProvider} that configures an RSocket Websocket endpoint.
 *
 * @author Brian Clozel
 */
class RSocketWebSocketNettyRouteProvider implements NettyRouteProvider {

	private final String mappingPath;

	private final SocketAcceptor socketAcceptor;

	private final List<ServerRSocketFactoryProcessor> processors;

	RSocketWebSocketNettyRouteProvider(String mappingPath, SocketAcceptor socketAcceptor,
			Stream<ServerRSocketFactoryProcessor> processors) {
		this.mappingPath = mappingPath;
		this.socketAcceptor = socketAcceptor;
		this.processors = processors.collect(Collectors.toList());
	}

	@Override
	public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
		RSocketFactory.ServerRSocketFactory server = RSocketFactory.receive();
		for (ServerRSocketFactoryProcessor processor : this.processors) {
			server = processor.process(server);
		}
		ServerTransport.ConnectionAcceptor acceptor = server.acceptor(this.socketAcceptor).toConnectionAcceptor();
		return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(acceptor));
	}

}
