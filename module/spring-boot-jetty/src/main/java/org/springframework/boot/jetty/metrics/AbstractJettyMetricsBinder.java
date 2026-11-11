/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.metrics;

import org.eclipse.jetty.server.Server;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Base class for binding Jetty metrics in response to an {@link ApplicationStartedEvent}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public abstract class AbstractJettyMetricsBinder implements ApplicationListener<ApplicationStartedEvent> {

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		Server server = findServer(event.getApplicationContext());
		if (server != null) {
			bindMetrics(server);
		}
	}

	private @Nullable Server findServer(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
			WebServer webServer = webServerApplicationContext.getWebServer();
			if (webServer instanceof JettyWebServer jettyWebServer) {
				return jettyWebServer.getServer();
			}
		}
		return null;
	}

	protected abstract void bindMetrics(Server server);

}
