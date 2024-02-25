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

package org.springframework.boot.actuate.metrics.web.jetty;

import org.eclipse.jetty.server.Server;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Base class for binding Jetty metrics in response to an {@link ApplicationStartedEvent}.
 *
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public abstract class AbstractJettyMetricsBinder implements ApplicationListener<ApplicationStartedEvent> {

	/**
	 * This method is called when the application has started. It finds the server
	 * instance from the application context and binds the metrics to it.
	 * @param event The ApplicationStartedEvent that triggered this method.
	 */
	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		Server server = findServer(event.getApplicationContext());
		if (server != null) {
			bindMetrics(server);
		}
	}

	/**
	 * Finds the Jetty server instance from the given ApplicationContext.
	 * @param applicationContext the ApplicationContext to search for the server
	 * @return the Jetty server instance if found, otherwise null
	 */
	private Server findServer(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
			WebServer webServer = webServerApplicationContext.getWebServer();
			if (webServer instanceof JettyWebServer jettyWebServer) {
				return jettyWebServer.getServer();
			}
		}
		return null;
	}

	/**
	 * Binds the metrics to the specified server.
	 * @param server the server to bind the metrics to
	 */
	protected abstract void bindMetrics(Server server);

}
