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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;

import org.springframework.boot.web.server.WebServerException;

/**
 * A {@code ConnectorStartFailedException} is thrown when a Tomcat {@link Connector} fails
 * to start, for example due to a port clash or incorrect SSL configuration.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ConnectorStartFailedException extends WebServerException {

	private final int port;

	/**
	 * Creates a new {@code ConnectorStartFailedException} for a connector that's
	 * configured to listen on the given {@code port}.
	 * @param port the port
	 */
	public ConnectorStartFailedException(int port) {
		super("Connector configured to listen on port " + port + " failed to start", null);
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

}
