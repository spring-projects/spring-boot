/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationEvent} published by a {@link BootstrapContext} when it's closed.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see BootstrapRegistry#addCloseListener(org.springframework.context.ApplicationListener)
 */
public class BootstrapContextClosedEvent extends ApplicationEvent {

	private final ConfigurableApplicationContext applicationContext;

	BootstrapContextClosedEvent(BootstrapContext source, ConfigurableApplicationContext applicationContext) {
		super(source);
		this.applicationContext = applicationContext;
	}

	/**
	 * Return the {@link BootstrapContext} that was closed.
	 * @return the bootstrap context
	 */
	public BootstrapContext getBootstrapContext() {
		return (BootstrapContext) this.source;
	}

	/**
	 * Return the prepared application context.
	 * @return the application context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
