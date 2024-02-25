/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.util.PropertySource;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Returns properties from Spring.
 *
 * @author Ralph Goers
 */
class SpringEnvironmentPropertySource implements PropertySource {

	/**
	 * System properties take precedence followed by properties in Log4j properties files.
	 */
	private static final int PRIORITY = -100;

	private final Environment environment;

	/**
	 * Constructs a new SpringEnvironmentPropertySource with the given Environment.
	 * @param environment the Environment to use for retrieving property values
	 * @throws IllegalArgumentException if the environment is null
	 */
	SpringEnvironmentPropertySource(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	/**
	 * Returns the priority of the SpringEnvironmentPropertySource.
	 * @return the priority value of the SpringEnvironmentPropertySource
	 */
	@Override
	public int getPriority() {
		return PRIORITY;
	}

	/**
	 * Retrieves the value of the property associated with the specified key.
	 * @param key the key of the property to retrieve
	 * @return the value of the property, or null if the property does not exist
	 */
	@Override
	public String getProperty(String key) {
		return this.environment.getProperty(key);
	}

	/**
	 * Checks if the property source contains a property with the specified key.
	 * @param key the key of the property to check
	 * @return true if the property source contains the property, false otherwise
	 */
	@Override
	public boolean containsProperty(String key) {
		return this.environment.containsProperty(key);
	}

}
