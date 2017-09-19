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

package org.springframework.boot.web.server;

/**
 * Simple interface that represents a fully configured web server (for example Tomcat,
 * Jetty, Netty). Allows the server to be {@link #start() started} and {@link #stop()
 * stopped}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 2.0.0
 */
public interface WebServer {

	/**
	 * Starts the web server. Calling this method on an already started server has no
	 * effect.
	 * @throws WebServerException if the server cannot be started
	 */
	void start() throws WebServerException;

	/**
	 * Stops the web server. Calling this method on an already stopped server has no
	 * effect.
	 * @throws WebServerException if the server cannot be stopped
	 */
	void stop() throws WebServerException;

	/**
	 * Return the port this server is listening on.
	 * @return the port (or -1 if none)
	 */
	int getPort();

}
