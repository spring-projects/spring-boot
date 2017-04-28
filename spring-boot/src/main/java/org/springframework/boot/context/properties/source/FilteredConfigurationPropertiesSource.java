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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	public Stream<ConfigurationPropertyName> stream() {
		return StreamSupport.stream(this.source.spliterator(), false).filter(this.filter);
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		return (this.filter.test(name) ? this.source.getConfigurationProperty(name)
				: null);
	}

}
