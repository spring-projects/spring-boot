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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.hamcrest.collection.IsMapContaining;

import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;
import org.springframework.util.ObjectUtils;

/**
 * AssertJ {@link Condition} to help test {@link ConfigurationMetadata}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public final class Metadata {

	private Metadata() {
	}

	public static MetadataItemCondition withGroup(String name) {
		return new MetadataItemCondition(ItemType.GROUP, name);
	}

	public static MetadataItemCondition withGroup(String name, Class<?> type) {
		return new MetadataItemCondition(ItemType.GROUP, name).ofType(type);
	}

	public static MetadataItemCondition withGroup(String name, String type) {
		return new MetadataItemCondition(ItemType.GROUP, name).ofType(type);
	}

	public static MetadataItemCondition withProperty(String name) {
		return new MetadataItemCondition(ItemType.PROPERTY, name);
	}

	public static MetadataItemCondition withProperty(String name, Class<?> type) {
		return new MetadataItemCondition(ItemType.PROPERTY, name).ofType(type);
	}

	public static MetadataItemCondition withProperty(String name, String type) {
		return new MetadataItemCondition(ItemType.PROPERTY, name).ofType(type);
	}

	public static Metadata.MetadataItemCondition withEnabledFlag(String key) {
		return withProperty(key).ofType(Boolean.class);
	}

	public static MetadataHintCondition withHint(String name) {
		return new MetadataHintCondition(name);
	}

	public static class MetadataItemCondition extends Condition<ConfigurationMetadata> {

		private final ItemType itemType;

		private final String name;

		private final String type;

		private final Class<?> sourceType;

		private final String sourceMethod;

		private final String description;

		private final Object defaultValue;

		private final ItemDeprecation deprecation;

		public MetadataItemCondition(ItemType itemType, String name) {
			this(itemType, name, null, null, null, null, null, null);
		}

		public MetadataItemCondition(ItemType itemType, String name, String type,
				Class<?> sourceType, String sourceMethod, String description,
				Object defaultValue, ItemDeprecation deprecation) {
			this.itemType = itemType;
			this.name = name;
			this.type = type;
			this.sourceType = sourceType;
			this.sourceMethod = sourceMethod;
			this.description = description;
			this.defaultValue = defaultValue;
			this.deprecation = deprecation;
			describedAs(createDescription());
		}

		private String createDescription() {
			StringBuilder description = new StringBuilder();
			description.append("an item named '" + this.name + "'");
			if (this.type != null) {
				description.append(" with dataType:").append(this.type);
			}
			if (this.sourceType != null) {
				description.append(" with sourceType:").append(this.sourceType);
			}
			if (this.sourceMethod != null) {
				description.append(" with sourceMethod:").append(this.sourceMethod);
			}
			if (this.defaultValue != null) {
				description.append(" with defaultValue:").append(this.defaultValue);
			}
			if (this.description != null) {
				description.append(" with description:").append(this.description);
			}
			if (this.deprecation != null) {
				description.append(" with deprecation:").append(this.deprecation);
			}
			return description.toString();
		}

		@Override
		public boolean matches(ConfigurationMetadata value) {
			ItemMetadata itemMetadata = getFirstItemWithName(value, this.name);
			if (itemMetadata == null) {
				return false;
			}
			if (this.type != null && !this.type.equals(itemMetadata.getType())) {
				return false;
			}
			if (this.sourceType != null
					&& !this.sourceType.getName().equals(itemMetadata.getSourceType())) {
				return false;
			}
			if (this.sourceMethod != null
					&& !this.sourceMethod.equals(itemMetadata.getSourceMethod())) {
				return false;
			}
			if (this.defaultValue != null && !ObjectUtils
					.nullSafeEquals(this.defaultValue, itemMetadata.getDefaultValue())) {
				return false;
			}
			if (this.defaultValue == null && itemMetadata.getDefaultValue() != null) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(itemMetadata.getDescription())) {
				return false;
			}
			if (this.deprecation == null && itemMetadata.getDeprecation() != null) {
				return false;
			}
			if (this.deprecation != null
					&& !this.deprecation.equals(itemMetadata.getDeprecation())) {
				return false;
			}
			return true;
		}

		public MetadataItemCondition ofType(Class<?> dataType) {
			return new MetadataItemCondition(this.itemType, this.name, dataType.getName(),
					this.sourceType, this.sourceMethod, this.description,
					this.defaultValue, this.deprecation);
		}

		public MetadataItemCondition ofType(String dataType) {
			return new MetadataItemCondition(this.itemType, this.name, dataType,
					this.sourceType, this.sourceMethod, this.description,
					this.defaultValue, this.deprecation);
		}

		public MetadataItemCondition fromSource(Class<?> sourceType) {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					sourceType, this.sourceMethod, this.description, this.defaultValue,
					this.deprecation);
		}

		public MetadataItemCondition fromSourceMethod(String sourceMethod) {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					this.sourceType, sourceMethod, this.description, this.defaultValue,
					this.deprecation);
		}

		public MetadataItemCondition withDescription(String description) {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					this.sourceType, this.sourceMethod, description, this.defaultValue,
					this.deprecation);
		}

		public MetadataItemCondition withDefaultValue(Object defaultValue) {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					this.sourceType, this.sourceMethod, this.description, defaultValue,
					this.deprecation);
		}

		public MetadataItemCondition withDeprecation(String reason, String replacement) {
			return withDeprecation(reason, replacement, null);
		}

		public MetadataItemCondition withDeprecation(String reason, String replacement,
				String level) {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					this.sourceType, this.sourceMethod, this.description,
					this.defaultValue, new ItemDeprecation(reason, replacement, level));
		}

		public MetadataItemCondition withNoDeprecation() {
			return new MetadataItemCondition(this.itemType, this.name, this.type,
					this.sourceType, this.sourceMethod, this.description,
					this.defaultValue, null);
		}

		private ItemMetadata getFirstItemWithName(ConfigurationMetadata metadata,
				String name) {
			for (ItemMetadata item : metadata.getItems()) {
				if (item.isOfItemType(this.itemType) && name.equals(item.getName())) {
					return item;
				}
			}
			return null;
		}

	}

	public static class MetadataHintCondition extends Condition<ConfigurationMetadata> {

		private final String name;

		private final List<ItemHintValueCondition> valueConditions;

		private final List<ItemHintProviderCondition> providerConditions;

		public MetadataHintCondition(String name) {
			this.name = name;
			this.valueConditions = Collections.emptyList();
			this.providerConditions = Collections.emptyList();
		}

		public MetadataHintCondition(String name,
				List<ItemHintValueCondition> valueConditions,
				List<ItemHintProviderCondition> providerConditions) {
			this.name = name;
			this.valueConditions = valueConditions;
			this.providerConditions = providerConditions;
			describedAs(createDescription());
		}

		private String createDescription() {
			StringBuilder description = new StringBuilder();
			description.append("a hints name '" + this.name + "'");
			if (!this.valueConditions.isEmpty()) {
				description.append(" with values:").append(this.valueConditions);
			}
			if (!this.providerConditions.isEmpty()) {
				description.append(" with providers:").append(this.providerConditions);
			}
			return description.toString();
		}

		@Override
		public boolean matches(ConfigurationMetadata metadata) {
			ItemHint itemHint = getFirstHintWithName(metadata, this.name);
			if (itemHint == null) {
				return false;
			}
			return matches(itemHint, this.valueConditions)
					&& matches(itemHint, this.providerConditions);
		}

		private boolean matches(ItemHint itemHint,
				List<? extends Condition<ItemHint>> conditions) {
			for (Condition<ItemHint> condition : conditions) {
				if (!condition.matches(itemHint)) {
					return false;
				}
			}
			return true;
		}

		private ItemHint getFirstHintWithName(ConfigurationMetadata metadata,
				String name) {
			for (ItemHint hint : metadata.getHints()) {
				if (name.equals(hint.getName())) {
					return hint;
				}
			}
			return null;
		}

		public MetadataHintCondition withValue(int index, Object value,
				String description) {
			return new MetadataHintCondition(this.name,
					add(this.valueConditions,
							new ItemHintValueCondition(index, value, description)),
					this.providerConditions);
		}

		public MetadataHintCondition withProvider(String provider) {
			return withProvider(this.providerConditions.size(), provider, null);
		}

		public MetadataHintCondition withProvider(String provider, String key,
				Object value) {
			return withProvider(this.providerConditions.size(), provider,
					Collections.singletonMap(key, value));
		}

		public MetadataHintCondition withProvider(int index, String provider,
				Map<String, Object> parameters) {
			return new MetadataHintCondition(this.name, this.valueConditions,
					add(this.providerConditions,
							new ItemHintProviderCondition(index, provider, parameters)));
		}

		private <T> List<T> add(List<T> items, T item) {
			List<T> result = new ArrayList<>(items);
			result.add(item);
			return result;
		}

	}

	private static class ItemHintValueCondition extends Condition<ItemHint> {

		private final int index;

		private final Object value;

		private final String description;

		ItemHintValueCondition(int index, Object value, String description) {
			this.index = index;
			this.value = value;
			this.description = description;
			describedAs(createDescription());
		}

		private String createDescription() {
			StringBuilder description = new StringBuilder();
			description.append("value hint at index '" + this.index + "'");
			if (this.value != null) {
				description.append(" with value:").append(this.value);
			}
			if (this.description != null) {
				description.append(" with description:").append(this.description);
			}
			return description.toString();
		}

		@Override
		public boolean matches(ItemHint value) {
			if (this.index + 1 > value.getValues().size()) {
				return false;
			}
			ItemHint.ValueHint valueHint = value.getValues().get(this.index);
			if (this.value != null && !this.value.equals(valueHint.getValue())) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(valueHint.getDescription())) {
				return false;
			}
			return true;
		}

	}

	private static class ItemHintProviderCondition extends Condition<ItemHint> {

		private final int index;

		private final String name;

		private final Map<String, Object> parameters;

		ItemHintProviderCondition(int index, String name,
				Map<String, Object> parameters) {
			this.index = index;
			this.name = name;
			this.parameters = parameters;
			describedAs(createDescription());
		}

		public String createDescription() {
			StringBuilder description = new StringBuilder();
			description.append("value provider");
			if (this.name != null) {
				description.append(" with name:").append(this.name);
			}
			if (this.parameters != null) {
				description.append(" with parameters:").append(this.parameters);
			}
			return description.toString();
		}

		@Override
		public boolean matches(ItemHint hint) {
			if (this.index + 1 > hint.getProviders().size()) {
				return false;
			}
			ItemHint.ValueProvider valueProvider = hint.getProviders().get(this.index);
			if (this.name != null && !this.name.equals(valueProvider.getName())) {
				return false;
			}
			if (this.parameters != null) {
				for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
					if (!IsMapContaining.hasEntry(entry.getKey(), entry.getValue())
							.matches(valueProvider.getParameters())) {
						return false;
					}
				}
			}
			return true;
		}

	}

}
