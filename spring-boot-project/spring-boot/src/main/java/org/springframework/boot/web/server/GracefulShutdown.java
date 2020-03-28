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

package org.springframework.boot.web.server;

/**
 * Handles graceful shutdown of a {@link WebServer}.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
public interface GracefulShutdown {

	/**
	 * A {@link GracefulShutdown} that returns immediately with no grace period.
	 */
	GracefulShutdown IMMEDIATE = new ImmediateGracefulShutdown();

	/**
	 * Shuts down the {@link WebServer}, returning {@code true} if activity ceased during
	 * the grace period, otherwise {@code false}.
	 * @return {@code true} if activity ceased during the grace period, otherwise
	 * {@code false}
	 */
	boolean shutDownGracefully();

	/**
	 * Returns whether the handler is in the process of gracefully shutting down the web
	 * server.
	 * @return {@code true} is graceful shutdown is in progress, otherwise {@code false}.
	 */
	boolean isShuttingDown();

}
