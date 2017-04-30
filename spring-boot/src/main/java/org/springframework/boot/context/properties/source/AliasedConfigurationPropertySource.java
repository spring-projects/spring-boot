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

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A {@link ConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final ConfigurationPropertyNameAliases aliases;

	AliasedConfigurationPropertySource(ConfigurationPropertySource source,
			ConfigurationPropertyNameAliases aliases) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(aliases, "Aliases must not be null");
		this.source = source;
		this.aliases = aliases;
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return StreamSupport.stream(this.source.spliterator(), false)
				.flatMap(this::addAliases);
	}

	private Stream<ConfigurationPropertyName> addAliases(ConfigurationPropertyName name) {
		Stream<ConfigurationPropertyName> names = Stream.of(name);
		List<ConfigurationPropertyName> aliases = this.aliases.getAliases(name);
		if (CollectionUtils.isEmpty(aliases)) {
			return names;
		}
		return Stream.concat(names, aliases.stream());
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		ConfigurationProperty result = this.source.getConfigurationProperty(name);
		if (result == null) {
			ConfigurationPropertyName aliasedName = this.aliases.getNameForAlias(name);
			result = this.source.getConfigurationProperty(aliasedName);
		}
		return result;
	}

}
