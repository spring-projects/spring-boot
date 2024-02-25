/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.Closeable;
import java.io.IOException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentManager;
import jakarta.servlet.ServletException;

import org.springframework.util.Assert;

/**
 * {@link HttpHandlerFactory} that for a {@link DeploymentManager}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DeploymentManagerHttpHandlerFactory implements HttpHandlerFactory {

	private final DeploymentManager deploymentManager;

	/**
	 * Constructs a new DeploymentManagerHttpHandlerFactory with the specified
	 * DeploymentManager.
	 * @param deploymentManager the DeploymentManager to be used by the factory
	 */
	DeploymentManagerHttpHandlerFactory(DeploymentManager deploymentManager) {
		this.deploymentManager = deploymentManager;
	}

	/**
	 * Returns the HTTP handler for the DeploymentManagerHttpHandlerFactory.
	 * @param next the next HTTP handler in the chain
	 * @return the HTTP handler for the DeploymentManagerHttpHandlerFactory
	 * @throws IllegalStateException if the next HTTP handler is not null
	 */
	@Override
	public HttpHandler getHandler(HttpHandler next) {
		Assert.state(next == null, "DeploymentManagerHttpHandlerFactory must be first");
		return new DeploymentManagerHandler(this.deploymentManager);
	}

	/**
	 * Returns the DeploymentManager instance associated with this
	 * DeploymentManagerHttpHandlerFactory.
	 * @return the DeploymentManager instance
	 */
	DeploymentManager getDeploymentManager() {
		return this.deploymentManager;
	}

	/**
	 * {@link HttpHandler} that delegates to a {@link DeploymentManager}.
	 */
	static class DeploymentManagerHandler implements HttpHandler, Closeable {

		private final DeploymentManager deploymentManager;

		private final HttpHandler handler;

		/**
		 * Constructs a new DeploymentManagerHandler with the specified DeploymentManager.
		 * @param deploymentManager the DeploymentManager to be used by this handler
		 * @throws RuntimeException if a ServletException occurs during the start of the
		 * DeploymentManager
		 */
		DeploymentManagerHandler(DeploymentManager deploymentManager) {
			this.deploymentManager = deploymentManager;
			try {
				this.handler = deploymentManager.start();
			}
			catch (ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

		/**
		 * Handles the HTTP server exchange request.
		 * @param exchange the HTTP server exchange object
		 * @throws Exception if an error occurs while handling the request
		 */
		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			this.handler.handleRequest(exchange);
		}

		/**
		 * Closes the DeploymentManagerHandler by stopping and undeploying the deployment
		 * manager.
		 * @throws IOException if an I/O error occurs while closing the
		 * DeploymentManagerHandler
		 * @throws RuntimeException if a ServletException occurs while stopping or
		 * undeploying the deployment manager
		 */
		@Override
		public void close() throws IOException {
			try {
				this.deploymentManager.stop();
				this.deploymentManager.undeploy();
			}
			catch (ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

		/**
		 * Returns the DeploymentManager object associated with this
		 * DeploymentManagerHandler.
		 * @return the DeploymentManager object
		 */
		DeploymentManager getDeploymentManager() {
			return this.deploymentManager;
		}

	}

}
