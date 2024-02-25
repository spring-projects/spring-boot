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

package org.springframework.boot.configurationprocessor.metadata;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Converter to change meta-data objects into JSON objects.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JsonConverter {

	private static final ItemMetadataComparator ITEM_COMPARATOR = new ItemMetadataComparator();

	/**
	 * Converts the given ConfigurationMetadata object into a JSONArray based on the
	 * specified ItemType.
	 * @param metadata The ConfigurationMetadata object to be converted.
	 * @param itemType The ItemType to filter the items in the ConfigurationMetadata.
	 * @return A JSONArray containing the converted items.
	 * @throws Exception If an error occurs during the conversion process.
	 */
	JSONArray toJsonArray(ConfigurationMetadata metadata, ItemType itemType) throws Exception {
		JSONArray jsonArray = new JSONArray();
		List<ItemMetadata> items = metadata.getItems()
			.stream()
			.filter((item) -> item.isOfItemType(itemType))
			.sorted(ITEM_COMPARATOR)
			.toList();
		for (ItemMetadata item : items) {
			if (item.isOfItemType(itemType)) {
				jsonArray.put(toJsonObject(item));
			}
		}
		return jsonArray;
	}

	/**
	 * Converts a collection of ItemHint objects to a JSONArray.
	 * @param hints the collection of ItemHint objects to convert
	 * @return the JSONArray representation of the collection
	 * @throws Exception if an error occurs during the conversion process
	 */
	JSONArray toJsonArray(Collection<ItemHint> hints) throws Exception {
		JSONArray jsonArray = new JSONArray();
		for (ItemHint hint : hints) {
			jsonArray.put(toJsonObject(hint));
		}
		return jsonArray;
	}

	/**
	 * Converts an ItemMetadata object to a JSONObject.
	 * @param item the ItemMetadata object to convert
	 * @return the converted JSONObject
	 * @throws Exception if an error occurs during conversion
	 */
	JSONObject toJsonObject(ItemMetadata item) throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", item.getName());
		jsonObject.putOpt("type", item.getType());
		jsonObject.putOpt("description", item.getDescription());
		jsonObject.putOpt("sourceType", item.getSourceType());
		jsonObject.putOpt("sourceMethod", item.getSourceMethod());
		Object defaultValue = item.getDefaultValue();
		if (defaultValue != null) {
			putDefaultValue(jsonObject, defaultValue);
		}
		ItemDeprecation deprecation = item.getDeprecation();
		if (deprecation != null) {
			jsonObject.put("deprecated", true); // backward compatibility
			JSONObject deprecationJsonObject = new JSONObject();
			if (deprecation.getLevel() != null) {
				deprecationJsonObject.put("level", deprecation.getLevel());
			}
			if (deprecation.getReason() != null) {
				deprecationJsonObject.put("reason", deprecation.getReason());
			}
			if (deprecation.getReplacement() != null) {
				deprecationJsonObject.put("replacement", deprecation.getReplacement());
			}
			if (deprecation.getSince() != null) {
				deprecationJsonObject.put("since", deprecation.getSince());
			}
			jsonObject.put("deprecation", deprecationJsonObject);
		}
		return jsonObject;
	}

	/**
	 * Converts an ItemHint object to a JSONObject.
	 * @param hint the ItemHint object to convert
	 * @return the converted JSONObject
	 * @throws Exception if an error occurs during conversion
	 */
	private JSONObject toJsonObject(ItemHint hint) throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", hint.getName());
		if (!hint.getValues().isEmpty()) {
			jsonObject.put("values", getItemHintValues(hint));
		}
		if (!hint.getProviders().isEmpty()) {
			jsonObject.put("providers", getItemHintProviders(hint));
		}
		return jsonObject;
	}

	/**
	 * Retrieves the hint values for the given ItemHint object.
	 * @param hint the ItemHint object for which to retrieve the hint values
	 * @return a JSONArray containing the hint values
	 * @throws Exception if an error occurs while retrieving the hint values
	 */
	private JSONArray getItemHintValues(ItemHint hint) throws Exception {
		JSONArray values = new JSONArray();
		for (ItemHint.ValueHint value : hint.getValues()) {
			values.put(getItemHintValue(value));
		}
		return values;
	}

	/**
	 * Retrieves the hint value of an item hint and returns it as a JSONObject.
	 * @param value the value hint to retrieve
	 * @return a JSONObject containing the hint value and description
	 * @throws Exception if an error occurs while retrieving the hint value
	 */
	private JSONObject getItemHintValue(ItemHint.ValueHint value) throws Exception {
		JSONObject result = new JSONObject();
		putHintValue(result, value.getValue());
		result.putOpt("description", value.getDescription());
		return result;
	}

	/**
	 * Retrieves the item hint providers for the given item hint.
	 * @param hint the item hint object
	 * @return the JSON array containing the item hint providers
	 * @throws Exception if an error occurs while retrieving the providers
	 */
	private JSONArray getItemHintProviders(ItemHint hint) throws Exception {
		JSONArray providers = new JSONArray();
		for (ItemHint.ValueProvider provider : hint.getProviders()) {
			providers.put(getItemHintProvider(provider));
		}
		return providers;
	}

	/**
	 * Retrieves the item hint provider as a JSONObject.
	 * @param provider the item hint value provider
	 * @return the item hint provider as a JSONObject
	 * @throws Exception if an error occurs while retrieving the item hint provider
	 */
	private JSONObject getItemHintProvider(ItemHint.ValueProvider provider) throws Exception {
		JSONObject result = new JSONObject();
		result.put("name", provider.getName());
		if (provider.getParameters() != null && !provider.getParameters().isEmpty()) {
			JSONObject parameters = new JSONObject();
			for (Map.Entry<String, Object> entry : provider.getParameters().entrySet()) {
				parameters.put(entry.getKey(), extractItemValue(entry.getValue()));
			}
			result.put("parameters", parameters);
		}
		return result;
	}

	/**
	 * Puts the hint value into the given JSONObject.
	 * @param jsonObject the JSONObject to put the hint value into
	 * @param value the value to extract the hint value from
	 * @throws Exception if an error occurs while extracting the hint value
	 */
	private void putHintValue(JSONObject jsonObject, Object value) throws Exception {
		Object hintValue = extractItemValue(value);
		jsonObject.put("value", hintValue);
	}

	/**
	 * Sets the default value for a JSON object.
	 * @param jsonObject the JSON object to set the default value for
	 * @param value the value to extract the default value from
	 * @throws Exception if an error occurs while extracting the default value
	 */
	private void putDefaultValue(JSONObject jsonObject, Object value) throws Exception {
		Object defaultValue = extractItemValue(value);
		jsonObject.put("defaultValue", defaultValue);
	}

	/**
	 * Extracts the value of an item from a JSON object or array.
	 * @param value the value to be extracted
	 * @return the extracted value
	 */
	private Object extractItemValue(Object value) {
		Object defaultValue = value;
		if (value.getClass().isArray()) {
			JSONArray array = new JSONArray();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				array.put(Array.get(value, i));
			}
			defaultValue = array;

		}
		return defaultValue;
	}

	/**
	 * ItemMetadataComparator class.
	 */
	private static final class ItemMetadataComparator implements Comparator<ItemMetadata> {

		private static final Comparator<ItemMetadata> GROUP = Comparator.comparing(ItemMetadata::getName)
			.thenComparing(ItemMetadata::getSourceType, Comparator.nullsFirst(Comparator.naturalOrder()));

		private static final Comparator<ItemMetadata> ITEM = Comparator.comparing(ItemMetadataComparator::isDeprecated)
			.thenComparing(ItemMetadata::getName)
			.thenComparing(ItemMetadata::getSourceType, Comparator.nullsFirst(Comparator.naturalOrder()));

		/**
		 * Compares two ItemMetadata objects based on their item type. If the first object
		 * is of type GROUP, it compares them using the GROUP comparator. Otherwise, it
		 * compares them using the ITEM comparator.
		 * @param o1 the first ItemMetadata object to be compared
		 * @param o2 the second ItemMetadata object to be compared
		 * @return a negative integer, zero, or a positive integer as the first object is
		 * less than, equal to, or greater than the second object
		 */
		@Override
		public int compare(ItemMetadata o1, ItemMetadata o2) {
			if (o1.isOfItemType(ItemType.GROUP)) {
				return GROUP.compare(o1, o2);
			}
			return ITEM.compare(o1, o2);
		}

		/**
		 * Checks if the given item is deprecated.
		 * @param item the item metadata to check
		 * @return true if the item is deprecated, false otherwise
		 */
		private static boolean isDeprecated(ItemMetadata item) {
			return item.getDeprecation() != null;
		}

	}

}
