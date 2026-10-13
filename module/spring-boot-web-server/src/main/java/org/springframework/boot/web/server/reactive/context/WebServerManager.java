/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
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

	WebServerManager(ReactiveWebServerApplicationContext applicationContext, ReactiveWebServerFactory factory,
			Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
		this.applicationContext = applicationContext;
		Assert.notNull(factory, "'factory' must not be null");
		this.handler = new DelayedInitializationHttpHandler(handlerSupplier, lazyInit);
		this.webServer = factory.getWebServer(this.handler);
	}

	void start() {
		this.handler.initializeHandler();
		this.webServer.start();
		this.applicationContext
			.publishEvent(new ReactiveWebServerInitializedEvent(this.webServer, this.applicationContext));
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		this.webServer.shutDownGracefully(callback);
	}

	void stop() {
		this.webServer.stop();
	}

	WebServer getWebServer() {
		return this.webServer;
	}

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

		private DelayedInitializationHttpHandler(Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
			this.handlerSupplier = handlerSupplier;
			this.lazyInit = lazyInit;
		}

		private Mono<Void> handleUninitialized(ServerHttpRequest request, ServerHttpResponse response) {
			throw new IllegalStateException("The HttpHandler has not yet been initialized");
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return this.delegate.handle(request, response);
		}

		void initializeHandler() {
			this.delegate = this.lazyInit ? new LazyHttpHandler(this.handlerSupplier) : this.handlerSupplier.get();
		}

		HttpHandler getHandler() {
			return this.delegate;
		}

	}

	/**
	 * {@link HttpHandler} that initializes its delegate on first request.
	 */
	private static final class LazyHttpHandler implements HttpHandler {

		private final Mono<HttpHandler> delegate;

		private LazyHttpHandler(Supplier<HttpHandler> handlerSupplier) {
			this.delegate = Mono.fromSupplier(handlerSupplier);
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return this.delegate.flatMap((handler) -> handler.handle(request, response));
		}

	}

}
