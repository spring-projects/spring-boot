/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provide hints on an {@link ItemMetadata}. Defines the list of possible values for a
 * particular item as {@link ItemHint.ValueHint} instances.
 * <p>
 * The {@code name} of the hint is the name of the related property with one major
 * exception for map types as both the keys and values of the map can have hints. In such
 * a case, the hint should be suffixed by ".keys" or ".values" respectively. Creating a
 * hint for a map using its property name is therefore invalid.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ItemHint implements Comparable<ItemHint> {

	private final String name;

	private final List<ValueHint> values;

	private final List<ValueProvider> providers;

	public ItemHint(String name, List<ValueHint> values, List<ValueProvider> providers) {
		this.name = toCanonicalName(name);
		this.values = (values != null) ? new ArrayList<>(values) : new ArrayList<>();
		this.providers = (providers != null) ? new ArrayList<>(providers)
				: new ArrayList<>();
	}

	private String toCanonicalName(String name) {
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			String prefix = name.substring(0, dot);
			String originalName = name.substring(dot);
			return prefix + ConfigurationMetadata.toDashedCase(originalName);
		}
		return ConfigurationMetadata.toDashedCase(name);
	}

	public String getName() {
		return this.name;
	}

	public List<ValueHint> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	public List<ValueProvider> getProviders() {
		return Collections.unmodifiableList(this.providers);
	}

	@Override
	public int compareTo(ItemHint other) {
		return getName().compareTo(other.getName());
	}

	public static ItemHint newHint(String name, ValueHint... values) {
		return new ItemHint(name, Arrays.asList(values), Collections.emptyList());
	}

	@Override
	public String toString() {
		return "ItemHint{" + "name='" + this.name + "', values=" + this.values
				+ ", providers=" + this.providers + '}';
	}

	/**
	 * A hint for a value.
	 */
	public static class ValueHint {

		private final Object value;

		private final String description;

		public ValueHint(Object value, String description) {
			this.value = value;
			this.description = description;
		}

		public Object getValue() {
			return this.value;
		}

		public String getDescription() {
			return this.description;
		}

		@Override
		public String toString() {
			return "ValueHint{" + "value=" + this.value + ", description='"
					+ this.description + '\'' + '}';
		}

	}

	/**
	 * A value provider.
	 */
	public static class ValueProvider {

		private final String name;

		private final Map<String, Object> parameters;

		public ValueProvider(String name, Map<String, Object> parameters) {
			this.name = name;
			this.parameters = parameters;
		}

		public String getName() {
			return this.name;
		}

		public Map<String, Object> getParameters() {
			return this.parameters;
		}

		@Override
		public String toString() {
			return "ValueProvider{" + "name='" + this.name + "', parameters="
					+ this.parameters + '}';
		}

	}

}
