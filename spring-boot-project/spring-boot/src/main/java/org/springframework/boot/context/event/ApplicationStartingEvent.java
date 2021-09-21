/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.event;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Event published as early as conceivably possible as soon as a {@link SpringApplication}
 * has been started - before the {@link Environment} or {@link ApplicationContext} is
 * available, but after the {@link ApplicationListener}s have been registered. The source
 * of the event is the {@link SpringApplication} itself, but beware of using its internal
 * state too much at this early stage since it might be modified later in the lifecycle.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.5.0
 */
@SuppressWarnings("serial")
public class ApplicationStartingEvent extends SpringApplicationEvent {

	private final ConfigurableBootstrapContext bootstrapContext;

	/**
	 * Create a new {@link ApplicationStartingEvent} instance.
	 * @param bootstrapContext the bootstrap context
	 * @param application the current application
	 * @param args the arguments the application is running with
	 */
	public ApplicationStartingEvent(ConfigurableBootstrapContext bootstrapContext, SpringApplication application,
			String[] args) {
		super(application, args);
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * Return the bootstrap context.
	 * @return the bootstrap context
	 * @since 2.4.0
	 */
	public ConfigurableBootstrapContext getBootstrapContext() {
		return this.bootstrapContext;
	}

}
