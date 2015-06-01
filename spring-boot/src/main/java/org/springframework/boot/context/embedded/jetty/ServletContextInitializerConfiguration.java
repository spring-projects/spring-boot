/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.util.Assert;

/**
 * Jetty {@link Configuration} that calls {@link ServletContextInitializer}s.
 *
 * @author Phillip Webb
 */
public class ServletContextInitializerConfiguration extends AbstractConfiguration {

	private final ContextHandler contextHandler;

	private final ServletContextInitializer[] initializers;

	/**
	 * Create a new {@link ServletContextInitializerConfiguration}.
	 * @param contextHandler the Jetty ContextHandler
	 * @param initializers the initializers that should be invoked
	 */
	public ServletContextInitializerConfiguration(ContextHandler contextHandler,
			ServletContextInitializer... initializers) {
		Assert.notNull(contextHandler, "Jetty ContextHandler must not be null");
		Assert.notNull(initializers, "Initializers must not be null");
		this.contextHandler = contextHandler;
		this.initializers = initializers;

	}

	@Override
	public void configure(WebAppContext context) throws Exception {
		context.addBean(new InitializerListener(), true);
	}

	private class InitializerListener extends AbstractLifeCycle {

		@Override
		protected void doStart() throws Exception {
			ServletContext servletContext = ServletContextInitializerConfiguration.this.contextHandler
					.getServletContext();
			for (ServletContextInitializer initializer : ServletContextInitializerConfiguration.this.initializers) {
				initializer.onStartup(servletContext);
			}
		}
	}

}
