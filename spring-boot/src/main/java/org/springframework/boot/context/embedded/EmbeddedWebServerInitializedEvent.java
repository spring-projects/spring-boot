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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * Event to be published after the application context is
 * refreshed and the {@link EmbeddedWebServer} is ready. Useful for
 * obtaining the local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see EmbeddedServletContainerInitializedEvent
 * @see EmbeddedReactiveWebServerInitializedEvent
 */
@SuppressWarnings("serial")
public abstract class EmbeddedWebServerInitializedEvent extends ApplicationEvent {

	protected EmbeddedWebServerInitializedEvent(EmbeddedWebServer source) {
		super(source);
	}

	/**
	 * Access the {@link EmbeddedWebServer}.
	 * @return the embedded web server
	 */
	public EmbeddedWebServer getEmbeddedWebServer() {
		return getSource();
	}

	/**
	 * Access the application context that the container was created in. Sometimes it is
	 * prudent to check that this matches expectations (like being equal to the current
	 * context) before acting on the server container itself.
	 * @return the applicationContext that the container was created from
	 */
	public abstract ApplicationContext getApplicationContext();

	/**
	 * Access the source of the event (an {@link EmbeddedWebServer}).
	 * @return the embedded web server
	 */
	@Override
	public EmbeddedWebServer getSource() {
		return (EmbeddedWebServer) super.getSource();
	}

	/**
	 * Access the {@link EmbeddedWebServer} Id used internally
	 * to differentiate application / management servers.
	 * @return the server internal Id
	 */
	public abstract String getServerId();

}
