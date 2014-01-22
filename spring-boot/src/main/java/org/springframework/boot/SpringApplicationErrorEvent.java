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

package org.springframework.boot;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published by a {@link SpringApplication} when it fails to start.
 * 
 * @author Dave Syer
 */
public class SpringApplicationErrorEvent extends ApplicationEvent {

	private final String[] args;
	private final Throwable exception;
	private final ConfigurableApplicationContext context;

	/**
	 * @param springApplication the current application
	 * @param context the context that was being created (maybe null)
	 * @param args the arguments the application was running with
	 * @param exception the exception that caused the error
	 */
	public SpringApplicationErrorEvent(SpringApplication springApplication,
			ConfigurableApplicationContext context, String[] args, Throwable exception) {
		super(springApplication);
		this.context = context;
		this.args = args;
		this.exception = exception;
	}

	/**
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

	/**
	 * @return the springApplication
	 */
	public SpringApplication getSpringApplication() {
		return (SpringApplication) getSource();
	}

	/**
	 * @return the exception
	 */
	public Throwable getException() {
		return this.exception;
	}

	/**
	 * @return the args
	 */
	public String[] getArgs() {
		return this.args;
	}

}
