/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure.servlet;

import org.eclipse.jetty.ee11.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.ee11.websocket.jakarta.server.JakartaWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.core.server.WebSocketMappings;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;

import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * WebSocket customizer for {@link JettyServletWebServerFactory}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class WebSocketJettyServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, Ordered {

	@Override
	public void customize(JettyServletWebServerFactory factory) {
		factory.addConfigurations(new AbstractConfiguration(new AbstractConfiguration.Builder()) {

			@Override
			public void configure(WebAppContext context) throws Exception {
				if (JettyWebSocketServerContainer.getContainer(context.getServletContext()) == null) {
					WebSocketServerComponents.ensureWebSocketComponents(context.getServer(),
							context.getContext().getContextHandler());
					JettyWebSocketServerContainer.ensureContainer(context.getServletContext());
				}
				if (JakartaWebSocketServerContainer.getContainer(context.getServletContext()) == null) {
					WebSocketServerComponents.ensureWebSocketComponents(context.getServer(),
							context.getContext().getContextHandler());
					WebSocketUpgradeFilter.ensureFilter(context.getServletContext());
					WebSocketMappings.ensureMappings(context.getContext().getContextHandler());
					JakartaWebSocketServerContainer.ensureContainer(context.getServletContext());
				}
			}

		});
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
