/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;

/**
 * Factory interface that can be used to create reactive {@link EmbeddedWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 * @see EmbeddedWebServer
 */
public interface ReactiveWebServerFactory {

	/**
	 * Gets a new fully configured but paused {@link EmbeddedWebServer} instance.
	 * Clients should not be able to connect to the returned server until
	 * {@link EmbeddedWebServer#start()} is called (which happens when the
	 * {@link ApplicationContext} has been fully refreshed).
	 * @param httpHandler the HTTP handler in charge of processing requests
	 * @return a fully configured and started {@link EmbeddedWebServer}
	 * @see EmbeddedWebServer#stop()
	 */
	EmbeddedWebServer getReactiveHttpServer(HttpHandler httpHandler);

	/**
	 * Register a map of {@link HttpHandler}s, each to a specific context path.
	 *
	 * @param handlerMap a map of context paths and the associated {@code HttpHandler}
	 * @return a fully configured and started {@link EmbeddedWebServer}
	 * @see EmbeddedWebServer#stop()
	 */
	EmbeddedWebServer getReactiveHttpServer(Map<String, HttpHandler> handlerMap);
}
