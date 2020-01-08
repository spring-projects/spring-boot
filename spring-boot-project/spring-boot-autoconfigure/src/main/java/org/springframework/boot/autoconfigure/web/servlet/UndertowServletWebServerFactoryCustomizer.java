/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import io.undertow.Handlers;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimitingHandler;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Undertow
 * Servlet web servers.
 *
 * @author Andy Wilkinson
 * @author Chris Bono
 * @since 2.1.7
 */
public class UndertowServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

	private final ServerProperties serverProperties;

	public UndertowServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	// TODO why was this not being used before now?

	@Override
	public void customize(UndertowServletWebServerFactory factory) {

		factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo
				.setEagerFilterInit(this.serverProperties.getUndertow().isEagerFilterInit()));

		addRateLimiterIfMaxRequestsSet(factory);
	}

	/**
	 * Installs a {@link RequestLimitingHandler} in the initial handler chain if
	 * {@link ServerProperties.Undertow#maxRequests} is set. If installed, it will use
	 * {@link ServerProperties.Undertow#maxQueueCapacity} to control the size of the queue
	 * in the rate limting handler.
	 * @param factory the factory to add the deployment info customizer on
	 */
	private void addRateLimiterIfMaxRequestsSet(UndertowServletWebServerFactory factory) {
		Integer maxRequests = this.serverProperties.getUndertow().getMaxRequests();
		if (maxRequests == null) {
			return;
		}
		Integer maxQueueCapacity = this.serverProperties.getUndertow().getMaxQueueCapacity();
		factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo.addInitialHandlerChainWrapper(
				new RequestLimiterHandlerWrapper(maxRequests, (maxQueueCapacity != null) ? maxQueueCapacity : -1)));
	}

	/**
	 * Explicit implementation of HandlerWrapper rather than Lambda to make it possible to
	 * verify the handler wrappers at runtime during tests. This could be instead written
	 * as a Lambda but then tests can not see what type of handler wrapper it is.
	 */
	static class RequestLimiterHandlerWrapper implements HandlerWrapper {

		Integer maxRequests;

		Integer maxQueueCapacity;

		RequestLimiterHandlerWrapper(Integer maxRequests, Integer maxQueueCapacity) {
			this.maxRequests = maxRequests;
			this.maxQueueCapacity = maxQueueCapacity;
		}

		@Override
		public HttpHandler wrap(HttpHandler handler) {
			return Handlers.requestLimitingHandler(this.maxRequests, this.maxQueueCapacity, handler);
		}

	}

}
