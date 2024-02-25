/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Adapter to convert Spring's {@link MutablePropertySources} to
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 */
class SpringConfigurationPropertySources implements Iterable<ConfigurationPropertySource> {

	private final Iterable<PropertySource<?>> sources;

	private final Map<PropertySource<?>, ConfigurationPropertySource> cache = new ConcurrentReferenceHashMap<>(16,
			ReferenceType.SOFT);

	/**
	 * Constructs a new SpringConfigurationPropertySources object with the given sources.
	 * @param sources the sources of property values to be used for configuration
	 * @throws IllegalArgumentException if the sources parameter is null
	 */
	SpringConfigurationPropertySources(Iterable<PropertySource<?>> sources) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources = sources;
	}

	/**
	 * Checks if the given Iterable of PropertySources is being used by the
	 * SpringConfigurationPropertySources instance.
	 * @param sources the Iterable of PropertySources to check
	 * @return true if the given Iterable of PropertySources is being used, false
	 * otherwise
	 */
	boolean isUsingSources(Iterable<PropertySource<?>> sources) {
		return this.sources == sources;
	}

	/**
	 * Returns an iterator over the configuration property sources in this
	 * SpringConfigurationPropertySources object.
	 * @return an iterator over the configuration property sources
	 */
	@Override
	public Iterator<ConfigurationPropertySource> iterator() {
		return new SourcesIterator(this.sources.iterator(), this::adapt);
	}

	/**
	 * Adapts a PropertySource to a ConfigurationPropertySource.
	 *
	 * This method takes a PropertySource and adapts it to a ConfigurationPropertySource.
	 * It first checks if the adapted source is already present in the cache. If it is, it
	 * returns the cached result. If not, it creates a new ConfigurationPropertySource
	 * using the SpringConfigurationPropertySource.from() method. If the source implements
	 * the OriginLookup interface, it adds the prefix to the result using the withPrefix()
	 * method. Finally, it adds the adapted source to the cache and returns the result.
	 * @param source the PropertySource to be adapted
	 * @return the adapted ConfigurationPropertySource
	 */
	private ConfigurationPropertySource adapt(PropertySource<?> source) {
		ConfigurationPropertySource result = this.cache.get(source);
		// Most PropertySources test equality only using the source name, so we need to
		// check the actual source hasn't also changed.
		if (result != null && result.getUnderlyingSource() == source) {
			return result;
		}
		result = SpringConfigurationPropertySource.from(source);
		if (source instanceof OriginLookup) {
			result = result.withPrefix(((OriginLookup<?>) source).getPrefix());
		}
		this.cache.put(source, result);
		return result;
	}

	/**
	 * SourcesIterator class.
	 */
	private static class SourcesIterator implements Iterator<ConfigurationPropertySource> {

		private final Deque<Iterator<PropertySource<?>>> iterators;

		private ConfigurationPropertySource next;

		private final Function<PropertySource<?>, ConfigurationPropertySource> adapter;

		/**
		 * Constructs a new SourcesIterator with the specified iterator and adapter.
		 * @param iterator the iterator to be used for iterating over property sources
		 * @param adapter the function used to adapt property sources to configuration
		 * property sources
		 */
		SourcesIterator(Iterator<PropertySource<?>> iterator,
				Function<PropertySource<?>, ConfigurationPropertySource> adapter) {
			this.iterators = new ArrayDeque<>(4);
			this.iterators.push(iterator);
			this.adapter = adapter;
		}

		/**
		 * Returns true if there is another element in the iteration, false otherwise.
		 * @return true if there is another element in the iteration, false otherwise
		 */
		@Override
		public boolean hasNext() {
			return fetchNext() != null;
		}

		/**
		 * Retrieves the next ConfigurationPropertySource from the iterator.
		 * @return The next ConfigurationPropertySource.
		 * @throws NoSuchElementException if there are no more elements in the iterator.
		 */
		@Override
		public ConfigurationPropertySource next() {
			ConfigurationPropertySource next = fetchNext();
			if (next == null) {
				throw new NoSuchElementException();
			}
			this.next = null;
			return next;
		}

		/**
		 * Fetches the next ConfigurationPropertySource from the iterator.
		 * @return the next ConfigurationPropertySource, or null if there are no more
		 * sources
		 */
		private ConfigurationPropertySource fetchNext() {
			if (this.next == null) {
				if (this.iterators.isEmpty()) {
					return null;
				}
				if (!this.iterators.peek().hasNext()) {
					this.iterators.pop();
					return fetchNext();
				}
				PropertySource<?> candidate = this.iterators.peek().next();
				if (candidate.getSource() instanceof ConfigurableEnvironment configurableEnvironment) {
					push(configurableEnvironment);
					return fetchNext();
				}
				if (isIgnored(candidate)) {
					return fetchNext();
				}
				this.next = this.adapter.apply(candidate);
			}
			return this.next;
		}

		/**
		 * Pushes the iterator of property sources from the given environment onto the
		 * stack.
		 * @param environment the configurable environment containing the property sources
		 */
		private void push(ConfigurableEnvironment environment) {
			this.iterators.push(environment.getPropertySources().iterator());
		}

		/**
		 * Checks if the given property source is ignored.
		 * @param candidate the property source to check
		 * @return {@code true} if the property source is ignored, {@code false} otherwise
		 */
		private boolean isIgnored(PropertySource<?> candidate) {
			return (candidate instanceof StubPropertySource
					|| candidate instanceof ConfigurationPropertySourcesPropertySource);
		}

	}

}
