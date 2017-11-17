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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerOptions.Builder;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private List<NettyServerCustomizer> nettyServerCustomizers = new ArrayList<>();

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

	/**
	 * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
	 * applied to the Netty server builder.
	 * @return the customizers that will be applied
	 */
	public Collection<NettyServerCustomizer> getNettyServerCustomizers() {
		return this.nettyServerCustomizers;
	}

	/**
	 * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
	 * builder. Calling this method will replace any existing customizers.
	 * @param nettyServerCustomizers the customizers to set
	 */
	public void setNettyServerCustomizers(
			Collection<? extends NettyServerCustomizer> nettyServerCustomizers) {
		Assert.notNull(nettyServerCustomizers, "NettyServerCustomizers must not be null");
		this.nettyServerCustomizers = new ArrayList<>(nettyServerCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should applied while building the server.
	 * @param nettyServerCustomizer the customizers to add
	 */
	public void addContextCustomizers(NettyServerCustomizer... nettyServerCustomizer) {
		Assert.notNull(nettyServerCustomizer,
				"NettyWebServerCustomizer must not be null");
		this.nettyServerCustomizers.addAll(Arrays.asList(nettyServerCustomizer));
	}

	private HttpServer createHttpServer() {
		return HttpServer.builder().options((options) -> {
			options.listenAddress(getListenAddress());
			if (getSsl() != null && getSsl().isEnabled()) {
				SslServerCustomizer sslServerCustomizer = new SslServerCustomizer(
						getSsl(), getSslStoreProvider());
				sslServerCustomizer.customize(options);
			}
			applyCustomizers(options);
		}).build();
	}

	private InetSocketAddress getListenAddress() {
		if (getAddress() != null) {
			return new InetSocketAddress(getAddress().getHostAddress(), getPort());
		}
		return new InetSocketAddress(getPort());
	}

	private void applyCustomizers(Builder options) {
		this.nettyServerCustomizers
				.forEach((customizer) -> customizer.customize(options));
	}

}
