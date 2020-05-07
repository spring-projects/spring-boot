/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link GenericReactiveWebApplicationContext} that can be used to bootstrap itself
 * from a contained {@link ReactiveWebServerFactory} bean.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class ReactiveWebServerApplicationContext extends GenericReactiveWebApplicationContext
		implements ConfigurableWebServerApplicationContext {

	private volatile ServerManager serverManager;

	private String serverNamespace;

	/**
	 * Create a new {@link ReactiveWebServerApplicationContext}.
	 */
	public ReactiveWebServerApplicationContext() {
	}

	/**
	 * Create a new {@link ReactiveWebServerApplicationContext} with the given
	 * {@code DefaultListableBeanFactory}.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 */
	public ReactiveWebServerApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	@Override
	public final void refresh() throws BeansException, IllegalStateException {
		try {
			super.refresh();
		}
		catch (RuntimeException ex) {
			ServerManager serverManager = this.serverManager;
			if (serverManager != null) {
				serverManager.server.stop();
			}
			throw ex;
		}
	}

	@Override
	protected void onRefresh() {
		super.onRefresh();
		try {
			createWebServer();
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Unable to start reactive web server", ex);
		}
	}

	private void createWebServer() {
		ServerManager serverManager = this.serverManager;
		if (serverManager == null) {
			String webServerFactoryBeanName = getWebServerFactoryBeanName();
			ReactiveWebServerFactory webServerFactory = getWebServerFactory(webServerFactoryBeanName);
			boolean lazyInit = getBeanFactory().getBeanDefinition(webServerFactoryBeanName).isLazyInit();
			this.serverManager = new ServerManager(webServerFactory, this::getHttpHandler, lazyInit);
			getBeanFactory().registerSingleton("webServerGracefulShutdown",
					new WebServerGracefulShutdownLifecycle(this.serverManager));
			getBeanFactory().registerSingleton("webServerStartStop",
					new WebServerStartStopLifecycle(this.serverManager));
		}
		initPropertySources();
	}

	protected String getWebServerFactoryBeanName() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory().getBeanNamesForType(ReactiveWebServerFactory.class);
		if (beanNames.length == 0) {
			throw new ApplicationContextException(
					"Unable to start ReactiveWebApplicationContext due to missing ReactiveWebServerFactory bean.");
		}
		if (beanNames.length > 1) {
			throw new ApplicationContextException("Unable to start ReactiveWebApplicationContext due to multiple "
					+ "ReactiveWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));
		}
		return beanNames[0];
	}

	protected ReactiveWebServerFactory getWebServerFactory(String factoryBeanName) {
		return getBeanFactory().getBean(factoryBeanName, ReactiveWebServerFactory.class);
	}

	/**
	 * Return the {@link HttpHandler} that should be used to process the reactive web
	 * server. By default this method searches for a suitable bean in the context itself.
	 * @return a {@link HttpHandler} (never {@code null}
	 */
	protected HttpHandler getHttpHandler() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory().getBeanNamesForType(HttpHandler.class);
		if (beanNames.length == 0) {
			throw new ApplicationContextException(
					"Unable to start ReactiveWebApplicationContext due to missing HttpHandler bean.");
		}
		if (beanNames.length > 1) {
			throw new ApplicationContextException(
					"Unable to start ReactiveWebApplicationContext due to multiple HttpHandler beans : "
							+ StringUtils.arrayToCommaDelimitedString(beanNames));
		}
		return getBeanFactory().getBean(beanNames[0], HttpHandler.class);
	}

	@Override
	protected void doClose() {
		AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
		super.doClose();
	}

	/**
	 * Returns the {@link WebServer} that was created by the context or {@code null} if
	 * the server has not yet been created.
	 * @return the web server
	 */
	@Override
	public WebServer getWebServer() {
		ServerManager serverManager = this.serverManager;
		return (serverManager != null) ? serverManager.server : null;
	}

	@Override
	public String getServerNamespace() {
		return this.serverNamespace;
	}

	@Override
	public void setServerNamespace(String serverNamespace) {
		this.serverNamespace = serverNamespace;
	}

	/**
	 * Internal class used to manage the server and the {@link HttpHandler}, taking care
	 * not to initialize the handler too early.
	 */
	final class ServerManager {

		private final WebServer server;

		private DelayedInitializationHttpHandler handler;

		private ServerManager(ReactiveWebServerFactory factory, Supplier<HttpHandler> handlerSupplier,
				boolean lazyInit) {
			Assert.notNull(factory, "ReactiveWebServerFactory must not be null");
			this.handler = new DelayedInitializationHttpHandler(handlerSupplier, lazyInit);
			this.server = factory.getWebServer(this.handler);
		}

		private void start() {
			this.handler.initializeHandler();
			this.server.start();
			ReactiveWebServerApplicationContext.this.publishEvent(
					new ReactiveWebServerInitializedEvent(this.server, ReactiveWebServerApplicationContext.this));
		}

		void shutDownGracefully(Runnable callback) {
			this.server.shutDownGracefully((result) -> callback.run());
		}

		void stop() {
			this.server.stop();
		}

		HttpHandler getHandler() {
			return this.handler;
		}

	}

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

		private void initializeHandler() {
			this.delegate = this.lazyInit ? new LazyHttpHandler(Mono.fromSupplier(this.handlerSupplier))
					: this.handlerSupplier.get();
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

		private LazyHttpHandler(Mono<HttpHandler> delegate) {
			this.delegate = delegate;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return this.delegate.flatMap((handler) -> handler.handle(request, response));
		}

	}

	private static final class WebServerStartStopLifecycle implements SmartLifecycle {

		private final ServerManager serverManager;

		private volatile boolean running;

		private WebServerStartStopLifecycle(ServerManager serverManager) {
			this.serverManager = serverManager;
		}

		@Override
		public void start() {
			this.serverManager.start();
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
			this.serverManager.stop();
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public int getPhase() {
			return Integer.MAX_VALUE - 1;
		}

	}

	private static final class WebServerGracefulShutdownLifecycle implements SmartLifecycle {

		private final ServerManager serverManager;

		private volatile boolean running;

		private WebServerGracefulShutdownLifecycle(ServerManager serverManager) {
			this.serverManager = serverManager;
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			throw new UnsupportedOperationException("Stop must not be invoked directly");
		}

		@Override
		public void stop(Runnable callback) {
			this.running = false;
			this.serverManager.shutDownGracefully(callback);
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

	}

}
