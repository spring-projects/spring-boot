/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.config;

import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.PropertySource;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver} backed by one or more
 * {@link ConfigDataEnvironmentContributor} instances.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributorPlaceholdersResolver implements PlaceholdersResolver {

	private final Iterable<ConfigDataEnvironmentContributor> contributors;

	private final ConfigDataActivationContext activationContext;

	private final boolean failOnResolveFromInactiveContributor;

	private final PropertyPlaceholderHelper helper;

	private final ConfigDataEnvironmentContributor activeContributor;

	/**
	 * Constructs a new ConfigDataEnvironmentContributorPlaceholdersResolver with the
	 * specified parameters.
	 * @param contributors the contributors to resolve placeholders from
	 * @param activationContext the activation context for resolving placeholders
	 * @param activeContributor the active contributor for resolving placeholders
	 * @param failOnResolveFromInactiveContributor whether to fail if resolving
	 * placeholders from an inactive contributor
	 */
	ConfigDataEnvironmentContributorPlaceholdersResolver(Iterable<ConfigDataEnvironmentContributor> contributors,
			ConfigDataActivationContext activationContext, ConfigDataEnvironmentContributor activeContributor,
			boolean failOnResolveFromInactiveContributor) {
		this.contributors = contributors;
		this.activationContext = activationContext;
		this.activeContributor = activeContributor;
		this.failOnResolveFromInactiveContributor = failOnResolveFromInactiveContributor;
		this.helper = new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
				SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);
	}

	/**
	 * Resolves placeholders in the given value.
	 * @param value the value to resolve placeholders in
	 * @return the value with resolved placeholders
	 */
	@Override
	public Object resolvePlaceholders(Object value) {
		if (value instanceof String string) {
			return this.helper.replacePlaceholders(string, this::resolvePlaceholder);
		}
		return value;
	}

	/**
	 * Resolves the placeholder value for the given placeholder string.
	 * @param placeholder the placeholder string to resolve
	 * @return the resolved placeholder value as a string, or null if the placeholder
	 * could not be resolved
	 * @throws InactiveConfigDataAccessException if the placeholder is resolved from an
	 * inactive contributor and failOnResolveFromInactiveContributor is set to true
	 */
	private String resolvePlaceholder(String placeholder) {
		Object result = null;
		for (ConfigDataEnvironmentContributor contributor : this.contributors) {
			PropertySource<?> propertySource = contributor.getPropertySource();
			Object value = (propertySource != null) ? propertySource.getProperty(placeholder) : null;
			if (value != null && !isActive(contributor)) {
				if (this.failOnResolveFromInactiveContributor) {
					ConfigDataResource resource = contributor.getResource();
					Origin origin = OriginLookup.getOrigin(propertySource, placeholder);
					throw new InactiveConfigDataAccessException(propertySource, resource, placeholder, origin);
				}
				value = null;
			}
			result = (result != null) ? result : value;
		}
		return (result != null) ? String.valueOf(result) : null;
	}

	/**
	 * Checks if the given ConfigDataEnvironmentContributor is active.
	 * @param contributor the ConfigDataEnvironmentContributor to check
	 * @return true if the contributor is active, false otherwise
	 */
	private boolean isActive(ConfigDataEnvironmentContributor contributor) {
		if (contributor == this.activeContributor) {
			return true;
		}
		if (contributor.getKind() != Kind.UNBOUND_IMPORT) {
			return contributor.isActive(this.activationContext);
		}
		return contributor.withBoundProperties(this.contributors, this.activationContext)
			.isActive(this.activationContext);
	}

}
