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

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.PropertySource;

/**
 * A source of {@link ConfigurationProperty ConfigurationProperties}, usually backed by a
 * Spring {@link PropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 */
public interface ConfigurationPropertySource extends Iterable<ConfigurationPropertyName> {

	/**
	 * Return a single {@link ConfigurationProperty} from the source or {@code null} if no
	 * property can be found.
	 * @param name the name of the property (must not be {@code null})
	 * @return the associated object or {@code null}.
	 */
	ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name);

	/**
	 * Return an iterator for the {@link ConfigurationPropertyName names} managed by this
	 * source. If it is not possible to determine the names an empty iterator may be
	 * returned.
	 * @return an iterator (never {@code null})
	 */
	@Override
	default Iterator<ConfigurationPropertyName> iterator() {
		return stream().iterator();
	}

	/**
	 * Returns a sequential {@code Stream} for the {@link ConfigurationPropertyName names}
	 * managed by this source. If it is not possible to determine the names an
	 * {@link Stream#empty() empty stream} may be returned.
	 * @return a stream of names (never {@code null})
	 */
	Stream<ConfigurationPropertyName> stream();

	/**
	 * Return a filtered variant of this source, containing only names that match the
	 * given {@link Predicate}.
	 * @param filter the filter to apply
	 * @return a filtered {@link ConfigurationPropertySource} instance
	 */
	default ConfigurationPropertySource filter(
			Predicate<ConfigurationPropertyName> filter) {
		return new FilteredConfigurationPropertiesSource(this, filter);
	}

	/**
	 * Return a variant of this source that supports name aliases.
	 * @param aliases a function that returns a stream of aliases for any given name
	 * @return a {@link ConfigurationPropertySource} instance supporting name aliases
	 */
	default ConfigurationPropertySource withAliases(
			ConfigurationPropertyNameAliases aliases) {
		return new AliasedConfigurationPropertySource(this, aliases);
	}

}
