/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A filtered {@link IterableConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSource extends FilteredConfigurationPropertiesSource
		implements IterableConfigurationPropertySource {

	private ConfigurationPropertyName[] filteredNames;

	private int numerOfFilteredNames;

	FilteredIterableConfigurationPropertiesSource(IterableConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		super(source, filter);
		ConfigurationPropertyName[] filterableNames = getFilterableNames(source);
		if (filterableNames != null) {
			this.filteredNames = new ConfigurationPropertyName[filterableNames.length];
			this.numerOfFilteredNames = 0;
			for (ConfigurationPropertyName name : filterableNames) {
				if (name == null) {
					break;
				}
				if (filter.test(name)) {
					this.filteredNames[this.numerOfFilteredNames++] = name;
				}
			}
		}
	}

	private ConfigurationPropertyName[] getFilterableNames(IterableConfigurationPropertySource source) {
		if (source instanceof SpringIterableConfigurationPropertySource springPropertySource
				&& springPropertySource.isImmutablePropertySource()) {
			return springPropertySource.getConfigurationPropertyNames();
		}
		if (source instanceof FilteredIterableConfigurationPropertiesSource filteredSource) {
			return filteredSource.filteredNames;
		}
		return null;
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		if (this.filteredNames != null) {
			return Arrays.stream(this.filteredNames, 0, this.numerOfFilteredNames);
		}
		return getSource().stream().filter(getFilter());
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		if (this.filteredNames != null) {
			return ConfigurationPropertyState.search(this.filteredNames, 0, this.numerOfFilteredNames,
					name::isAncestorOf);
		}
		return ConfigurationPropertyState.search(this, name::isAncestorOf);
	}

}
