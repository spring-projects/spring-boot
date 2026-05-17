/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.context;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.event.PortBound;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationEvent;

/**
 * Event to be published when the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@SuppressWarnings("serial")
public abstract class WebServerInitializedEvent extends ApplicationEvent implements PortBound {

	protected WebServerInitializedEvent(WebServer webServer) {
		super(webServer);
	}

	/**
	 * Return the port that the {@link WebServer} bound to.
	 * @return the bound port
	 */
	@Override
	public int getPort() {
		return getWebServer().getPort();
	}

	/**
	 * Return the namespace of the underlying web server, if available.
	 * @return the server namespace or {@code null}
	 */
	@Override
	public @Nullable String getNamespace() {
		return getApplicationContext().getServerNamespace();
	}

	/**
	 * Access the {@link WebServer}.
	 * @return the embedded web server
	 */
	public WebServer getWebServer() {
		return getSource();
	}

	/**
	 * Access the application context that the server was created in. Sometimes it is
	 * prudent to check that this matches expectations (like being equal to the current
	 * context) before acting on the server itself.
	 * @return the applicationContext that the server was created from
	 */
	public abstract WebServerApplicationContext getApplicationContext();

	/**
	 * Access the source of the event (an {@link WebServer}).
	 * @return the embedded web server
	 */
	@Override
	public WebServer getSource() {
		return (WebServer) super.getSource();
	}

}
