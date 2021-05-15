/*
 * Copyright 2012-2020 the original author or authors.
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

	GracefulShutdown(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		new Thread(() -> doShutdown(callback), "tomcat-shutdown").start();
	}

	private void doShutdown(GracefulShutdownCallback callback) {
		List<Connector> connectors = getConnectors();
		connectors.forEach(this::close);
		try {
			for (Container host : this.tomcat.getEngine().findChildren()) {
				for (Container context : host.findChildren()) {
					while (isActive(context)) {
						if (this.aborted) {
							logger.info("Graceful shutdown aborted with one or more requests still active");
							callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
							return;
						}
						Thread.sleep(50);
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

	private List<Connector> getConnectors() {
		List<Connector> connectors = new ArrayList<>();
		for (Service service : this.tomcat.getServer().findServices()) {
			Collections.addAll(connectors, service.findConnectors());
		}
		return connectors;
	}

	private void close(Connector connector) {
		connector.pause();
		connector.getProtocolHandler().closeServerSocketGraceful();
	}

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

	void abort() {
		this.aborted = true;
	}

}
