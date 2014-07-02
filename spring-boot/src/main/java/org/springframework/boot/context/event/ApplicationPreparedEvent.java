/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Event published as when a {@link SpringApplication} is starting up and the
 * {@link ApplicationContext} is fully prepared but not refreshed. The bean definitions
 * will be loaded and the {@link Environment} is ready for use at this stage.
 *
 * @author Dave Syer
 */
public class ApplicationPreparedEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	/**
	 * @param application the current application
	 * @param args the argumemts the application is running with
	 * @param context the ApplicationContext about to be refreshed
	 */
	public ApplicationPreparedEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context) {
		super(application, args);
		this.context = context;
	}

	/**
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}
