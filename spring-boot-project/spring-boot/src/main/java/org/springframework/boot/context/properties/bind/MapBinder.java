/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;

/**
 * {@link AggregateBinder} for Maps.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapBinder extends AggregateBinder<Map<Object, Object>> {

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

	/**
     * Constructs a new MapBinder with the specified context.
     *
     * @param context the context to be used by the MapBinder
     */
    MapBinder(Context context) {
		super(context);
	}

	/**
     * Determines whether recursive binding is allowed for the given configuration property source.
     * 
     * @param source the configuration property source to check
     * @return true if recursive binding is allowed, false otherwise
     */
    @Override
	protected boolean isAllowRecursiveBinding(ConfigurationPropertySource source) {
		return true;
	}

	/**
     * Binds an aggregate configuration property to a map.
     * 
     * @param name The name of the configuration property.
     * @param target The target bindable object.
     * @param elementBinder The element binder for binding individual elements of the map.
     * @return The bound map object.
     */
    @Override
	protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Map<Object, Object> map = CollectionFactory
			.createMap((target.getValue() != null) ? Map.class : target.getType().resolve(Object.class), 0);
		Bindable<?> resolvedTarget = resolveTarget(target);
		boolean hasDescendants = hasDescendants(name);
		for (ConfigurationPropertySource source : getContext().getSources()) {
			if (!ConfigurationPropertyName.EMPTY.equals(name)) {
				ConfigurationProperty property = source.getConfigurationProperty(name);
				if (property != null && !hasDescendants) {
					getContext().setConfigurationProperty(property);
					Object result = property.getValue();
					result = getContext().getPlaceholdersResolver().resolvePlaceholders(result);
					return getContext().getConverter().convert(result, target);
				}
				source = source.filter(name::isAncestorOf);
			}
			new EntryBinder(name, resolvedTarget, elementBinder).bindEntries(source, map);
		}
		return map.isEmpty() ? null : map;
	}

	/**
     * Checks if the given ConfigurationPropertyName has any descendants in the sources of the current context.
     * 
     * @param name the ConfigurationPropertyName to check for descendants
     * @return true if the ConfigurationPropertyName has descendants, false otherwise
     */
    private boolean hasDescendants(ConfigurationPropertyName name) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	/**
     * Resolves the target bindable object.
     * 
     * @param target the target bindable object to be resolved
     * @return the resolved target bindable object
     */
    private Bindable<?> resolveTarget(Bindable<?> target) {
		Class<?> type = target.getType().resolve(Object.class);
		if (Properties.class.isAssignableFrom(type)) {
			return STRING_STRING_MAP;
		}
		return target;
	}

	/**
     * Merges the existing map with the additional map.
     * 
     * @param existing a supplier that provides the existing map
     * @param additional the additional map to be merged
     * @return the merged map
     */
    @Override
	protected Map<Object, Object> merge(Supplier<Map<Object, Object>> existing, Map<Object, Object> additional) {
		Map<Object, Object> existingMap = getExistingIfPossible(existing);
		if (existingMap == null) {
			return additional;
		}
		try {
			existingMap.putAll(additional);
			return copyIfPossible(existingMap);
		}
		catch (UnsupportedOperationException ex) {
			Map<Object, Object> result = createNewMap(additional.getClass(), existingMap);
			result.putAll(additional);
			return result;
		}
	}

	/**
     * Retrieves an existing map if available, otherwise returns null.
     * 
     * @param existing a supplier function that provides an existing map
     * @return the existing map if available, otherwise null
     */
    private Map<Object, Object> getExistingIfPossible(Supplier<Map<Object, Object>> existing) {
		try {
			return existing.get();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
     * Creates a new map by copying the elements from the given map if possible.
     * If an exception occurs during the copy process, the original map is returned.
     * 
     * @param map the map to be copied
     * @return a new map with the copied elements, or the original map if copying is not possible
     */
    private Map<Object, Object> copyIfPossible(Map<Object, Object> map) {
		try {
			return createNewMap(map.getClass(), map);
		}
		catch (Exception ex) {
			return map;
		}
	}

	/**
     * Creates a new map of the specified class and populates it with the elements from the given map.
     * 
     * @param mapClass the class of the new map to be created
     * @param map the map containing the elements to be added to the new map
     * @return a new map of the specified class with the elements from the given map
     */
    private Map<Object, Object> createNewMap(Class<?> mapClass, Map<Object, Object> map) {
		Map<Object, Object> result = CollectionFactory.createMap(mapClass, map.size());
		result.putAll(map);
		return result;
	}

	/**
     * EntryBinder class.
     */
    private class EntryBinder {

		private final ConfigurationPropertyName root;

		private final AggregateElementBinder elementBinder;

		private final ResolvableType mapType;

		private final ResolvableType keyType;

		private final ResolvableType valueType;

		/**
         * Constructs a new EntryBinder with the specified root configuration property name, target bindable, and element binder.
         * 
         * @param root the root configuration property name
         * @param target the target bindable
         * @param elementBinder the element binder
         */
        EntryBinder(ConfigurationPropertyName root, Bindable<?> target, AggregateElementBinder elementBinder) {
			this.root = root;
			this.elementBinder = elementBinder;
			this.mapType = target.getType().asMap();
			this.keyType = this.mapType.getGeneric(0);
			this.valueType = this.mapType.getGeneric(1);
		}

		/**
         * Binds entries from the given {@link ConfigurationPropertySource} to the provided {@link Map}.
         * 
         * @param source the {@link ConfigurationPropertySource} to bind entries from
         * @param map the {@link Map} to bind entries to
         */
        void bindEntries(ConfigurationPropertySource source, Map<Object, Object> map) {
			if (source instanceof IterableConfigurationPropertySource iterableSource) {
				for (ConfigurationPropertyName name : iterableSource) {
					Bindable<?> valueBindable = getValueBindable(name);
					ConfigurationPropertyName entryName = getEntryName(source, name);
					Object key = getContext().getConverter().convert(getKeyName(entryName), this.keyType);
					map.computeIfAbsent(key, (k) -> this.elementBinder.bind(entryName, valueBindable));
				}
			}
		}

		/**
         * Returns a Bindable object for the given ConfigurationPropertyName.
         * If the given name is not a parent of the root and the value is treated as a nested map,
         * returns a Bindable object of the mapType.
         * Otherwise, returns a Bindable object of the valueType.
         *
         * @param name the ConfigurationPropertyName to get the Bindable object for
         * @return a Bindable object for the given ConfigurationPropertyName
         */
        private Bindable<?> getValueBindable(ConfigurationPropertyName name) {
			if (!this.root.isParentOf(name) && isValueTreatedAsNestedMap()) {
				return Bindable.of(this.mapType);
			}
			return Bindable.of(this.valueType);
		}

		/**
         * Returns the entry name for the given configuration property source and name.
         * 
         * @param source the configuration property source
         * @param name the configuration property name
         * @return the entry name
         */
        private ConfigurationPropertyName getEntryName(ConfigurationPropertySource source,
				ConfigurationPropertyName name) {
			Class<?> resolved = this.valueType.resolve(Object.class);
			if (Collection.class.isAssignableFrom(resolved) || this.valueType.isArray()) {
				return chopNameAtNumericIndex(name);
			}
			if (!this.root.isParentOf(name) && (isValueTreatedAsNestedMap() || !isScalarValue(source, name))) {
				return name.chop(this.root.getNumberOfElements() + 1);
			}
			return name;
		}

		/**
         * Chops the given ConfigurationPropertyName at the first numeric index encountered.
         * 
         * @param name the ConfigurationPropertyName to be chopped
         * @return the chopped ConfigurationPropertyName
         */
        private ConfigurationPropertyName chopNameAtNumericIndex(ConfigurationPropertyName name) {
			int start = this.root.getNumberOfElements() + 1;
			int size = name.getNumberOfElements();
			for (int i = start; i < size; i++) {
				if (name.isNumericIndex(i)) {
					return name.chop(i);
				}
			}
			return name;
		}

		/**
         * Checks if the value is treated as a nested map.
         * 
         * @return true if the value is treated as a nested map, false otherwise
         */
        private boolean isValueTreatedAsNestedMap() {
			return Object.class.equals(this.valueType.resolve(Object.class));
		}

		/**
         * Checks if the given configuration property is a scalar value.
         * A scalar value is defined as a value that is either of a primitive type or a String,
         * or an enum type.
         * 
         * @param source the configuration property source
         * @param name the name of the configuration property
         * @return true if the configuration property is a scalar value, false otherwise
         */
        private boolean isScalarValue(ConfigurationPropertySource source, ConfigurationPropertyName name) {
			Class<?> resolved = this.valueType.resolve(Object.class);
			if (!resolved.getName().startsWith("java.lang") && !resolved.isEnum()) {
				return false;
			}
			ConfigurationProperty property = source.getConfigurationProperty(name);
			if (property == null) {
				return false;
			}
			Object value = property.getValue();
			value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
			return getContext().getConverter().canConvert(value, this.valueType);
		}

		/**
         * Returns the key name for the given ConfigurationPropertyName.
         * 
         * @param name the ConfigurationPropertyName to get the key name for
         * @return the key name as a String
         */
        private String getKeyName(ConfigurationPropertyName name) {
			StringBuilder result = new StringBuilder();
			for (int i = this.root.getNumberOfElements(); i < name.getNumberOfElements(); i++) {
				if (!result.isEmpty()) {
					result.append('.');
				}
				result.append(name.getElement(i, Form.ORIGINAL));
			}
			return result.toString();
		}

	}

}
