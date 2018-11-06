/*
 * Copyright 2012-2018 the original author or authors.
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

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.ChannelBindException;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

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

	private static final Log logger = LogFactory.getLog(NettyWebServer.class);

	private final HttpServer httpServer;

	private final ReactorHttpHandlerAdapter handlerAdapter;

	private final Duration lifecycleTimeout;

	private DisposableServer disposableServer;

	public NettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
			Duration lifecycleTimeout) {
		Assert.notNull(httpServer, "HttpServer must not be null");
		Assert.notNull(handlerAdapter, "HandlerAdapter must not be null");
		this.httpServer = httpServer;
		this.handlerAdapter = handlerAdapter;
		this.lifecycleTimeout = lifecycleTimeout;
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
		if (this.lifecycleTimeout != null) {
			return this.httpServer.handle(this.handlerAdapter)
					.bindNow(this.lifecycleTimeout);
		}
		return this.httpServer.handle(this.handlerAdapter).bindNow();
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
