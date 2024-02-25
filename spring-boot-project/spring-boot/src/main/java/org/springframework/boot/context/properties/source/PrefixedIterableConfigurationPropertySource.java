/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.stream.Stream;

/**
 * An iterable {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedIterableConfigurationPropertySource extends PrefixedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	/**
     * Constructs a new PrefixedIterableConfigurationPropertySource with the specified source and prefix.
     *
     * @param source the IterableConfigurationPropertySource to be wrapped
     * @param prefix the prefix to be applied to the property names
     */
    PrefixedIterableConfigurationPropertySource(IterableConfigurationPropertySource source, String prefix) {
		super(source, prefix);
	}

	/**
     * Returns a stream of ConfigurationPropertyName objects.
     * 
     * @return a stream of ConfigurationPropertyName objects
     */
    @Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().map(this::stripPrefix);
	}

	/**
     * Strips the prefix from the given configuration property name.
     * 
     * @param name the configuration property name to strip the prefix from
     * @return the configuration property name without the prefix
     */
    private ConfigurationPropertyName stripPrefix(ConfigurationPropertyName name) {
		return (getPrefix().isAncestorOf(name)) ? name.subName(getPrefix().getNumberOfElements()) : name;
	}

	/**
     * Returns the source of the configuration property.
     * 
     * @return the source of the configuration property
     */
    @Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
