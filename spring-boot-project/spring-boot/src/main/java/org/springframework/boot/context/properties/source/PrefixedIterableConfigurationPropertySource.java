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

import org.springframework.util.StringUtils;

/**
 * An iterable {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedIterableConfigurationPropertySource extends PrefixedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	PrefixedIterableConfigurationPropertySource(IterableConfigurationPropertySource source, String prefix) {
		super(source, prefix);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		if (!StringUtils.hasText(getPrefix())) {
			return getSource().stream();
		}
		ConfigurationPropertyName prefix = ConfigurationPropertyName.of(getPrefix());
		return getSource().stream().map((propertyName) -> {
			if (prefix.isAncestorOf(propertyName)) {
				String name = propertyName.toString();
				return ConfigurationPropertyName.of(name.substring(getPrefix().length() + 1));
			}
			return propertyName;
		});
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
