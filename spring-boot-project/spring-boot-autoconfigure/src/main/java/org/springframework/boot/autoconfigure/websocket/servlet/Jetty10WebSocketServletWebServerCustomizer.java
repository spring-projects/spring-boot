/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket.servlet;

import java.lang.reflect.Method;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * WebSocket customizer for {@link JettyServletWebServerFactory} with Jetty 10.
 *
 * @author Andy Wilkinson
 */
class Jetty10WebSocketServletWebServerCustomizer
		implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, Ordered {

	static final String JETTY_WEB_SOCKET_SERVER_CONTAINER = "org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer";

	static final String JAVAX_WEB_SOCKET_SERVER_CONTAINER = "org.eclipse.jetty.websocket.javax.server.internal.JavaxWebSocketServerContainer";

	@Override
	public void customize(JettyServletWebServerFactory factory) {
		factory.addConfigurations(new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				ServletContext servletContext = context.getServletContext();
				Class<?> jettyContainer = ClassUtils.forName(JETTY_WEB_SOCKET_SERVER_CONTAINER, null);
				Method getJettyContainer = ReflectionUtils.findMethod(jettyContainer, "getContainer",
						ServletContext.class);
				Server server = context.getServer();
				if (ReflectionUtils.invokeMethod(getJettyContainer, null, servletContext) == null) {
					ensureWebSocketComponents(server, servletContext);
					ensureContainer(jettyContainer, servletContext);
				}
				Class<?> javaxContainer = ClassUtils.forName(JAVAX_WEB_SOCKET_SERVER_CONTAINER, null);
				Method getJavaxContainer = ReflectionUtils.findMethod(javaxContainer, "getContainer",
						ServletContext.class);
				if (ReflectionUtils.invokeMethod(getJavaxContainer, "getContainer", servletContext) == null) {
					ensureWebSocketComponents(server, servletContext);
					ensureUpgradeFilter(servletContext);
					ensureMappings(servletContext);
					ensureContainer(javaxContainer, servletContext);
				}
			}

			private void ensureWebSocketComponents(Server server, ServletContext servletContext)
					throws ClassNotFoundException {
				Class<?> webSocketServerComponents = ClassUtils
						.forName("org.eclipse.jetty.websocket.core.server.WebSocketServerComponents", null);
				Method ensureWebSocketComponents = ReflectionUtils.findMethod(webSocketServerComponents,
						"ensureWebSocketComponents", Server.class, ServletContext.class);
				ReflectionUtils.invokeMethod(ensureWebSocketComponents, null, server, servletContext);
			}

			private void ensureContainer(Class<?> container, ServletContext servletContext)
					throws ClassNotFoundException {
				Method ensureContainer = ReflectionUtils.findMethod(container, "ensureContainer", ServletContext.class);
				ReflectionUtils.invokeMethod(ensureContainer, null, servletContext);
			}

			private void ensureUpgradeFilter(ServletContext servletContext) throws ClassNotFoundException {
				Class<?> webSocketUpgradeFilter = ClassUtils
						.forName("org.eclipse.jetty.websocket.servlet.WebSocketUpgradeFilter", null);
				Method ensureFilter = ReflectionUtils.findMethod(webSocketUpgradeFilter, "ensureFilter",
						ServletContext.class);
				ReflectionUtils.invokeMethod(ensureFilter, null, servletContext);
			}

			private void ensureMappings(ServletContext servletContext) throws ClassNotFoundException {
				Class<?> webSocketMappings = ClassUtils
						.forName("org.eclipse.jetty.websocket.core.server.WebSocketMappings", null);
				Method ensureMappings = ReflectionUtils.findMethod(webSocketMappings, "ensureMappings",
						ServletContext.class);
				ReflectionUtils.invokeMethod(ensureMappings, null, servletContext);
			}

		});
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
