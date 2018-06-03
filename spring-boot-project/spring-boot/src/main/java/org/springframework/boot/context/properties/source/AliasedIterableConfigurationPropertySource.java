/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.util.CollectionUtils;

/**
 * A {@link IterableConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedIterableConfigurationPropertySource
		extends AliasedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	AliasedIterableConfigurationPropertySource(IterableConfigurationPropertySource source,
			ConfigurationPropertyNameAliases aliases) {
		super(source, aliases);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().flatMap(this::addAliases);
	}

	private Stream<ConfigurationPropertyName> addAliases(ConfigurationPropertyName name) {
		Stream<ConfigurationPropertyName> names = Stream.of(name);
		List<ConfigurationPropertyName> aliases = getAliases().getAliases(name);
		if (CollectionUtils.isEmpty(aliases)) {
			return names;
		}
		return Stream.concat(names, aliases.stream());
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
