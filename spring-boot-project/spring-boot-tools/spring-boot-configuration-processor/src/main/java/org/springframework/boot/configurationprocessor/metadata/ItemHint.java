/*
 * Copyright 2012-2019 the original author or authors.
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

	/**
	 * Constructs a new ItemHint with the specified name, values, and providers.
	 * @param name the name of the ItemHint
	 * @param values the list of ValueHints associated with the ItemHint
	 * @param providers the list of ValueProviders associated with the ItemHint
	 */
	public ItemHint(String name, List<ValueHint> values, List<ValueProvider> providers) {
		this.name = toCanonicalName(name);
		this.values = (values != null) ? new ArrayList<>(values) : new ArrayList<>();
		this.providers = (providers != null) ? new ArrayList<>(providers) : new ArrayList<>();
	}

	/**
	 * Converts the given name to its canonical form.
	 * @param name the name to be converted
	 * @return the canonical form of the name
	 */
	private String toCanonicalName(String name) {
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			String prefix = name.substring(0, dot);
			String originalName = name.substring(dot);
			return prefix + ConfigurationMetadata.toDashedCase(originalName);
		}
		return ConfigurationMetadata.toDashedCase(name);
	}

	/**
	 * Returns the name of the item.
	 * @return the name of the item
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns an unmodifiable list of ValueHint objects.
	 * @return the list of ValueHint objects
	 */
	public List<ValueHint> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	/**
	 * Returns an unmodifiable list of ValueProvider objects.
	 * @return the list of ValueProvider objects
	 */
	public List<ValueProvider> getProviders() {
		return Collections.unmodifiableList(this.providers);
	}

	/**
	 * Compares this ItemHint object with the specified ItemHint object for order based on
	 * their names.
	 * @param other the ItemHint object to be compared
	 * @return a negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object
	 * @throws NullPointerException if the specified object is null
	 */
	@Override
	public int compareTo(ItemHint other) {
		return getName().compareTo(other.getName());
	}

	/**
	 * Creates a new ItemHint with the given name and values.
	 * @param name the name of the hint
	 * @param values the values associated with the hint
	 * @return a new ItemHint object
	 */
	public static ItemHint newHint(String name, ValueHint... values) {
		return new ItemHint(name, Arrays.asList(values), Collections.emptyList());
	}

	/**
	 * Returns a string representation of the ItemHint object.
	 * @return a string representation of the ItemHint object
	 */
	@Override
	public String toString() {
		return "ItemHint{name='" + this.name + "', values=" + this.values + ", providers=" + this.providers + '}';
	}

	/**
	 * A hint for a value.
	 */
	public static class ValueHint {

		private final Object value;

		private final String description;

		/**
		 * Constructs a new ValueHint object with the specified value and description.
		 * @param value the value associated with the hint
		 * @param description the description of the hint
		 */
		public ValueHint(Object value, String description) {
			this.value = value;
			this.description = description;
		}

		/**
		 * Returns the value of the object.
		 * @return the value of the object
		 */
		public Object getValue() {
			return this.value;
		}

		/**
		 * Returns the description of the ValueHint.
		 * @return the description of the ValueHint
		 */
		public String getDescription() {
			return this.description;
		}

		/**
		 * Returns a string representation of the ValueHint object.
		 * @return a string representation of the ValueHint object
		 */
		@Override
		public String toString() {
			return "ValueHint{value=" + this.value + ", description='" + this.description + '\'' + '}';
		}

	}

	/**
	 * A value provider.
	 */
	public static class ValueProvider {

		private final String name;

		private final Map<String, Object> parameters;

		/**
		 * Constructs a new ValueProvider with the specified name and parameters.
		 * @param name the name of the ValueProvider
		 * @param parameters the parameters for the ValueProvider
		 */
		public ValueProvider(String name, Map<String, Object> parameters) {
			this.name = name;
			this.parameters = parameters;
		}

		/**
		 * Returns the name of the ValueProvider.
		 * @return the name of the ValueProvider
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the parameters of the ValueProvider.
		 * @return the parameters of the ValueProvider
		 */
		public Map<String, Object> getParameters() {
			return this.parameters;
		}

		/**
		 * Returns a string representation of the ValueProvider object.
		 * @return a string representation of the ValueProvider object
		 */
		@Override
		public String toString() {
			return "ValueProvider{name='" + this.name + "', parameters=" + this.parameters + '}';
		}

	}

}
