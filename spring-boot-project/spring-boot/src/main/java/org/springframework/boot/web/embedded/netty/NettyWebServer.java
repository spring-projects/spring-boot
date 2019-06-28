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

package org.springframework.boot.web.embedded.netty;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.ChannelBindException;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerRoutes;

import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link WebServer} that can be used to control a Reactor Netty web server. Usually this
 * class should be created using the {@link NettyReactiveWebServerFactory} and not
 * directly.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class NettyWebServer implements WebServer {

	private static final Predicate<HttpServerRequest> ALWAYS = (r) -> true;

	private static final Log logger = LogFactory.getLog(NettyWebServer.class);

	private final HttpServer httpServer;

	private final ReactorHttpHandlerAdapter handlerAdapter;

	private final Duration lifecycleTimeout;

	private List<NettyRouteProvider> routeProviders = Collections.emptyList();

	private DisposableServer disposableServer;

	public NettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout) {
		Assert.notNull(httpServer, "HttpServer must not be null");
		Assert.notNull(handlerAdapter, "HandlerAdapter must not be null");
		this.httpServer = httpServer;
		this.handlerAdapter = handlerAdapter;
		this.lifecycleTimeout = lifecycleTimeout;
	}

	public void setRouteProviders(List<NettyRouteProvider> routeProviders) {
		this.routeProviders = routeProviders;
	}

	@Override
	public void start() throws WebServerException {
		if (this.disposableServer == null) {
			try {
				this.disposableServer = startHttpServer();
			}
			catch (Exception ex) {
				ChannelBindException bindException = findBindException(ex);
				if (bindException != null) {
					throw new PortInUseException(bindException.localPort());
				}
				throw new WebServerException("Unable to start Netty", ex);
			}
			logger.info("Netty started on port(s): " + getPort());
			startDaemonAwaitThread(this.disposableServer);
		}
	}

	private DisposableServer startHttpServer() {
		HttpServer server = this.httpServer;
		if (this.routeProviders.isEmpty()) {
			server = server.handle(this.handlerAdapter);
		}
		else {
			server = server.route(this::applyRouteProviders);
		}
		if (this.lifecycleTimeout != null) {
			return server.bindNow(this.lifecycleTimeout);
		}
		return server.bindNow();
	}

	private void applyRouteProviders(HttpServerRoutes routes) {
		this.routeProviders.forEach((provider) -> provider.apply(routes));
		routes.route(ALWAYS, this.handlerAdapter);
	}

	private ChannelBindException findBindException(Exception ex) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (candidate instanceof ChannelBindException) {
				return (ChannelBindException) candidate;
			}
			candidate = candidate.getCause();
		}
		return null;
	}

	private void startDaemonAwaitThread(DisposableServer disposableServer) {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				disposableServer.onDispose().block();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void stop() throws WebServerException {
		if (this.disposableServer != null) {
			if (this.lifecycleTimeout != null) {
				this.disposableServer.disposeNow(this.lifecycleTimeout);
			}
			else {
				this.disposableServer.disposeNow();
			}
			this.disposableServer = null;
		}
	}

	@Override
	public int getPort() {
		if (this.disposableServer != null) {
			return this.disposableServer.port();
		}
		return 0;
	}

}
