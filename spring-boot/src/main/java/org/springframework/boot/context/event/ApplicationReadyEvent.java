/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published as late as conceivably possible to indicate that the application is
 * ready to service requests. The source of the event is the created
 * {@link ConfigurableApplicationContext}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class ApplicationReadyEvent extends ApplicationEvent {

	private final String[] args;

	/**
	 * @param applicationContext the main application context
	 * @param args the arguments the application is running with
	 */
	public ApplicationReadyEvent(ConfigurableApplicationContext applicationContext, String[] args) {
		super(applicationContext);
		this.args = args;
	}

	public ConfigurableApplicationContext getApplicationContext() {
		return (ConfigurableApplicationContext) getSource();
	}

	public String[] getArgs() {
		return args;
	}

}
