/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.naming.ContextBindings;

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

	private static final Log logger = LogFactory
			.getLog(TomcatEmbeddedServletContainer.class);

	private static final AtomicInteger containerCounter = new AtomicInteger(-1);

	private final Object monitor = new Object();

	private final Map<Service, Connector[]> serviceConnectors = new HashMap<Service, Connector[]>();

	private final Tomcat tomcat;

	private final boolean autoStart;

	private volatile boolean started;

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
	 * @param autoStart if the server should be started
	 */
	public TomcatEmbeddedServletContainer(Tomcat tomcat, boolean autoStart) {
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
		this.autoStart = autoStart;
		initialize();
	}

	private void initialize() throws EmbeddedServletContainerException {
		TomcatEmbeddedServletContainer.logger
				.info("Tomcat initialized with port(s): " + getPortsDescription(false));
		synchronized (this.monitor) {
			try {
				addInstanceIdToEngineName();
				try {
					final Context context = findContext();
					context.addLifecycleListener(new LifecycleListener() {

						@Override
						public void lifecycleEvent(LifecycleEvent event) {
							if (context.equals(event.getSource())
									&& Lifecycle.START_EVENT.equals(event.getType())) {
								// Remove service connectors so that protocol
								// binding doesn't happen when the service is
								// started.
								removeServiceConnectors();
							}
						}

					});

					// Start the server to trigger initialization listeners
					this.tomcat.start();

					// We can re-throw failure exception directly in the main thread
					rethrowDeferredStartupExceptions();

					try {
						ContextBindings.bindClassLoader(context, getNamingToken(context),
								getClass().getClassLoader());
					}
					catch (NamingException ex) {
						// Naming is not enabled. Continue
					}

					// Unlike Jetty, all Tomcat threads are daemon threads. We create a
					// blocking non-daemon to stop immediate shutdown
					startDaemonAwaitThread();
				}
				catch (Exception ex) {
					containerCounter.decrementAndGet();
					throw ex;
				}
			}
			catch (Exception ex) {
				stopSilently();
				throw new EmbeddedServletContainerException(
						"Unable to start embedded Tomcat", ex);
			}
		}
	}

	private Context findContext() {
		for (Container child : this.tomcat.getHost().findChildren()) {
			if (child instanceof Context) {
				return (Context) child;
			}
		}
		throw new IllegalStateException("The host does not contain a Context");
	}

	private void addInstanceIdToEngineName() {
		int instanceId = containerCounter.incrementAndGet();
		if (instanceId > 0) {
			Engine engine = this.tomcat.getEngine();
			engine.setName(engine.getName() + "-" + instanceId);
		}
	}

	private void removeServiceConnectors() {
		for (Service service : this.tomcat.getServer().findServices()) {
			Connector[] connectors = service.findConnectors().clone();
			this.serviceConnectors.put(service, connectors);
			for (Connector connector : connectors) {
				service.removeConnector(connector);
			}
		}
	}

	private void rethrowDeferredStartupExceptions() throws Exception {
		Container[] children = this.tomcat.getHost().findChildren();
		for (Container container : children) {
			if (container instanceof TomcatEmbeddedContext) {
				Exception exception = ((TomcatEmbeddedContext) container).getStarter()
						.getStartUpException();
				if (exception != null) {
					throw exception;
				}
			}
			if (!LifecycleState.STARTED.equals(container.getState())) {
				throw new IllegalStateException(container + " failed to start");
			}
		}
	}

	private void startDaemonAwaitThread() {
		Thread awaitThread = new Thread("container-" + (containerCounter.get())) {

			@Override
			public void run() {
				TomcatEmbeddedServletContainer.this.tomcat.getServer().await();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void start() throws EmbeddedServletContainerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			try {
				addPreviouslyRemovedConnectors();
				Connector connector = this.tomcat.getConnector();
				if (connector != null && this.autoStart) {
					performDeferredLoadOnStartup();
				}
				checkThatConnectorsHaveStarted();
				this.started = true;
				TomcatEmbeddedServletContainer.logger
						.info("Tomcat started on port(s): " + getPortsDescription(true));
			}
			catch (ConnectorStartFailedException ex) {
				stopSilently();
				throw ex;
			}
			catch (Exception ex) {
				throw new EmbeddedServletContainerException(
						"Unable to start embedded Tomcat servlet container", ex);
			}
			finally {
				Context context = findContext();
				ContextBindings.unbindClassLoader(context, getNamingToken(context),
						getClass().getClassLoader());
			}
		}
	}

	private void checkThatConnectorsHaveStarted() {
		for (Connector connector : this.tomcat.getService().findConnectors()) {
			if (LifecycleState.FAILED.equals(connector.getState())) {
				throw new ConnectorStartFailedException(connector.getPort());
			}
		}
	}

	private void stopSilently() {
		try {
			stopTomcat();
		}
		catch (LifecycleException ex) {
			// Ignore
		}
	}

	private void stopTomcat() throws LifecycleException {
		if (Thread.currentThread()
				.getContextClassLoader() instanceof TomcatEmbeddedWebappClassLoader) {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		}
		this.tomcat.stop();
	}

	private void addPreviouslyRemovedConnectors() {
		Service[] services = this.tomcat.getServer().findServices();
		for (Service service : services) {
			Connector[] connectors = this.serviceConnectors.get(service);
			if (connectors != null) {
				for (Connector connector : connectors) {
					service.addConnector(connector);
					if (!this.autoStart) {
						stopProtocolHandler(connector);
					}
				}
				this.serviceConnectors.remove(service);
			}
		}
	}

	private void stopProtocolHandler(Connector connector) {
		try {
			connector.getProtocolHandler().stop();
		}
		catch (Exception ex) {
			TomcatEmbeddedServletContainer.logger.error("Cannot pause connector: ", ex);
		}
	}

	private void performDeferredLoadOnStartup() {
		try {
			for (Container child : this.tomcat.getHost().findChildren()) {
				if (child instanceof TomcatEmbeddedContext) {
					((TomcatEmbeddedContext) child).deferredLoadOnStartup();
				}
			}
		}
		catch (Exception ex) {
			TomcatEmbeddedServletContainer.logger.error("Cannot start connector: ", ex);
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Tomcat connectors", ex);
		}
	}

	Map<Service, Connector[]> getServiceConnectors() {
		return this.serviceConnectors;
	}

	@Override
	public void stop() throws EmbeddedServletContainerException {
		synchronized (this.monitor) {
			boolean wasStarted = this.started;
			try {
				this.started = false;
				try {
					stopTomcat();
					this.tomcat.destroy();
				}
				catch (LifecycleException ex) {
					// swallow and continue
				}
			}
			catch (Exception ex) {
				throw new EmbeddedServletContainerException(
						"Unable to stop embedded Tomcat", ex);
			}
			finally {
				if (wasStarted) {
					containerCounter.decrementAndGet();
				}
			}
		}
	}

	private String getPortsDescription(boolean localPort) {
		StringBuilder ports = new StringBuilder();
		for (Connector connector : this.tomcat.getService().findConnectors()) {
			ports.append((ports.length() != 0) ? " " : "");
			int port = (localPort ? connector.getLocalPort() : connector.getPort());
			ports.append(port + " (" + connector.getScheme() + ")");
		}
		return ports.toString();
	}

	@Override
	public int getPort() {
		Connector connector = this.tomcat.getConnector();
		if (connector != null) {
			return connector.getLocalPort();
		}
		return 0;
	}

	/**
	 * Returns access to the underlying Tomcat server.
	 * @return the Tomcat server
	 */
	public Tomcat getTomcat() {
		return this.tomcat;
	}

	private Object getNamingToken(Context context) {
		try {
			return context.getNamingToken();
		}
		catch (NoSuchMethodError ex) {
			// Use the context itself on Tomcat 7
			return context;
		}

	}

}
