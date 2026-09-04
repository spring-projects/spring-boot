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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.util.StringUtils;

/**
 * A {@link ConfigurationPropertySource} with a fully {@link Iterable} set of entries.
 * Implementations of this interface <strong>must</strong> be able to iterate over all
 * contained configuration properties. Any {@code non-null} result from
 * {@link #getConfigurationProperty(ConfigurationPropertyName)} must also have an
 * equivalent entry in the {@link #iterator() iterator}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Tommy Karlsson
 * @since 2.0.0
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 * @see #iterator()
 * @see #stream()
 */
public interface IterableConfigurationPropertySource
		extends ConfigurationPropertySource, Iterable<ConfigurationPropertyName> {

	/**
	 * Return an iterator for the {@link ConfigurationPropertyName names} managed by this
	 * source.
	 * @return an iterator (never {@code null})
	 */
	@Override
	default Iterator<ConfigurationPropertyName> iterator() {
		return stream().iterator();
	}

	/**
	 * Returns a sequential {@code Stream} for the {@link ConfigurationPropertyName names}
	 * managed by this source.
	 * @return a stream of names (never {@code null})
	 */
	Stream<ConfigurationPropertyName> stream();

	@Override
	default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		return ConfigurationPropertyState.search(this, name::isAncestorOf);
	}

	/**
	 * Return the names directly beneath the given name. For example, if this source
	 * contains {@code foo.bar} and {@code foo.baz[0]}, the children of {@code foo} are
	 * {@code foo.bar} and {@code foo.baz}.
	 * <p>
	 * A returned name is not necessarily a name that this source has a value for. Given
	 * only {@code foo.bar.baz}, the children of {@code foo} are {@code foo.bar}, for
	 * which {@link #getConfigurationProperty(ConfigurationPropertyName)} returns
	 * {@code null}.
	 * <p>
	 * Implementations that can answer without inspecting every name they contain should
	 * override this method, since callers may ask about many names in turn.
	 * @param name the name whose children should be returned
	 * @return the child names (never {@code null})
	 * @since 4.2.0
	 */
	default Set<ConfigurationPropertyName> getChildrenOf(ConfigurationPropertyName name) {
		Set<ConfigurationPropertyName> children = new LinkedHashSet<>();
		int childElements = name.getNumberOfElements() + 1;
		for (ConfigurationPropertyName candidate : this.filter(name::isAncestorOf)) {
			children.add(candidate.chop(childElements));
		}
		return children;
	}

	@Override
	default IterableConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
		return new FilteredIterableConfigurationPropertiesSource(this, filter);
	}

	@Override
	default IterableConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
		return new AliasedIterableConfigurationPropertySource(this, aliases);
	}

	@Override
	default IterableConfigurationPropertySource withPrefix(@Nullable String prefix) {
		return (StringUtils.hasText(prefix)) ? new PrefixedIterableConfigurationPropertySource(this, prefix) : this;
	}

}
