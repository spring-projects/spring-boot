/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.servlet;

import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.jetty.JettyWebServer;

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
