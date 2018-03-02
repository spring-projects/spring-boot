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

package org.springframework.boot.context.properties;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

/**
 * A composite {@link PropertySources} backed by one or more {@code PropertySources}.
 * Changes to the backing {@code PropertySources} are automatically reflected in the
 * composite.
 *
 * @author Andy Wilkinson
 */
final class CompositePropertySources implements PropertySources {

	private final List<PropertySources> propertySources;

	CompositePropertySources(PropertySources... propertySources) {
		this.propertySources = Arrays.asList(propertySources);
	}

	@Override
	public Iterator<PropertySource<?>> iterator() {
		return this.propertySources.stream()
				.flatMap((sources) -> StreamSupport.stream(sources.spliterator(), false))
				.collect(Collectors.toList()).iterator();
	}

	@Override
	public boolean contains(String name) {
		for (PropertySources sources : this.propertySources) {
			if (sources.contains(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PropertySource<?> get(String name) {
		for (PropertySources sources : this.propertySources) {
			PropertySource<?> source = sources.get(name);
			if (source != null) {
				return source;
			}
		}
		return null;
	}

}
