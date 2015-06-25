/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsMapContaining;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Hamcrest {@link Matcher} to help test {@link ConfigurationMetadata}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataMatchers {

	public static ContainsItemMatcher containsGroup(String name) {
		return new ContainsItemMatcher(ItemType.GROUP, name);
	}

	public static ContainsItemMatcher containsGroup(String name, Class<?> type) {
		return new ContainsItemMatcher(ItemType.GROUP, name).ofType(type);
	}

	public static ContainsItemMatcher containsGroup(String name, String type) {
		return new ContainsItemMatcher(ItemType.GROUP, name).ofType(type);
	}

	public static ContainsItemMatcher containsProperty(String name) {
		return new ContainsItemMatcher(ItemType.PROPERTY, name);
	}

	public static ContainsItemMatcher containsProperty(String name, Class<?> type) {
		return new ContainsItemMatcher(ItemType.PROPERTY, name).ofType(type);
	}

	public static ContainsItemMatcher containsProperty(String name, String type) {
		return new ContainsItemMatcher(ItemType.PROPERTY, name).ofType(type);
	}

	public static ContainsHintMatcher containsHint(String name) {
		return new ContainsHintMatcher(name);
	}

	public static class ContainsItemMatcher extends BaseMatcher<ConfigurationMetadata> {

		private final ItemType itemType;

		private final String name;

		private final String type;

		private final Class<?> sourceType;

		private final String description;

		private final Matcher<?> defaultValue;

		private final boolean deprecated;

		public ContainsItemMatcher(ItemType itemType, String name) {
			this(itemType, name, null, null, null, null, false);
		}

		public ContainsItemMatcher(ItemType itemType, String name, String type,
				Class<?> sourceType, String description, Matcher<?> defaultValue,
				boolean deprecated) {
			this.itemType = itemType;
			this.name = name;
			this.type = type;
			this.sourceType = sourceType;
			this.description = description;
			this.defaultValue = defaultValue;
			this.deprecated = deprecated;
		}

		@Override
		public boolean matches(Object item) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			ItemMetadata itemMetadata = getFirstItemWithName(metadata, this.name);
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
			if (this.defaultValue != null
					&& !this.defaultValue.matches(itemMetadata.getDefaultValue())) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(itemMetadata.getDescription())) {
				return false;
			}
			if (this.deprecated != itemMetadata.isDeprecated()) {
				return false;
			}
			return true;
		}

		@Override
		public void describeMismatch(Object item, Description description) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			ItemMetadata property = getFirstItemWithName(metadata, this.name);
			if (property == null) {
				description.appendText("missing "
						+ this.itemType.toString().toLowerCase() + " " + this.name);
			}
			else {
				description.appendText(
						"was " + this.itemType.toString().toLowerCase() + " ")
						.appendValue(property);
			}
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("metadata containing " + this.name);
			if (this.type != null) {
				description.appendText(" dataType ").appendValue(this.type);
			}
			if (this.sourceType != null) {
				description.appendText(" sourceType ").appendValue(this.sourceType);
			}
			if (this.defaultValue != null) {
				description.appendText(" defaultValue ").appendValue(this.defaultValue);
			}
			if (this.description != null) {
				description.appendText(" description ").appendValue(this.description);
			}
			if (this.deprecated) {
				description.appendText(" deprecated ").appendValue(true);
			}
		}

		public ContainsItemMatcher ofType(Class<?> dataType) {
			return new ContainsItemMatcher(this.itemType, this.name, dataType.getName(),
					this.sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public ContainsItemMatcher ofType(String dataType) {
			return new ContainsItemMatcher(this.itemType, this.name, dataType,
					this.sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public ContainsItemMatcher fromSource(Class<?> sourceType) {
			return new ContainsItemMatcher(this.itemType, this.name, this.type,
					sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public ContainsItemMatcher withDescription(String description) {
			return new ContainsItemMatcher(this.itemType, this.name, this.type,
					this.sourceType, description, this.defaultValue, this.deprecated);
		}

		public ContainsItemMatcher withDefaultValue(Matcher<?> defaultValue) {
			return new ContainsItemMatcher(this.itemType, this.name, this.type,
					this.sourceType, this.description, defaultValue, this.deprecated);
		}

		public ContainsItemMatcher withDeprecated() {
			return new ContainsItemMatcher(this.itemType, this.name, this.type,
					this.sourceType, this.description, this.defaultValue, true);
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

	public static class ContainsHintMatcher extends BaseMatcher<ConfigurationMetadata> {

		private final String name;

		private final List<ValueHintMatcher> values;

		private final List<ProviderHintMatcher> providers;

		public ContainsHintMatcher(String name) {
			this(name, new ArrayList<ValueHintMatcher>(),
					new ArrayList<ProviderHintMatcher>());
		}

		public ContainsHintMatcher(String name, List<ValueHintMatcher> values,
				List<ProviderHintMatcher> providers) {
			this.name = name;
			this.values = values;
			this.providers = providers;
		}

		@Override
		public boolean matches(Object item) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			ItemHint itemHint = getFirstHintWithName(metadata, this.name);
			if (itemHint == null) {
				return false;
			}
			if (this.name != null && !this.name.equals(itemHint.getName())) {
				return false;
			}
			for (ValueHintMatcher value : this.values) {
				if (!value.matches(itemHint)) {
					return false;
				}
			}
			for (ProviderHintMatcher provider : this.providers) {
				if (!provider.matches(itemHint)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void describeMismatch(Object item, Description description) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			ItemHint itemHint = getFirstHintWithName(metadata, this.name);
			if (itemHint == null) {
				description.appendText("missing hint " + this.name);
			}
			else {
				description.appendText("was hint ").appendValue(itemHint);
			}
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("hints for " + this.name);
			if (this.values != null) {
				description.appendText(" values ").appendValue(this.values);
			}
			if (this.providers != null) {
				description.appendText(" providers ").appendValue(this.providers);
			}
		}

		public ContainsHintMatcher withValue(int index, Object value, String description) {
			List<ValueHintMatcher> values = new ArrayList<ValueHintMatcher>(this.values);
			values.add(new ValueHintMatcher(index, value, description));
			return new ContainsHintMatcher(this.name, values, this.providers);
		}

		public ContainsHintMatcher withProvider(int index, String provider,
				Map<String, Object> parameters) {
			List<ProviderHintMatcher> providers = new ArrayList<ProviderHintMatcher>(
					this.providers);
			providers.add(new ProviderHintMatcher(index, provider, parameters));
			return new ContainsHintMatcher(this.name, this.values, providers);
		}

		public ContainsHintMatcher withProvider(String provider, String key, Object value) {
			return withProvider(this.providers.size(), provider,
					Collections.singletonMap(key, value));
		}

		public ContainsHintMatcher withProvider(String provider) {
			return withProvider(this.providers.size(), provider, null);
		}

		private ItemHint getFirstHintWithName(ConfigurationMetadata metadata, String name) {
			for (ItemHint hint : metadata.getHints()) {
				if (name.equals(hint.getName())) {
					return hint;
				}
			}
			return null;
		}

	}

	public static class ValueHintMatcher extends BaseMatcher<ItemHint> {

		private final int index;

		private final Object value;

		private final String description;

		public ValueHintMatcher(int index, Object value, String description) {
			this.index = index;
			this.value = value;
			this.description = description;
		}

		@Override
		public boolean matches(Object item) {
			ItemHint hint = (ItemHint) item;
			if (this.index + 1 > hint.getValues().size()) {
				return false;
			}
			ItemHint.ValueHint valueHint = hint.getValues().get(this.index);
			if (this.value != null && !this.value.equals(valueHint.getValue())) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(valueHint.getDescription())) {
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("value hint at index '" + this.index + "'");
			if (this.value != null) {
				description.appendText(" value ").appendValue(this.value);
			}
			if (this.description != null) {
				description.appendText(" description ").appendValue(this.description);
			}
		}

	}

	public static class ProviderHintMatcher extends BaseMatcher<ItemHint> {
		private final int index;
		private final String name;
		private final Map<String, Object> parameters;

		public ProviderHintMatcher(int index, String name, Map<String, Object> parameters) {
			this.index = index;
			this.name = name;
			this.parameters = parameters;
		}

		@Override
		public boolean matches(Object item) {
			ItemHint hint = (ItemHint) item;
			if (this.index + 1 > hint.getProviders().size()) {
				return false;
			}
			ItemHint.ProviderHint providerHint = hint.getProviders().get(this.index);
			if (this.name != null && !this.name.equals(providerHint.getName())) {
				return false;
			}
			if (this.parameters != null) {
				for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
					if (!IsMapContaining.hasEntry(entry.getKey(), entry.getValue())
							.matches(providerHint.getParameters())) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("provider hint ");
			if (this.name != null) {
				description.appendText(" name ").appendValue(this.name);
			}
			if (this.parameters != null) {
				description.appendText(" parameters ").appendValue(this.parameters);
			}
		}

	}

}
