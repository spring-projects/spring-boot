/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.server.servlet.jetty;

import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.jetty.JettyWebServer;

/**
 * Servlet-specific {@link JettyWebServer}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class JettyServletWebServer extends JettyWebServer {

	public JettyServletWebServer(Server server) {
		super(server);
	}

	public JettyServletWebServer(Server server, boolean autoStart) {
		super(server, autoStart);
	}

	@Override
	protected void handleDeferredInitialize(Server server) throws Exception {
		handleDeferredInitialize(server.getHandlers());
	}

	protected void handleDeferredInitialize(List<Handler> handlers) throws Exception {
		for (Handler handler : handlers) {
			handleDeferredInitialize(handler);
		}
	}

	private void handleDeferredInitialize(Handler handler) throws Exception {
		if (handler instanceof JettyEmbeddedWebAppContext jettyEmbeddedWebAppContext) {
			jettyEmbeddedWebAppContext.deferredInitialize();
		}
		else if (handler instanceof Handler.Wrapper handlerWrapper) {
			handleDeferredInitialize(handlerWrapper.getHandler());
		}
		else if (handler instanceof Handler.Collection handlerCollection) {
			handleDeferredInitialize(handlerCollection.getHandlers());
		}
	}

}
