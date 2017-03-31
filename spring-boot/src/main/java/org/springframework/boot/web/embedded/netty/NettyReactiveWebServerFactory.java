/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.netty;

import reactor.ipc.netty.http.server.HttpServer;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	public NettyReactiveWebServerFactory() {
	}

	public NettyReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer server = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(
				httpHandler);
		return new NettyWebServer(server, handlerAdapter);
	}

	private HttpServer createHttpServer() {
		HttpServer server;
		if (getAddress() != null) {
			server = HttpServer.create(getAddress().getHostAddress(), getPort());
		}
		else {
			server = HttpServer.create(getPort());
		}
		return server;
	}

}
