/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * A filtered {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final Predicate<ConfigurationPropertyName> filter;

	FilteredConfigurationPropertiesSource(ConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.source = source;
		this.filter = filter;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		boolean filtered = getFilter().test(name);
		return (filtered ? getSource().getConfigurationProperty(name) : null);
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(
			ConfigurationPropertyName name) {
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result == ConfigurationPropertyState.PRESENT) {
			// We can't be sure a contained descendant won't be filtered
			return ConfigurationPropertyState.UNKNOWN;
		}
		return result;
	}

	@Override
	public Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

	protected Predicate<ConfigurationPropertyName> getFilter() {
		return this.filter;
	}

	@Override
	public String toString() {
		return this.source.toString() + " (filtered)";
	}

}
