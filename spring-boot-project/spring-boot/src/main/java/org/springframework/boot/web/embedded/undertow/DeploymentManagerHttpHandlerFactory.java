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

	DeploymentManagerHttpHandlerFactory(DeploymentManager deploymentManager) {
		this.deploymentManager = deploymentManager;
	}

	@Override
	public HttpHandler getHandler(HttpHandler next) {
		Assert.state(next == null, "DeploymentManagerHttpHandlerFactory must be first");
		return new DeploymentManagerHandler(this.deploymentManager);
	}

	DeploymentManager getDeploymentManager() {
		return this.deploymentManager;
	}

	/**
	 * {@link HttpHandler} that delegates to a {@link DeploymentManager}.
	 */
	static class DeploymentManagerHandler implements HttpHandler, Closeable {

		private final DeploymentManager deploymentManager;

		private final HttpHandler handler;

		DeploymentManagerHandler(DeploymentManager deploymentManager) {
			this.deploymentManager = deploymentManager;
			try {
				this.handler = deploymentManager.start();
			}
			catch (ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			this.handler.handleRequest(exchange);
		}

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

		DeploymentManager getDeploymentManager() {
			return this.deploymentManager;
		}

	}

}
