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

package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published as late as conceivably possible to indicate that the application is
 * ready to service requests. The source of the event is the {@link SpringApplication}
 * itself, but beware of modifying its internal state since all initialization steps will
 * have been completed by then.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see ApplicationFailedEvent
 */
@SuppressWarnings("serial")
public class ApplicationReadyEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	/**
	 * Create a new {@link ApplicationReadyEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application is running with
	 * @param context the context that was being created
	 */
	public ApplicationReadyEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context) {
		super(application, args);
		this.context = context;
	}

	/**
	 * Return the application context.
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}
