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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

/**
 * A {@link PropertySources} decorator that filters property sources by name.
 *
 * @author Andy Wilkinson
 */
final class FilteredPropertySources implements PropertySources {

	private final PropertySources delegate;

	private final Set<String> filtered;

	FilteredPropertySources(PropertySources delegate, String... filtered) {
		this.delegate = delegate;
		this.filtered = new HashSet<>(Arrays.asList(filtered));
	}

	@Override
	public boolean contains(String name) {
		if (included(name)) {
			return this.delegate.contains(name);
		}
		return false;
	}

	@Override
	public PropertySource<?> get(String name) {
		if (included(name)) {
			return this.delegate.get(name);
		}
		return null;
	}

	@Override
	public Iterator<PropertySource<?>> iterator() {
		return StreamSupport.stream(this.delegate.spliterator(), false)
				.filter(this::included).iterator();
	}

	private boolean included(PropertySource<?> propertySource) {
		return included(propertySource.getName());
	}

	private boolean included(String name) {
		return (!this.filtered.contains(name));
	}

}
