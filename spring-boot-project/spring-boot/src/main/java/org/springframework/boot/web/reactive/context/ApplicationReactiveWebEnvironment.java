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

package org.springframework.boot.web.reactive.context;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;

/**
 * {@link StandardReactiveWebEnvironment} for typical use in a typical
 * {@link SpringApplication}.
 *
 * @author Phillip Webb
 */
class ApplicationReactiveWebEnvironment extends StandardReactiveWebEnvironment {

	/**
	 * Returns the value of the active profiles property.
	 * @return the value of the active profiles property, or null if not set
	 */
	@Override
	protected String doGetActiveProfilesProperty() {
		return null;
	}

	/**
	 * Returns the value of the default profiles property for this environment.
	 * @return the value of the default profiles property, or {@code null} if not set
	 */
	@Override
	protected String doGetDefaultProfilesProperty() {
		return null;
	}

	/**
	 * Creates a ConfigurablePropertyResolver using the provided MutablePropertySources.
	 * @param propertySources the MutablePropertySources to use for resolving properties
	 * @return the created ConfigurablePropertyResolver
	 */
	@Override
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return ConfigurationPropertySources.createPropertyResolver(propertySources);
	}

}
