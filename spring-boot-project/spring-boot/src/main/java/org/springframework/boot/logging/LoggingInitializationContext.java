/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.logging;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Context passed to the {@link LoggingSystem} during initialization.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class LoggingInitializationContext {

	private final ConfigurableEnvironment environment;

	/**
	 * Create a new {@link LoggingInitializationContext} instance.
	 * @param environment the Spring environment.
	 */
	public LoggingInitializationContext(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the Spring environment if available.
	 * @return the {@link Environment} or {@code null}
	 */
	public Environment getEnvironment() {
		return this.environment;
	}

}
