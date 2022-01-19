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

	@Override
	public Object resolvePlaceholders(Object value) {
		if (value instanceof String) {
			return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
		}
		return value;
	}

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
