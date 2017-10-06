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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Adapter to convert Spring's {@link MutablePropertySources} to
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 */
class SpringConfigurationPropertySources
		implements Iterable<ConfigurationPropertySource> {

	private final MutablePropertySources sources;

	private volatile PropertySourcesKey lastKey;

	private volatile List<ConfigurationPropertySource> adaptedSources;

	SpringConfigurationPropertySources(MutablePropertySources sources) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources = sources;
	}

	@Override
	public Iterator<ConfigurationPropertySource> iterator() {
		checkForChanges();
		return this.adaptedSources.iterator();
	}

	private void checkForChanges() {
		PropertySourcesKey lastKey = this.lastKey;
		PropertySourcesKey currentKey = new PropertySourcesKey(this.sources);
		if (!currentKey.equals(lastKey)) {
			onChange(this.sources);
			this.lastKey = currentKey;
		}
	}

	private void onChange(MutablePropertySources sources) {
		this.adaptedSources = streamPropertySources(sources)
				.map(SpringConfigurationPropertySource::from)
				.collect(Collectors.toList());
	}

	private Stream<PropertySource<?>> streamPropertySources(
			Iterable<PropertySource<?>> sources) {
		return StreamSupport.stream(sources.spliterator(), false).flatMap(this::flatten)
				.filter(this::isIncluded);
	}

	private Stream<PropertySource<?>> flatten(PropertySource<?> source) {
		if (source.getSource() instanceof ConfigurableEnvironment) {
			return streamPropertySources(
					((ConfigurableEnvironment) source.getSource()).getPropertySources());
		}
		return Stream.of(source);
	}

	private boolean isIncluded(PropertySource<?> source) {
		return !(source instanceof StubPropertySource)
				&& !(source instanceof ConfigurationPropertySourcesPropertySource);
	}

	private static class PropertySourcesKey {

		private final List<PropertySourceKey> keys = new ArrayList<>();

		PropertySourcesKey(MutablePropertySources sources) {
			sources.forEach(this::addKey);
		}

		private void addKey(PropertySource<?> source) {
			this.keys.add(new PropertySourceKey(source));
		}

		@Override
		public int hashCode() {
			return this.keys.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.keys.equals(((PropertySourcesKey) obj).keys);
		}

	}

	private static class PropertySourceKey {

		private final String name;

		private final Class<?> type;

		PropertySourceKey(PropertySource<?> source) {
			this.name = source.getName();
			this.type = source.getClass();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
			result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			PropertySourceKey other = (PropertySourceKey) obj;
			boolean result = true;
			result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
			result = result && ObjectUtils.nullSafeEquals(this.type, other.type);
			return result;
		}

	}

}
