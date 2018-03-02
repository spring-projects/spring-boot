/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * {@link AggregateBinder} for Maps.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapBinder extends AggregateBinder<Map<Object, Object>> {

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable
			.mapOf(String.class, String.class);

	MapBinder(Context context) {
		super(context);
	}

	@Override
	protected boolean isAllowRecursiveBinding(ConfigurationPropertySource source) {
		return true;
	}

	@Override
	protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Map<Object, Object> map = CollectionFactory.createMap((target.getValue() == null
				? target.getType().resolve(Object.class) : Map.class), 0);
		Bindable<?> resolvedTarget = resolveTarget(target);
		boolean hasDescendants = getContext().streamSources().anyMatch((source) -> source
				.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT);
		for (ConfigurationPropertySource source : getContext().getSources()) {
			if (!ConfigurationPropertyName.EMPTY.equals(name)) {
				ConfigurationProperty property = source.getConfigurationProperty(name);
				if (property != null && !hasDescendants) {
					return getContext().getConverter().convert(property.getValue(),
							target);
				}
				source = source.filter(name::isAncestorOf);
			}
			new EntryBinder(name, resolvedTarget, elementBinder).bindEntries(source, map);
		}
		return (map.isEmpty() ? null : map);
	}

	private Bindable<?> resolveTarget(Bindable<?> target) {
		Class<?> type = target.getType().resolve(Object.class);
		if (Properties.class.isAssignableFrom(type)) {
			return STRING_STRING_MAP;
		}
		return target;
	}

	@Override
	protected Map<Object, Object> merge(Map<Object, Object> existing,
			Map<Object, Object> additional) {
		existing.putAll(additional);
		return existing;
	}

	private class EntryBinder {

		private final ConfigurationPropertyName root;

		private final AggregateElementBinder elementBinder;

		private final ResolvableType mapType;

		private final ResolvableType keyType;

		private final ResolvableType valueType;

		EntryBinder(ConfigurationPropertyName root, Bindable<?> target,
				AggregateElementBinder elementBinder) {
			this.root = root;
			this.elementBinder = elementBinder;
			this.mapType = target.getType().asMap();
			this.keyType = this.mapType.getGeneric(0);
			this.valueType = this.mapType.getGeneric(1);
		}

		public void bindEntries(ConfigurationPropertySource source,
				Map<Object, Object> map) {
			if (source instanceof IterableConfigurationPropertySource) {
				for (ConfigurationPropertyName name : (IterableConfigurationPropertySource) source) {
					Bindable<?> valueBindable = getValueBindable(name);
					ConfigurationPropertyName entryName = getEntryName(source, name);
					Object key = getContext().getConverter()
							.convert(getKeyName(entryName), this.keyType);
					map.computeIfAbsent(key,
							(k) -> this.elementBinder.bind(entryName, valueBindable));
				}
			}
		}

		private Bindable<?> getValueBindable(ConfigurationPropertyName name) {
			if (!this.root.isParentOf(name) && isValueTreatedAsNestedMap()) {
				return Bindable.of(this.mapType);
			}
			return Bindable.of(this.valueType);
		}

		private ConfigurationPropertyName getEntryName(ConfigurationPropertySource source,
				ConfigurationPropertyName name) {
			Class<?> resolved = this.valueType.resolve(Object.class);
			if (Collection.class.isAssignableFrom(resolved) || this.valueType.isArray()) {
				return chopNameAtNumericIndex(name);
			}
			if (!this.root.isParentOf(name)
					&& (isValueTreatedAsNestedMap() || !isScalarValue(source, name))) {
				return name.chop(this.root.getNumberOfElements() + 1);
			}
			return name;
		}

		private ConfigurationPropertyName chopNameAtNumericIndex(
				ConfigurationPropertyName name) {
			int start = this.root.getNumberOfElements() + 1;
			int size = name.getNumberOfElements();
			for (int i = start; i < size; i++) {
				if (name.isNumericIndex(i)) {
					return name.chop(i);
				}
			}
			return name;
		}

		private boolean isValueTreatedAsNestedMap() {
			return Object.class.equals(this.valueType.resolve(Object.class));
		}

		private boolean isScalarValue(ConfigurationPropertySource source,
				ConfigurationPropertyName name) {
			Class<?> resolved = this.valueType.resolve(Object.class);
			String packageName = ClassUtils.getPackageName(resolved);
			if (!packageName.startsWith("java.lang") && !resolved.isEnum()) {
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

		private String getKeyName(ConfigurationPropertyName name) {
			StringBuilder result = new StringBuilder();
			for (int i = this.root.getNumberOfElements(); i < name
					.getNumberOfElements(); i++) {
				result.append(result.length() == 0 ? "" : ".");
				result.append(name.getElement(i, Form.ORIGINAL));
			}
			return result.toString();
		}

	}

}
