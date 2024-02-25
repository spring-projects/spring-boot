/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.MissingWebServerFactoryBeanException;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.metrics.StartupStep;
import org.springframework.http.server.reactive.HttpHandler;
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

	private volatile WebServerManager serverManager;

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

	/**
     * Refreshes the application context.
     * 
     * @throws BeansException if an error occurs during bean initialization or configuration
     * @throws IllegalStateException if the application context has already been refreshed
     */
    @Override
	public final void refresh() throws BeansException, IllegalStateException {
		try {
			super.refresh();
		}
		catch (RuntimeException ex) {
			WebServerManager serverManager = this.serverManager;
			if (serverManager != null) {
				serverManager.getWebServer().stop();
			}
			throw ex;
		}
	}

	/**
     * This method is called when the application context is refreshed.
     * It overrides the onRefresh() method from the parent class.
     * It creates a web server for handling reactive requests.
     * If an exception occurs while creating the web server, an ApplicationContextException is thrown.
     * 
     * @throws ApplicationContextException if unable to start the reactive web server
     */
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

	/**
     * Creates a web server for the application.
     * 
     * This method initializes the web server manager and registers the necessary
     * lifecycle beans for graceful shutdown and start/stop of the web server.
     * 
     * @throws IllegalStateException if the web server manager is already created
     * @see WebServerManager
     * @see WebServerGracefulShutdownLifecycle
     * @see WebServerStartStopLifecycle
     */
    private void createWebServer() {
		WebServerManager serverManager = this.serverManager;
		if (serverManager == null) {
			StartupStep createWebServer = getApplicationStartup().start("spring.boot.webserver.create");
			String webServerFactoryBeanName = getWebServerFactoryBeanName();
			ReactiveWebServerFactory webServerFactory = getWebServerFactory(webServerFactoryBeanName);
			createWebServer.tag("factory", webServerFactory.getClass().toString());
			boolean lazyInit = getBeanFactory().getBeanDefinition(webServerFactoryBeanName).isLazyInit();
			this.serverManager = new WebServerManager(this, webServerFactory, this::getHttpHandler, lazyInit);
			getBeanFactory().registerSingleton("webServerGracefulShutdown",
					new WebServerGracefulShutdownLifecycle(this.serverManager.getWebServer()));
			getBeanFactory().registerSingleton("webServerStartStop",
					new WebServerStartStopLifecycle(this.serverManager));
			createWebServer.end();
		}
		initPropertySources();
	}

	/**
     * Returns the name of the bean that implements the ReactiveWebServerFactory interface.
     * 
     * @return the name of the ReactiveWebServerFactory bean
     * @throws MissingWebServerFactoryBeanException if no bean implementing ReactiveWebServerFactory is found
     * @throws ApplicationContextException if multiple beans implementing ReactiveWebServerFactory are found
     */
    protected String getWebServerFactoryBeanName() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory().getBeanNamesForType(ReactiveWebServerFactory.class);
		if (beanNames.length == 0) {
			throw new MissingWebServerFactoryBeanException(getClass(), ReactiveWebServerFactory.class,
					WebApplicationType.REACTIVE);
		}
		if (beanNames.length > 1) {
			throw new ApplicationContextException("Unable to start ReactiveWebApplicationContext due to multiple "
					+ "ReactiveWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));
		}
		return beanNames[0];
	}

	/**
     * Retrieves the ReactiveWebServerFactory bean with the specified factory bean name from the bean factory.
     * 
     * @param factoryBeanName the name of the factory bean
     * @return the ReactiveWebServerFactory bean
     */
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

	/**
     * Closes the ReactiveWebServerApplicationContext.
     * 
     * This method first checks if the application context is active. If it is active, it publishes an AvailabilityChangeEvent with the ReadinessState set to REFUSING_TRAFFIC. 
     * Then, it calls the superclass's doClose() method to perform the actual closing of the application context.
     */
    @Override
	protected void doClose() {
		if (isActive()) {
			AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
		}
		super.doClose();
	}

	/**
	 * Returns the {@link WebServer} that was created by the context or {@code null} if
	 * the server has not yet been created.
	 * @return the web server
	 */
	@Override
	public WebServer getWebServer() {
		WebServerManager serverManager = this.serverManager;
		return (serverManager != null) ? serverManager.getWebServer() : null;
	}

	/**
     * Returns the server namespace.
     *
     * @return the server namespace
     */
    @Override
	public String getServerNamespace() {
		return this.serverNamespace;
	}

	/**
     * Sets the server namespace for this ReactiveWebServerApplicationContext.
     * 
     * @param serverNamespace the server namespace to set
     */
    @Override
	public void setServerNamespace(String serverNamespace) {
		this.serverNamespace = serverNamespace;
	}

}
