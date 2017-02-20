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

package org.springframework.boot.context.embedded;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;

/**
 * A {@link AnnotationConfigApplicationContext} that can be used to bootstrap
 * itself from a contained embedded web server factory bean.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class ReactiveWebApplicationContext extends AnnotationConfigApplicationContext {

	private volatile EmbeddedWebServer embeddedWebServer;

	public ReactiveWebApplicationContext() {
		super();
	}

	public ReactiveWebApplicationContext(Class... annotatedClasses) {
		super(annotatedClasses);
	}

	@Override
	public final void refresh() throws BeansException, IllegalStateException {
		try {
			super.refresh();
		}
		catch (RuntimeException ex) {
			stopAndReleaseReactiveWebServer();
			throw ex;
		}
	}

	@Override
	protected void onRefresh() {
		super.onRefresh();
		try {
			createEmbeddedServletContainer();
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Unable to start reactive web server", ex);
		}
	}

	@Override
	protected void finishRefresh() {
		super.finishRefresh();
		EmbeddedWebServer localServer = startReactiveWebServer();
		if (localServer != null) {
			publishEvent(
					new EmbeddedReactiveWebServerInitializedEvent(localServer, this));
		}
	}

	@Override
	protected void onClose() {
		super.onClose();
		stopAndReleaseReactiveWebServer();
	}

	private void createEmbeddedServletContainer() {
		EmbeddedWebServer localServer = this.embeddedWebServer;
		if (localServer == null) {
			this.embeddedWebServer = getReactiveWebServerFactory()
					.getReactiveHttpServer(getHttpHandler());
		}
		initPropertySources();
	}

	/**
	 * Return the {@link ReactiveWebServerFactory} that should be used to create
	 * the reactive web server. By default this method searches for a suitable bean
	 * in the context itself.
	 * @return a {@link ReactiveWebServerFactory} (never {@code null})
	 */
	protected ReactiveWebServerFactory getReactiveWebServerFactory() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory()
				.getBeanNamesForType(ReactiveWebServerFactory.class);
		if (beanNames.length == 0) {
			throw new ApplicationContextException(
					"Unable to start ReactiveWebApplicationContext due to missing "
							+ "ReactiveWebServerFactory bean.");
		}
		if (beanNames.length > 1) {
			throw new ApplicationContextException(
					"Unable to start ReactiveWebApplicationContext due to multiple "
							+ "ReactiveWebServerFactory beans : "
							+ StringUtils.arrayToCommaDelimitedString(beanNames));
		}
		return getBeanFactory().getBean(beanNames[0], ReactiveWebServerFactory.class);
	}

	/**
	 * Return the {@link HttpHandler} that should be used to process
	 * the reactive web server. By default this method searches for a suitable bean
	 * in the context itself.
	 * @return a {@link HttpHandler} (never {@code null}
	 */
	protected HttpHandler getHttpHandler() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory()
				.getBeanNamesForType(HttpHandler.class);
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

	private EmbeddedWebServer startReactiveWebServer() {
		EmbeddedWebServer localServer = this.embeddedWebServer;
		if (localServer != null) {
			localServer.start();
		}
		return localServer;
	}

	private void stopAndReleaseReactiveWebServer() {
		EmbeddedWebServer localServer = this.embeddedWebServer;
		if (localServer != null) {
			try {
				localServer.stop();
				this.embeddedWebServer = null;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}
}
