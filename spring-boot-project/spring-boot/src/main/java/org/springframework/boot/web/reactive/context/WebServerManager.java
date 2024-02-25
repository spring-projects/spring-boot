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

package org.springframework.boot.web.reactive.context;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Internal class used to manage the server and the {@link HttpHandler}, taking care not
 * to initialize the handler too early.
 *
 * @author Andy Wilkinson
 */
class WebServerManager {

	private final ReactiveWebServerApplicationContext applicationContext;

	private final DelayedInitializationHttpHandler handler;

	private final WebServer webServer;

	/**
	 * Constructs a new WebServerManager with the specified parameters.
	 * @param applicationContext the ReactiveWebServerApplicationContext associated with
	 * the WebServerManager
	 * @param factory the ReactiveWebServerFactory used to create the web server
	 * @param handlerSupplier the supplier for the HttpHandler used by the web server
	 * @param lazyInit a boolean indicating whether lazy initialization should be enabled
	 * @throws IllegalArgumentException if the factory is null
	 */
	WebServerManager(ReactiveWebServerApplicationContext applicationContext, ReactiveWebServerFactory factory,
			Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
		this.applicationContext = applicationContext;
		Assert.notNull(factory, "Factory must not be null");
		this.handler = new DelayedInitializationHttpHandler(handlerSupplier, lazyInit);
		this.webServer = factory.getWebServer(this.handler);
	}

	/**
	 * Starts the web server and initializes the handler.
	 *
	 * This method initializes the handler by calling the initializeHandler() method of
	 * the handler object. It then starts the web server by calling the start() method of
	 * the webServer object. Finally, it publishes a ReactiveWebServerInitializedEvent to
	 * the applicationContext.
	 *
	 * @see Handler#initializeHandler()
	 * @see WebServer#start()
	 * @see ReactiveWebServerInitializedEvent
	 */
	void start() {
		this.handler.initializeHandler();
		this.webServer.start();
		this.applicationContext
			.publishEvent(new ReactiveWebServerInitializedEvent(this.webServer, this.applicationContext));
	}

	/**
	 * Shuts down the web server gracefully.
	 * @param callback the callback function to be executed after the shutdown is complete
	 */
	void shutDownGracefully(GracefulShutdownCallback callback) {
		this.webServer.shutDownGracefully(callback);
	}

	/**
	 * Stops the web server.
	 */
	void stop() {
		this.webServer.stop();
	}

	/**
	 * Returns the WebServer object associated with this WebServerManager.
	 * @return the WebServer object associated with this WebServerManager
	 */
	WebServer getWebServer() {
		return this.webServer;
	}

	/**
	 * Returns the HttpHandler object associated with this WebServerManager.
	 * @return the HttpHandler object associated with this WebServerManager
	 */
	HttpHandler getHandler() {
		return this.handler;
	}

	/**
	 * A delayed {@link HttpHandler} that doesn't initialize things too early.
	 */
	static final class DelayedInitializationHttpHandler implements HttpHandler {

		private final Supplier<HttpHandler> handlerSupplier;

		private final boolean lazyInit;

		private volatile HttpHandler delegate = this::handleUninitialized;

		/**
		 * Constructs a new DelayedInitializationHttpHandler with the specified handler
		 * supplier and lazy initialization flag.
		 * @param handlerSupplier the supplier of the HttpHandler to be used for handling
		 * HTTP requests
		 * @param lazyInit a boolean flag indicating whether the HttpHandler should be
		 * lazily initialized
		 */
		private DelayedInitializationHttpHandler(Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
			this.handlerSupplier = handlerSupplier;
			this.lazyInit = lazyInit;
		}

		/**
		 * Handles the case when the HttpHandler has not yet been initialized.
		 * @param request the server HTTP request
		 * @param response the server HTTP response
		 * @return a Mono representing the completion of the handling process
		 * @throws IllegalStateException if the HttpHandler has not yet been initialized
		 */
		private Mono<Void> handleUninitialized(ServerHttpRequest request, ServerHttpResponse response) {
			throw new IllegalStateException("The HttpHandler has not yet been initialized");
		}

		/**
		 * Handles the server HTTP request and response.
		 * @param request the server HTTP request
		 * @param response the server HTTP response
		 * @return a Mono representing the completion of the handling process
		 */
		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return this.delegate.handle(request, response);
		}

		/**
		 * Initializes the handler by assigning a delegate based on the lazy
		 * initialization flag. If lazy initialization is enabled, a LazyHttpHandler is
		 * created using the provided handler supplier. Otherwise, the handler supplier is
		 * used to directly obtain the handler.
		 */
		void initializeHandler() {
			this.delegate = this.lazyInit ? new LazyHttpHandler(this.handlerSupplier) : this.handlerSupplier.get();
		}

		/**
		 * Returns the HttpHandler instance that is delegated by this
		 * DelayedInitializationHttpHandler.
		 * @return the HttpHandler instance that is delegated by this
		 * DelayedInitializationHttpHandler.
		 */
		HttpHandler getHandler() {
			return this.delegate;
		}

	}

	/**
	 * {@link HttpHandler} that initializes its delegate on first request.
	 */
	private static final class LazyHttpHandler implements HttpHandler {

		private final Mono<HttpHandler> delegate;

		/**
		 * Constructs a new LazyHttpHandler with the given handler supplier.
		 * @param handlerSupplier the supplier of the HttpHandler to be used
		 */
		private LazyHttpHandler(Supplier<HttpHandler> handlerSupplier) {
			this.delegate = Mono.fromSupplier(handlerSupplier);
		}

		/**
		 * Handles the server HTTP request and response.
		 * @param request the server HTTP request
		 * @param response the server HTTP response
		 * @return a Mono representing the completion of the handling process
		 */
		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return this.delegate.flatMap((handler) -> handler.handle(request, response));
		}

	}

}
