/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure.reactive;

import jakarta.servlet.ServletContext;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.websocket.jakarta.server.JakartaWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.websocket.core.server.WebSocketMappings;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jetty.reactive.JettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * WebSocket customizer for {@link JettyReactiveWebServerFactory}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class WebSocketJettyReactiveWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<JettyReactiveWebServerFactory>, Ordered {

	@Override
	public void customize(JettyReactiveWebServerFactory factory) {
		factory.addServerCustomizers((server) -> {
			ServletContextHandler servletContextHandler = findServletContextHandler(server);
			if (servletContextHandler != null) {
				ServletContext servletContext = servletContextHandler.getServletContext();
				if (JettyWebSocketServerContainer.getContainer(servletContext) == null) {
					WebSocketServerComponents.ensureWebSocketComponents(server, servletContextHandler);
					JettyWebSocketServerContainer.ensureContainer(servletContext);
				}
				if (JakartaWebSocketServerContainer.getContainer(servletContext) == null) {
					WebSocketServerComponents.ensureWebSocketComponents(server, servletContextHandler);
					WebSocketUpgradeFilter.ensureFilter(servletContext);
					WebSocketMappings.ensureMappings(servletContextHandler);
					JakartaWebSocketServerContainer.ensureContainer(servletContext);
				}
			}
		});
	}

	private @Nullable ServletContextHandler findServletContextHandler(Handler handler) {
		if (handler instanceof ServletContextHandler servletContextHandler) {
			return servletContextHandler;
		}
		if (handler instanceof Handler.Wrapper handlerWrapper) {
			return findServletContextHandler(handlerWrapper.getHandler());
		}
		if (handler instanceof Handler.Collection handlerCollection) {
			for (Handler contained : handlerCollection.getHandlers()) {
				ServletContextHandler servletContextHandler = findServletContextHandler(contained);
				if (servletContextHandler != null) {
					return servletContextHandler;
				}
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
