/*
 * Copyright 2012-2013 the original author or authors.
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
 * Event to be published after the context is refreshed and the
 * {@link EmbeddedServletContainer} is ready. Useful for obtaining the local port of a
 * running server. Normally it will have been started, but listeners are free to inspect
 * the server and stop and start it if they want to.
 * 
 * @author Dave Syer
 */
public class EmbeddedServletContainerInitializedEvent extends ApplicationEvent {

	private ApplicationContext applicationContext;

	public EmbeddedServletContainerInitializedEvent(
			ApplicationContext applicationContext, EmbeddedServletContainer source) {
		super(source);
		this.applicationContext = applicationContext;
	}

	/**
	 * Access the source of the event (an {@link EmbeddedServletContainer}).
	 * 
	 * @return the embedded servlet container
	 */
	@Override
	public EmbeddedServletContainer getSource() {
		return (EmbeddedServletContainer) super.getSource();
	}

	/**
	 * Access the application context that the container was created in. Sometimes it is
	 * prudent to check that this matches expectations (like being equal to the current
	 * context) before acting on the server container itself.
	 * 
	 * @return the applicationContext that the container was created from
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}
}
