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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Hamcrest {@link Matcher} to help test {@link ConfigurationMetadata}.
 *
 * @author Phillip Webb
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
			ItemMetadata itemMetadata = getFirstPropertyWithName(metadata, this.name);
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
			ItemMetadata property = getFirstPropertyWithName(metadata, this.name);
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

		private ItemMetadata getFirstPropertyWithName(ConfigurationMetadata metadata,
				String name) {
			for (ItemMetadata item : metadata.getItems()) {
				if (item.isOfItemType(this.itemType) && name.equals(item.getName())) {
					return item;
				}
			}
			return null;
		}

	}

}
