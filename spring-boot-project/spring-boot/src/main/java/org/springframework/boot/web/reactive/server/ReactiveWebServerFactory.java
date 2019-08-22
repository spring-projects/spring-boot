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

package org.springframework.boot.web.reactive.server;

import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;

/**
 * Factory interface that can be used to create a reactive {@link WebServer}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 * @see WebServer
 */
@FunctionalInterface
public interface ReactiveWebServerFactory {

	/**
	 * Gets a new fully configured but paused {@link WebServer} instance. Clients should
	 * not be able to connect to the returned server until {@link WebServer#start()} is
	 * called (which happens when the {@code ApplicationContext} has been fully
	 * refreshed).
	 * @param httpHandler the HTTP handler in charge of processing requests
	 * @return a fully configured and started {@link WebServer}
	 * @see WebServer#stop()
	 */
	WebServer getWebServer(HttpHandler httpHandler);

}
