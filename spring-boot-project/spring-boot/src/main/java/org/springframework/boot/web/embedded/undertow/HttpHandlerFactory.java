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

package org.springframework.boot.web.embedded.undertow;

import java.io.Closeable;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

/**
 * Factory used by {@link UndertowServletWebServer} to add {@link HttpHandler
 * HttpHandlers}. Instances returned from this factory may optionally implement the
 * following interfaces:
 * <ul>
 * <li>{@link Closeable} - if they wish to be closed just before server stops.</li>
 * <li>{@link GracefulShutdownHandler} - if they wish to manage graceful shutdown.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface HttpHandlerFactory {

	/**
	 * Create the {@link HttpHandler} instance that should be added.
	 * @param next the next handler in the chain
	 * @return the new HTTP handler instance
	 */
	HttpHandler getHandler(HttpHandler next);

}
