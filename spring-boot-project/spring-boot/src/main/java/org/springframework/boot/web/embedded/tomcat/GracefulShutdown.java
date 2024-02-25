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

package org.springframework.boot.web.embedded.tomcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;

/**
 * Handles Tomcat graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Tomcat tomcat;

	private volatile boolean aborted = false;

	/**
     * Initiates a graceful shutdown of the specified Tomcat server.
     * 
     * @param tomcat the Tomcat server to be gracefully shutdown
     */
    GracefulShutdown(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	/**
     * Initiates a graceful shutdown of the application.
     * 
     * @param callback the callback function to be executed after the shutdown is complete
     */
    void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		new Thread(() -> doShutdown(callback), "tomcat-shutdown").start();
	}

	/**
     * Performs a graceful shutdown of the server.
     * 
     * @param callback the callback to be invoked when the shutdown is complete
     */
    private void doShutdown(GracefulShutdownCallback callback) {
		List<Connector> connectors = getConnectors();
		connectors.forEach(this::close);
		try {
			for (Container host : this.tomcat.getEngine().findChildren()) {
				for (Container context : host.findChildren()) {
					while (!this.aborted && isActive(context)) {
						Thread.sleep(50);
					}
					if (this.aborted) {
						logger.info("Graceful shutdown aborted with one or more requests still active");
						callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
						return;
					}
				}
			}

		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		logger.info("Graceful shutdown complete");
		callback.shutdownComplete(GracefulShutdownResult.IDLE);
	}

	/**
     * Retrieves a list of connectors from the Tomcat server.
     * 
     * @return A list of Connector objects representing the connectors found in the Tomcat server.
     */
    private List<Connector> getConnectors() {
		List<Connector> connectors = new ArrayList<>();
		for (Service service : this.tomcat.getServer().findServices()) {
			Collections.addAll(connectors, service.findConnectors());
		}
		return connectors;
	}

	/**
     * Closes the given connector gracefully.
     * <p>
     * This method pauses the given connector and then closes the server socket associated with the connector's protocol handler.
     * </p>
     *
     * @param connector the connector to be closed gracefully
     */
    private void close(Connector connector) {
		connector.pause();
		connector.getProtocolHandler().closeServerSocketGraceful();
	}

	/**
     * Checks if the given container is active.
     * 
     * @param context the container to check
     * @return true if the container is active, false otherwise
     * @throws RuntimeException if an exception occurs during the check
     */
    private boolean isActive(Container context) {
		try {
			if (((StandardContext) context).getInProgressAsyncCount() > 0) {
				return true;
			}
			for (Container wrapper : context.findChildren()) {
				if (((StandardWrapper) wrapper).getCountAllocated() > 0) {
					return true;
				}
			}
			return false;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
     * Aborts the execution of the program.
     * Sets the 'aborted' flag to true.
     */
    void abort() {
		this.aborted = true;
	}

}
