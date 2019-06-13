/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.springframework.util.Assert;

/**
 * Exception thrown when a {@link ConfigurationProperties} has been misconfigured.
 *
 * @author Madhura Bhave
 * @since 2.2.0
 */
public class InvalidConfigurationPropertiesException extends RuntimeException {

	private final Class<?> configurationProperties;

	private final Class<?> component;

	public InvalidConfigurationPropertiesException(Class<?> configurationProperties, Class<?> component) {
		super("Found @" + component.getSimpleName() + " and @ConfigurationProperties on "
				+ configurationProperties.getName() + ".");
		Assert.notNull(configurationProperties, "Class must not be null");
		this.configurationProperties = configurationProperties;
		this.component = component;
	}

	public Class<?> getConfigurationProperties() {
		return this.configurationProperties;
	}

	public Class<?> getComponent() {
		return this.component;
	}

}
