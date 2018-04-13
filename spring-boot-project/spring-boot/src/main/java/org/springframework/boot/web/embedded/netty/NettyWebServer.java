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

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.ipc.netty.http.HttpResources;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.tcp.BlockingNettyContext;

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

	private BlockingNettyContext nettyContext;

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
		if (this.nettyContext == null) {
			try {
				this.nettyContext = startHttpServer();
			}
			catch (Exception ex) {
				if (findBindException(ex) != null) {
					SocketAddress address = this.httpServer.options().getAddress();
					if (address instanceof InetSocketAddress) {
						throw new PortInUseException(
								((InetSocketAddress) address).getPort());
					}
				}
				throw new WebServerException("Unable to start Netty", ex);
			}
			NettyWebServer.logger.info("Netty started on port(s): " + getPort());
			startDaemonAwaitThread(this.nettyContext);
		}
	}

	private BlockingNettyContext startHttpServer() {
		if (this.lifecycleTimeout != null) {
			return this.httpServer.start(this.handlerAdapter, this.lifecycleTimeout);
		}
		return this.httpServer.start(this.handlerAdapter);
	}

	private BindException findBindException(Exception ex) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (candidate instanceof BindException) {
				return (BindException) candidate;
			}
			candidate = candidate.getCause();
		}
		return null;
	}

	private void startDaemonAwaitThread(BlockingNettyContext nettyContext) {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				nettyContext.getContext().onClose().block();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void stop() throws WebServerException {
		if (this.nettyContext != null) {
			this.nettyContext.shutdown();
			// temporary fix for gh-9146
			this.nettyContext.getContext().onClose()
					.doOnSuccess((o) -> HttpResources.reset()).block();
			this.nettyContext = null;
		}
	}

	@Override
	public int getPort() {
		if (this.nettyContext != null) {
			return this.nettyContext.getPort();
		}
		return 0;
	}

}
