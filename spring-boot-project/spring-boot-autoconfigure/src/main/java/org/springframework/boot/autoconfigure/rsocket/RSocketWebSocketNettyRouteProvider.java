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

import io.rsocket.RSocketFactory;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import reactor.netty.http.server.HttpServerRoutes;

import org.springframework.boot.web.embedded.netty.NettyRouteProvider;
import org.springframework.messaging.rsocket.MessageHandlerAcceptor;

/**
 * {@link NettyRouteProvider} that configures an RSocket Websocket endpoint.
 *
 * @author Brian Clozel
 */
class RSocketWebSocketNettyRouteProvider implements NettyRouteProvider {

	private final String mappingPath;

	private final MessageHandlerAcceptor messageHandlerAcceptor;

	RSocketWebSocketNettyRouteProvider(String mappingPath, MessageHandlerAcceptor messageHandlerAcceptor) {
		this.mappingPath = mappingPath;
		this.messageHandlerAcceptor = messageHandlerAcceptor;
	}

	@Override
	public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
		ServerTransport.ConnectionAcceptor acceptor = RSocketFactory.receive().acceptor(this.messageHandlerAcceptor)
				.toConnectionAcceptor();
		return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(acceptor));
	}

}
