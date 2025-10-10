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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.env.PropertySourceInfo;
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

	SpringConfigurationPropertySources(Iterable<PropertySource<?>> sources) {
		Assert.notNull(sources, "'sources' must not be null");
		this.sources = sources;
	}

	boolean isUsingSources(Iterable<PropertySource<?>> sources) {
		return this.sources == sources;
	}

	@Override
	public Iterator<ConfigurationPropertySource> iterator() {
		return new SourcesIterator(this.sources.iterator(), this::adapt);
	}

	private ConfigurationPropertySource adapt(PropertySource<?> source) {
		ConfigurationPropertySource result = this.cache.get(source);
		// Most PropertySources test equality only using the source name, so we need to
		// check the actual source hasn't also changed.
		if (result != null && result.getUnderlyingSource() == source) {
			return result;
		}
		result = SpringConfigurationPropertySource.from(source);
		if (source instanceof PropertySourceInfo propertySourceInfo) {
			result = result.withPrefix(propertySourceInfo.getPrefix());
		}
		this.cache.put(source, result);
		return result;
	}

	private static class SourcesIterator implements Iterator<ConfigurationPropertySource> {

		private final Deque<Iterator<PropertySource<?>>> iterators;

		private @Nullable ConfigurationPropertySource next;

		private final Function<PropertySource<?>, ConfigurationPropertySource> adapter;

		SourcesIterator(Iterator<PropertySource<?>> iterator,
				Function<PropertySource<?>, ConfigurationPropertySource> adapter) {
			this.iterators = new ArrayDeque<>(4);
			this.iterators.push(iterator);
			this.adapter = adapter;
		}

		@Override
		public boolean hasNext() {
			return fetchNext() != null;
		}

		@Override
		public ConfigurationPropertySource next() {
			ConfigurationPropertySource next = fetchNext();
			if (next == null) {
				throw new NoSuchElementException();
			}
			this.next = null;
			return next;
		}

		private @Nullable ConfigurationPropertySource fetchNext() {
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

		private void push(ConfigurableEnvironment environment) {
			this.iterators.push(environment.getPropertySources().iterator());
		}

		private boolean isIgnored(PropertySource<?> candidate) {
			return (candidate instanceof StubPropertySource
					|| candidate instanceof ConfigurationPropertySourcesPropertySource);
		}

	}

}
