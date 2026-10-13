/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.servlet;

import org.eclipse.jetty.ee11.servlet.ServletHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.util.ClassMatcher;

import org.springframework.boot.jetty.JettyWebServer;

/**
 * Jetty {@link WebAppContext} used by {@link JettyWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 */
class JettyEmbeddedWebAppContext extends WebAppContext {

	JettyEmbeddedWebAppContext() {
		setHiddenClassMatcher(new ClassMatcher("org.springframework.boot.loader."));
	}

	@Override
	protected ServletHandler newServletHandler() {
		return new JettyEmbeddedServletHandler();
	}

	void deferredInitialize() throws Exception {
		JettyEmbeddedServletHandler handler = (JettyEmbeddedServletHandler) getServletHandler();
		getContext().call(handler::deferredInitialize, null);
	}

	@Override
	public String getCanonicalNameForTmpDir() {
		return super.getCanonicalNameForTmpDir();
	}

	private static final class JettyEmbeddedServletHandler extends ServletHandler {

		@Override
		public void initialize() throws Exception {
		}

		void deferredInitialize() throws Exception {
			super.initialize();
		}

	}

}
