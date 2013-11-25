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

package org.springframework.boot.context.embedded.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.util.Assert;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Tomcat server.
 * Usually this class should be created using the
 * {@link TomcatEmbeddedServletContainerFactory} and not directly.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see TomcatEmbeddedServletContainerFactory
 */
public class TomcatEmbeddedServletContainer implements EmbeddedServletContainer {

	private final Log logger = LogFactory.getLog(TomcatEmbeddedServletContainer.class);

	private static int containerCounter = 0;

	private final Tomcat tomcat;

	private boolean autoStart;

	/**
	 * Create a new {@link TomcatEmbeddedServletContainer} instance.
	 * @param tomcat the underlying Tomcat server
	 */
	public TomcatEmbeddedServletContainer(Tomcat tomcat) {
		this(tomcat, true);
	}

	/**
	 * Create a new {@link TomcatEmbeddedServletContainer} instance.
	 * @param tomcat the underlying Tomcat server
	 */
	public TomcatEmbeddedServletContainer(Tomcat tomcat, boolean autoStart) {
		this.autoStart = autoStart;
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
		initialize();
	}

	private synchronized void initialize() throws EmbeddedServletContainerException {
		try {
			this.tomcat.start();
			try {
				// Allow the server to start so the ServletContext is available, but stop
				// the connector to prevent requests from being handled before the Spring
				// context is ready:
				Connector connector = this.tomcat.getConnector();
				connector.getProtocolHandler().stop();
			}
			catch (Exception ex) {
				this.logger.error("Cannot pause connector: ", ex);
			}
			// Unlike Jetty, all Tomcat threads are daemon threads. We create a
			// blocking non-daemon to stop immediate shutdown
			Thread awaitThread = new Thread("container-" + (containerCounter++)) {
				@Override
				public void run() {
					TomcatEmbeddedServletContainer.this.tomcat.getServer().await();
				};
			};
			awaitThread.setDaemon(false);
			awaitThread.start();
			if (LifecycleState.FAILED.equals(this.tomcat.getConnector().getState())) {
				this.tomcat.stop();
				throw new IllegalStateException("Tomcat connector in failed state");
			}
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Tomcat", ex);
		}
	}

	@Override
	public void start() throws EmbeddedServletContainerException {
		Connector connector = this.tomcat.getConnector();
		if (connector != null && this.autoStart) {
			try {
				connector.getProtocolHandler().start();
				this.logger.info("Tomcat started on port: " + connector.getLocalPort());
			}
			catch (Exception ex) {
				this.logger.error("Cannot start connector: ", ex);
				throw new EmbeddedServletContainerException(
						"Unable to start embedded Tomcat connectors", ex);
			}
		}
	}

	@Override
	public synchronized void stop() throws EmbeddedServletContainerException {
		try {
			try {
				this.tomcat.stop();
			}
			catch (LifecycleException ex) {
				// swallow and continue
			}
			this.tomcat.destroy();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to stop embedded Tomcat", ex);
		}
	}

	/**
	 * Returns access to the underlying Tomcat server.
	 */
	public Tomcat getTomcat() {
		return this.tomcat;
	}

}
