/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentManager;

import org.springframework.boot.web.server.WebServer;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control an embedded Undertow server. Typically
 * this class should be created using {@link UndertowServletWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Kristine Jetzke
 * @since 2.0.0
 * @see UndertowServletWebServerFactory
 */
public class UndertowServletWebServer extends UndertowWebServer {

	private final String contextPath;

	private final DeploymentManager manager;

	/**
	 * Create a new {@link UndertowServletWebServer} instance.
	 * @param builder the builder
	 * @param httpHandlerFactories the handler factories
	 * @param contextPath the root context path
	 * @param autoStart if the server should be started
	 * @since 2.3.0
	 */
	public UndertowServletWebServer(Builder builder, Iterable<HttpHandlerFactory> httpHandlerFactories,
			String contextPath, boolean autoStart) {
		super(builder, httpHandlerFactories, autoStart);
		this.contextPath = contextPath;
		this.manager = findManager(httpHandlerFactories);
	}

	/**
	 * Finds the DeploymentManager from the given list of HttpHandlerFactories.
	 * @param httpHandlerFactories the list of HttpHandlerFactories to search through
	 * @return the DeploymentManager if found, null otherwise
	 */
	private DeploymentManager findManager(Iterable<HttpHandlerFactory> httpHandlerFactories) {
		for (HttpHandlerFactory httpHandlerFactory : httpHandlerFactories) {
			if (httpHandlerFactory instanceof DeploymentManagerHttpHandlerFactory deploymentManagerFactory) {
				return deploymentManagerFactory.getDeploymentManager();
			}
		}
		return null;
	}

	/**
	 * Creates the HTTP handler for the Undertow servlet web server.
	 * @return The created HTTP handler.
	 */
	@Override
	protected HttpHandler createHttpHandler() {
		HttpHandler handler = super.createHttpHandler();
		if (StringUtils.hasLength(this.contextPath)) {
			handler = Handlers.path().addPrefixPath(this.contextPath, handler);
		}
		return handler;
	}

	/**
	 * Returns the start log message for the UndertowServletWebServer.
	 * @return the start log message
	 */
	@Override
	protected String getStartLogMessage() {
		String contextPath = StringUtils.hasText(this.contextPath) ? this.contextPath : "/";
		StringBuilder message = new StringBuilder(super.getStartLogMessage());
		message.append(" with context path '");
		message.append(contextPath);
		message.append("'");
		return message.toString();
	}

	/**
	 * Returns the DeploymentManager associated with this UndertowServletWebServer.
	 * @return the DeploymentManager associated with this UndertowServletWebServer
	 */
	public DeploymentManager getDeploymentManager() {
		return this.manager;
	}

}
