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

import java.net.BindException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * {@link WebServer} that can be used to control a Reactor Netty web server. Usually this
 * class should be created using the {@link NettyReactiveWebServerFactory} and not
 * directly.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class NettyWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(NettyWebServer.class);

	private CountDownLatch latch;

	private final ReactorHttpHandlerAdapter handlerAdapter;

	private final HttpServer reactorServer;

	private AtomicReference<NettyContext> nettyContext = new AtomicReference<>();

	public NettyWebServer(HttpServer reactorServer,
			ReactorHttpHandlerAdapter handlerAdapter) {
		this.reactorServer = reactorServer;
		this.handlerAdapter = handlerAdapter;
	}

	@Override
	public void start() throws WebServerException {
		if (this.nettyContext.get() == null) {
			this.latch = new CountDownLatch(1);
			try {
				this.nettyContext
						.set(this.reactorServer.newHandler(this.handlerAdapter).block());
			}
			catch (Exception ex) {
				if (findBindException(ex) != null) {
//					throw new PortInUseException();
				}
				throw new WebServerException("Unable to start Netty", ex);
			}
			NettyWebServer.logger.info("Netty started on port(s): " + getPort());
			startDaemonAwaitThread();
		}
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

	private void startDaemonAwaitThread() {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				try {
					NettyWebServer.this.latch.await();
				}
				catch (InterruptedException e) {
				}
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void stop() throws WebServerException {
		NettyContext context = this.nettyContext.getAndSet(null);
		if (context != null) {
			context.dispose();
		}
		this.latch.countDown();
	}

	@Override
	public int getPort() {
		if (this.nettyContext.get() != null) {
			return this.nettyContext.get().address().getPort();
		}
		return 0;
	}

}
