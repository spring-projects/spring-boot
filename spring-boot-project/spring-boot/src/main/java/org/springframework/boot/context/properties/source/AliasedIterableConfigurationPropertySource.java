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

import java.util.List;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;

/**
 * A {@link IterableConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedIterableConfigurationPropertySource extends AliasedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	/**
	 * Constructs a new AliasedIterableConfigurationPropertySource with the specified
	 * source and aliases.
	 * @param source the IterableConfigurationPropertySource to be aliased
	 * @param aliases the ConfigurationPropertyNameAliases to be used for aliasing
	 */
	AliasedIterableConfigurationPropertySource(IterableConfigurationPropertySource source,
			ConfigurationPropertyNameAliases aliases) {
		super(source, aliases);
	}

	/**
	 * Returns a stream of ConfigurationPropertyName objects.
	 * @return a stream of ConfigurationPropertyName objects
	 */
	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().flatMap(this::addAliases);
	}

	/**
	 * Adds aliases for the given configuration property name.
	 * @param name the configuration property name
	 * @return a stream of configuration property names including the given name and its
	 * aliases
	 */
	private Stream<ConfigurationPropertyName> addAliases(ConfigurationPropertyName name) {
		Stream<ConfigurationPropertyName> names = Stream.of(name);
		List<ConfigurationPropertyName> aliases = getAliases().getAliases(name);
		if (CollectionUtils.isEmpty(aliases)) {
			return names;
		}
		return Stream.concat(names, aliases.stream());
	}

	/**
	 * Returns the source of this AliasedIterableConfigurationPropertySource.
	 * @return the source of this AliasedIterableConfigurationPropertySource
	 */
	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
