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

package org.springframework.boot.autoconfigure.websocket.reactive;

import jakarta.servlet.ServletContext;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.JakartaWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.websocket.core.server.WebSocketMappings;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;

import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * WebSocket customizer for {@link JettyReactiveWebServerFactory}.
 *
 * @author Andy Wilkinson
 * @since 3.0.8
 */
public class JettyWebSocketReactiveWebServerCustomizer
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

	private ServletContextHandler findServletContextHandler(Handler handler) {
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
