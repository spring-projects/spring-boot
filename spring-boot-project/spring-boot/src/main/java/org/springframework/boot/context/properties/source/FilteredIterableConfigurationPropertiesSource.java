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

package org.springframework.boot.context.properties.source;

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

	/**
     * Constructs a new FilteredIterableConfigurationPropertiesSource with the specified source and filter.
     *
     * @param source the IterableConfigurationPropertySource to be filtered
     * @param filter the Predicate used to filter the ConfigurationPropertyNames
     */
    FilteredIterableConfigurationPropertiesSource(IterableConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		super(source, filter);
	}

	/**
     * Returns a stream of ConfigurationPropertyName objects.
     * 
     * @return a stream of ConfigurationPropertyName objects
     */
    @Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().filter(getFilter());
	}

	/**
     * Returns the source of the configuration property as an IterableConfigurationPropertySource.
     * 
     * @return the source of the configuration property as an IterableConfigurationPropertySource
     */
    @Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

	/**
     * Returns the state of whether this configuration property source contains a descendant of the specified configuration property name.
     * 
     * @param name the configuration property name to check for descendants
     * @return the state of whether this configuration property source contains a descendant of the specified configuration property name
     */
    @Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		return ConfigurationPropertyState.search(this, name::isAncestorOf);
	}

}
