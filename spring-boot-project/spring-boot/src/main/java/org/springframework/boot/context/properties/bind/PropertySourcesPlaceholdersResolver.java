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

package org.springframework.boot.context.properties.bind;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver} to resolve placeholders from {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {

	private final Iterable<PropertySource<?>> sources;

	private final PropertyPlaceholderHelper helper;

	/**
     * Constructs a new PropertySourcesPlaceholdersResolver with the specified environment.
     * 
     * @param environment the environment to use for resolving property sources
     */
    public PropertySourcesPlaceholdersResolver(Environment environment) {
		this(getSources(environment), null);
	}

	/**
     * Constructs a new PropertySourcesPlaceholdersResolver with the specified sources.
     *
     * @param sources the sources to be used for resolving placeholders
     */
    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources) {
		this(sources, null);
	}

	/**
     * Constructs a new PropertySourcesPlaceholdersResolver with the specified sources and helper.
     * 
     * @param sources the sources of property values
     * @param helper the property placeholder helper to use for resolving placeholders
     */
    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources, PropertyPlaceholderHelper helper) {
		this.sources = sources;
		this.helper = (helper != null) ? helper : new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
				SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);
	}

	/**
     * Resolves placeholders in the given value.
     * 
     * @param value the value to resolve placeholders in
     * @return the resolved value
     */
    @Override
	public Object resolvePlaceholders(Object value) {
		if (value instanceof String string) {
			return this.helper.replacePlaceholders(string, this::resolvePlaceholder);
		}
		return value;
	}

	/**
     * Resolves the value of the given placeholder.
     * 
     * @param placeholder the placeholder to resolve
     * @return the resolved value of the placeholder, or null if not found
     */
    protected String resolvePlaceholder(String placeholder) {
		if (this.sources != null) {
			for (PropertySource<?> source : this.sources) {
				Object value = source.getProperty(placeholder);
				if (value != null) {
					return String.valueOf(value);
				}
			}
		}
		return null;
	}

	/**
     * Retrieves the property sources from the given environment.
     * 
     * @param environment the environment from which to retrieve the property sources (must not be null)
     * @return the property sources of the environment
     * @throws IllegalArgumentException if the environment is null or not an instance of ConfigurableEnvironment
     */
    private static PropertySources getSources(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
				"Environment must be a ConfigurableEnvironment");
		return ((ConfigurableEnvironment) environment).getPropertySources();
	}

}
