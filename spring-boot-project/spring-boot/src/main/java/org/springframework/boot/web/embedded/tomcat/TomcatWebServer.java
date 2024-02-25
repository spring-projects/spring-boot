/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.naming.NamingException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.naming.ContextBindings;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control a Tomcat web server. Usually this class
 * should be created using the {@link TomcatReactiveWebServerFactory} or
 * {@link TomcatServletWebServerFactory}, but not directly.
 *
 * @author Brian Clozel
 * @author Kristine Jetzke
 * @since 2.0.0
 */
public class TomcatWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(TomcatWebServer.class);

	private static final AtomicInteger containerCounter = new AtomicInteger(-1);

	private final Object monitor = new Object();

	private final Map<Service, Connector[]> serviceConnectors = new HashMap<>();

	private final Tomcat tomcat;

	private final boolean autoStart;

	private final GracefulShutdown gracefulShutdown;

	private volatile boolean started;

	/**
	 * Create a new {@link TomcatWebServer} instance.
	 * @param tomcat the underlying Tomcat server
	 */
	public TomcatWebServer(Tomcat tomcat) {
		this(tomcat, true);
	}

	/**
	 * Create a new {@link TomcatWebServer} instance.
	 * @param tomcat the underlying Tomcat server
	 * @param autoStart if the server should be started
	 */
	public TomcatWebServer(Tomcat tomcat, boolean autoStart) {
		this(tomcat, autoStart, Shutdown.IMMEDIATE);
	}

	/**
	 * Create a new {@link TomcatWebServer} instance.
	 * @param tomcat the underlying Tomcat server
	 * @param autoStart if the server should be started
	 * @param shutdown type of shutdown supported by the server
	 * @since 2.3.0
	 */
	public TomcatWebServer(Tomcat tomcat, boolean autoStart, Shutdown shutdown) {
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
		this.autoStart = autoStart;
		this.gracefulShutdown = (shutdown == Shutdown.GRACEFUL) ? new GracefulShutdown(tomcat) : null;
		initialize();
	}

	/**
     * Initializes the Tomcat web server.
     * 
     * @throws WebServerException if unable to start the embedded Tomcat
     */
    private void initialize() throws WebServerException {
		logger.info("Tomcat initialized with " + getPortsDescription(false));
		synchronized (this.monitor) {
			try {
				addInstanceIdToEngineName();

				Context context = findContext();
				context.addLifecycleListener((event) -> {
					if (context.equals(event.getSource()) && Lifecycle.START_EVENT.equals(event.getType())) {
						// Remove service connectors so that protocol binding doesn't
						// happen when the service is started.
						removeServiceConnectors();
					}
				});

				disableBindOnInit();

				// Start the server to trigger initialization listeners
				this.tomcat.start();

				// We can re-throw failure exception directly in the main thread
				rethrowDeferredStartupExceptions();

				try {
					ContextBindings.bindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
				}
				catch (NamingException ex) {
					// Naming is not enabled. Continue
				}

				// Unlike Jetty, all Tomcat threads are daemon threads. We create a
				// blocking non-daemon to stop immediate shutdown
				startNonDaemonAwaitThread();
			}
			catch (Exception ex) {
				stopSilently();
				destroySilently();
				throw new WebServerException("Unable to start embedded Tomcat", ex);
			}
		}
	}

	/**
     * Finds and returns the Context object associated with the TomcatWebServer.
     * 
     * @return the Context object
     * @throws IllegalStateException if the host does not contain a Context
     */
    private Context findContext() {
		for (Container child : this.tomcat.getHost().findChildren()) {
			if (child instanceof Context context) {
				return context;
			}
		}
		throw new IllegalStateException("The host does not contain a Context");
	}

	/**
     * Adds an instance ID to the engine name.
     * 
     * This method increments the container counter and appends the instance ID to the engine name
     * of the TomcatWebServer. The instance ID is obtained by calling the incrementAndGet() method
     * on the containerCounter. If the instance ID is greater than 0, the engine name is updated
     * by appending the instance ID to it.
     */
    private void addInstanceIdToEngineName() {
		int instanceId = containerCounter.incrementAndGet();
		if (instanceId > 0) {
			Engine engine = this.tomcat.getEngine();
			engine.setName(engine.getName() + "-" + instanceId);
		}
	}

	/**
     * Removes the service connectors from the TomcatWebServer.
     * 
     * This method iterates over the service connectors and removes them from the TomcatWebServer's service.
     * The removed connectors are stored in the serviceConnectors map for future reference.
     */
    private void removeServiceConnectors() {
		doWithConnectors((service, connectors) -> {
			this.serviceConnectors.put(service, connectors);
			for (Connector connector : connectors) {
				service.removeConnector(connector);
			}
		});
	}

	/**
     * Disables the bind on init functionality for all connectors in the TomcatWebServer.
     * This method iterates through all the connectors and checks if the "bindOnInit" property is set.
     * If the property is not set, it sets the "bindOnInit" property to "false".
     */
    private void disableBindOnInit() {
		doWithConnectors((service, connectors) -> {
			for (Connector connector : connectors) {
				Object bindOnInit = connector.getProperty("bindOnInit");
				if (bindOnInit == null) {
					connector.setProperty("bindOnInit", "false");
				}
			}
		});
	}

	/**
     * Executes the specified consumer function for each service and its corresponding connectors in the TomcatWebServer.
     * 
     * @param consumer the consumer function to be executed for each service and connectors
     */
    private void doWithConnectors(BiConsumer<Service, Connector[]> consumer) {
		for (Service service : this.tomcat.getServer().findServices()) {
			Connector[] connectors = service.findConnectors().clone();
			consumer.accept(service, connectors);
		}
	}

	/**
     * Rethrows any deferred startup exceptions that occurred during the initialization of the Tomcat web server.
     * 
     * @throws Exception if any deferred startup exception occurred during the initialization process.
     */
    private void rethrowDeferredStartupExceptions() throws Exception {
		Container[] children = this.tomcat.getHost().findChildren();
		for (Container container : children) {
			if (container instanceof TomcatEmbeddedContext embeddedContext) {
				TomcatStarter tomcatStarter = embeddedContext.getStarter();
				if (tomcatStarter != null) {
					Exception exception = tomcatStarter.getStartUpException();
					if (exception != null) {
						throw exception;
					}
				}
			}
			if (!LifecycleState.STARTED.equals(container.getState())) {
				throw new IllegalStateException(container + " failed to start");
			}
		}
	}

	/**
     * Starts a non-daemon await thread for the TomcatWebServer.
     * This method creates a new thread that waits for the Tomcat server to finish processing requests.
     * The thread is given a name based on the container counter.
     * The thread's run method calls the await method of the Tomcat server to wait for it to finish.
     * The thread's context class loader is set to the class loader of the TomcatWebServer class.
     * The thread is set to be a non-daemon thread.
     * The thread is started and runs concurrently with the main thread.
     */
    private void startNonDaemonAwaitThread() {
		Thread awaitThread = new Thread("container-" + (containerCounter.get())) {

			@Override
			public void run() {
				TomcatWebServer.this.tomcat.getServer().await();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	/**
     * Starts the embedded Tomcat server.
     *
     * @throws WebServerException if unable to start the server
     */
    @Override
	public void start() throws WebServerException {
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
				logger.info(getStartedLogMessage());
			}
			catch (ConnectorStartFailedException ex) {
				stopSilently();
				throw ex;
			}
			catch (Exception ex) {
				PortInUseException.throwIfPortBindingException(ex, () -> this.tomcat.getConnector().getPort());
				throw new WebServerException("Unable to start embedded Tomcat server", ex);
			}
			finally {
				Context context = findContext();
				ContextBindings.unbindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
			}
		}
	}

	/**
     * Returns the log message indicating the start of the Tomcat server.
     * 
     * @return the log message indicating the start of the Tomcat server
     */
    String getStartedLogMessage() {
		String contextPath = getContextPath();
		return "Tomcat started on " + getPortsDescription(true)
				+ ((contextPath != null) ? " with context path '" + contextPath + "'" : "");
	}

	/**
     * Checks if the connectors of the TomcatWebServer have started.
     * 
     * @param tomcatConnector The Tomcat connector to be checked.
     */
    private void checkThatConnectorsHaveStarted() {
		checkConnectorHasStarted(this.tomcat.getConnector());
		for (Connector connector : this.tomcat.getService().findConnectors()) {
			checkConnectorHasStarted(connector);
		}
	}

	/**
     * Checks if the given Connector has started successfully.
     * 
     * @param connector The Connector to check.
     * @throws ConnectorStartFailedException If the Connector has failed to start.
     */
    private void checkConnectorHasStarted(Connector connector) {
		if (LifecycleState.FAILED.equals(connector.getState())) {
			throw new ConnectorStartFailedException(connector.getPort());
		}
	}

	/**
     * Stops the Tomcat server silently.
     * <p>
     * This method attempts to stop the Tomcat server without throwing any exceptions.
     * If a {@link LifecycleException} occurs during the stop process, it will be ignored.
     * </p>
     */
    private void stopSilently() {
		try {
			stopTomcat();
		}
		catch (LifecycleException ex) {
			// Ignore
		}
	}

	/**
     * Destroys the Tomcat web server instance silently.
     * <p>
     * This method attempts to destroy the Tomcat web server instance without throwing any exceptions.
     * If a {@link LifecycleException} occurs during the destruction process, it is ignored.
     * </p>
     */
    private void destroySilently() {
		try {
			this.tomcat.destroy();
		}
		catch (LifecycleException ex) {
			// Ignore
		}
	}

	/**
     * Stops the Tomcat server.
     * 
     * @throws LifecycleException if an error occurs during the server stop process
     */
    private void stopTomcat() throws LifecycleException {
		if (Thread.currentThread().getContextClassLoader() instanceof TomcatEmbeddedWebappClassLoader) {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		}
		this.tomcat.stop();
	}

	/**
     * Adds previously removed connectors to the Tomcat server.
     * 
     * This method retrieves the services from the Tomcat server and adds the previously removed connectors
     * to each service. If the 'autoStart' flag is set to false, the protocol handler for each connector is stopped.
     * 
     * @see TomcatWebServer
     * @since version 1.0
     */
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

	/**
     * Stops the protocol handler for the given connector.
     * 
     * @param connector the connector for which the protocol handler needs to be stopped
     */
    private void stopProtocolHandler(Connector connector) {
		try {
			connector.getProtocolHandler().stop();
		}
		catch (Exception ex) {
			logger.error("Cannot pause connector: ", ex);
		}
	}

	/**
     * Performs deferred load on startup for all embedded contexts in the Tomcat host.
     * This method is called during the startup of the TomcatWebServer.
     * 
     * @throws WebServerException if unable to start embedded Tomcat connectors
     */
    private void performDeferredLoadOnStartup() {
		try {
			for (Container child : this.tomcat.getHost().findChildren()) {
				if (child instanceof TomcatEmbeddedContext embeddedContext) {
					embeddedContext.deferredLoadOnStartup();
				}
			}
		}
		catch (Exception ex) {
			if (ex instanceof WebServerException webServerException) {
				throw webServerException;
			}
			throw new WebServerException("Unable to start embedded Tomcat connectors", ex);
		}
	}

	/**
     * Returns a map of services and their corresponding connectors.
     * 
     * @return the map of services and connectors
     */
    Map<Service, Connector[]> getServiceConnectors() {
		return this.serviceConnectors;
	}

	/**
     * Stops the embedded Tomcat web server.
     * 
     * @throws WebServerException if unable to stop the server
     */
    @Override
	public void stop() throws WebServerException {
		synchronized (this.monitor) {
			boolean wasStarted = this.started;
			try {
				this.started = false;
				if (this.gracefulShutdown != null) {
					this.gracefulShutdown.abort();
				}
				removeServiceConnectors();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop embedded Tomcat", ex);
			}
			finally {
				if (wasStarted) {
					containerCounter.decrementAndGet();
				}
			}
		}
	}

	/**
     * Destroys the embedded Tomcat server.
     * 
     * @throws WebServerException if unable to destroy the embedded Tomcat server
     */
    @Override
	public void destroy() throws WebServerException {
		try {
			stopTomcat();
			this.tomcat.destroy();
		}
		catch (LifecycleException ex) {
			// Swallow and continue
		}
		catch (Exception ex) {
			throw new WebServerException("Unable to destroy embedded Tomcat", ex);
		}
	}

	/**
     * Returns a description of the ports used by the TomcatWebServer.
     * 
     * @param localPort true if the local port should be used, false if the remote port should be used
     * @return a string containing the description of the ports used by the TomcatWebServer
     */
    private String getPortsDescription(boolean localPort) {
		StringBuilder description = new StringBuilder();
		Connector[] connectors = this.tomcat.getService().findConnectors();
		description.append("port");
		if (connectors.length != 1) {
			description.append("s");
		}
		description.append(" ");
		for (int i = 0; i < connectors.length; i++) {
			if (i != 0) {
				description.append(", ");
			}
			Connector connector = connectors[i];
			int port = localPort ? connector.getLocalPort() : connector.getPort();
			description.append(port).append(" (").append(connector.getScheme()).append(')');
		}
		return description.toString();
	}

	/**
     * Returns the port number on which the Tomcat web server is running.
     * 
     * @return the port number if the web server is running, -1 otherwise
     */
    @Override
	public int getPort() {
		Connector connector = this.tomcat.getConnector();
		if (connector != null) {
			return connector.getLocalPort();
		}
		return -1;
	}

	/**
     * Returns the context path of the Tomcat web server.
     * 
     * @return the context path as a String, or null if it is empty or not found
     */
    private String getContextPath() {
		String contextPath = Arrays.stream(this.tomcat.getHost().findChildren())
			.filter(TomcatEmbeddedContext.class::isInstance)
			.map(TomcatEmbeddedContext.class::cast)
			.filter(this::imperative)
			.map(TomcatEmbeddedContext::getPath)
			.map((path) -> path.equals("") ? "/" : path)
			.collect(Collectors.joining(" "));
		return StringUtils.hasText(contextPath) ? contextPath : null;
	}

	/**
     * Checks if the given TomcatEmbeddedContext has any child containers that are instances of Wrapper and have a servlet class
     * equal to "org.springframework.http.server.reactive.TomcatHttpHandlerAdapter".
     * 
     * @param context the TomcatEmbeddedContext to check
     * @return true if no child containers match the specified conditions, false otherwise
     */
    private boolean imperative(TomcatEmbeddedContext context) {
		for (Container container : context.findChildren()) {
			if (container instanceof Wrapper wrapper) {
				if (wrapper.getServletClass()
					.equals("org.springframework.http.server.reactive.TomcatHttpHandlerAdapter")) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns access to the underlying Tomcat server.
	 * @return the Tomcat server
	 */
	public Tomcat getTomcat() {
		return this.tomcat;
	}

	/**
     * Shuts down the web server gracefully.
     * 
     * @param callback the callback to be invoked when the shutdown is complete
     */
    @Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		this.gracefulShutdown.shutDownGracefully(callback);
	}

}
