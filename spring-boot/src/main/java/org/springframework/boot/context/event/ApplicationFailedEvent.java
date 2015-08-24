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

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published by a {@link SpringApplication} when it fails to start.
 *
 * @author Dave Syer
 * @see ApplicationReadyEvent
 */
@SuppressWarnings("serial")
public class ApplicationFailedEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	private final Throwable exception;

	/**
	 * @param application the current application
	 * @param args the arguments the application was running with
	 * @param context the context that was being created (maybe null)
	 * @param exception the exception that caused the error
	 */
	public ApplicationFailedEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context, Throwable exception) {
		super(application, args);
		this.context = context;
		this.exception = exception;
	}

	/**
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

	/**
	 * @return the exception
	 */
	public Throwable getException() {
		return this.exception;
	}

}
