/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.bootstrap.context.embedded.jetty;

import org.eclipse.jetty.server.Server;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainer;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerException;
import org.springframework.util.Assert;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Jetty server.
 * Usually this class should be created using the
 * {@link JettyEmbeddedServletContainerFactory} and not directly.
 * 
 * @author Phillip Webb
 * @since 4.0
 * @see JettyEmbeddedServletContainerFactory
 */
public class JettyEmbeddedServletContainer implements EmbeddedServletContainer {

	private final Server server;

	/**
	 * Create a new {@link JettyEmbeddedServletContainer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyEmbeddedServletContainer(Server server) {
		Assert.notNull(server, "Jetty Server must not be null");
		this.server = server;
		start();
	}

	private synchronized void start() {
		try {
			this.server.start();
		} catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Jetty servlet container", ex);
		}
	}

	@Override
	public synchronized void stop() {
		try {
			this.server.setGracefulShutdown(10000);
			this.server.stop();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			// No drama
		} catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to stop embedded Jetty servlet container", ex);
		}
	}

	/**
	 * Returns access to the underlying Jetty Server.
	 */
	public Server getServer() {
		return this.server;
	}
}
