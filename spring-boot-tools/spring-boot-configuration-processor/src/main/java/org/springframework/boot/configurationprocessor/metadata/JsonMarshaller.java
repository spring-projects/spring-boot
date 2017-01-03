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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Marshaller to write {@link ConfigurationMetadata} as JSON.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
public class JsonMarshaller {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final int BUFFER_SIZE = 4098;

	public void write(ConfigurationMetadata metadata, OutputStream outputStream)
			throws IOException {
		JSONObject object = new JSONOrderedObject();
		object.put("groups", toJsonArray(metadata, ItemType.GROUP));
		object.put("properties", toJsonArray(metadata, ItemType.PROPERTY));
		object.put("hints", toJsonArray(metadata.getHints()));
		outputStream.write(object.toString().getBytes(UTF_8));
	}

	private JSONArray toJsonArray(ConfigurationMetadata metadata, ItemType itemType) {
		JSONArray jsonArray = new JSONArray();
		for (ItemMetadata item : metadata.getItems()) {
			if (item.isOfItemType(itemType)) {
				jsonArray.add(toJsonObject(item));
			}
		}
		return jsonArray;
	}

	private JSONArray toJsonArray(Collection<ItemHint> hints) {
		JSONArray jsonArray = new JSONArray();
		for (ItemHint hint : hints) {
			jsonArray.add(toJsonObject(hint));
		}
		return jsonArray;
	}

	private JSONObject toJsonObject(ItemMetadata item) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", item.getName());
		putIfPresent(jsonObject, "type", item.getType());
		putIfPresent(jsonObject, "description", item.getDescription());
		putIfPresent(jsonObject, "sourceType", item.getSourceType());
		putIfPresent(jsonObject, "sourceMethod", item.getSourceMethod());
		Object defaultValue = item.getDefaultValue();
		if (defaultValue != null) {
			putDefaultValue(jsonObject, defaultValue);
		}
		ItemDeprecation deprecation = item.getDeprecation();
		if (deprecation != null) {
			jsonObject.put("deprecated", true); // backward compatibility
			JSONObject deprecationJsonObject = new JSONObject();
			if (deprecation.getReason() != null) {
				deprecationJsonObject.put("reason", deprecation.getReason());
			}
			if (deprecation.getReplacement() != null) {
				deprecationJsonObject.put("replacement", deprecation.getReplacement());
			}
			jsonObject.put("deprecation", deprecationJsonObject);
		}
		return jsonObject;
	}

	private JSONObject toJsonObject(ItemHint hint) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", hint.getName());
		if (!hint.getValues().isEmpty()) {
			jsonObject.put("values", getItemHintValues(hint));
		}
		if (!hint.getProviders().isEmpty()) {
			jsonObject.put("providers", getItemHintProviders(hint));
		}
		return jsonObject;
	}

	private JSONArray getItemHintValues(ItemHint hint) {
		JSONArray values = new JSONArray();
		for (ItemHint.ValueHint value : hint.getValues()) {
			values.add(getItemHintValue(value));
		}
		return values;
	}

	private JSONObject getItemHintValue(ItemHint.ValueHint value) {
		JSONObject result = new JSONOrderedObject();
		putHintValue(result, value.getValue());
		putIfPresent(result, "description", value.getDescription());
		return result;
	}

	private JSONArray getItemHintProviders(ItemHint hint) {
		JSONArray providers = new JSONArray();
		for (ItemHint.ValueProvider provider : hint.getProviders()) {
			providers.add(getItemHintProvider(provider));
		}
		return providers;
	}

	private JSONObject getItemHintProvider(ItemHint.ValueProvider provider) {
		JSONObject result = new JSONOrderedObject();
		result.put("name", provider.getName());
		if (provider.getParameters() != null && !provider.getParameters().isEmpty()) {
			JSONObject parameters = new JSONOrderedObject();
			for (Map.Entry<String, Object> entry : provider.getParameters().entrySet()) {
				parameters.put(entry.getKey(), extractItemValue(entry.getValue()));
			}
			result.put("parameters", parameters);
		}
		return result;
	}

	private void putIfPresent(JSONObject jsonObject, String name, Object value) {
		if (value != null) {
			jsonObject.put(name, value);
		}
	}

	private void putHintValue(JSONObject jsonObject, Object value) {
		Object hintValue = extractItemValue(value);
		jsonObject.put("value", hintValue);
	}

	private void putDefaultValue(JSONObject jsonObject, Object value) {
		Object defaultValue = extractItemValue(value);
		jsonObject.put("defaultValue", defaultValue);
	}

	private Object extractItemValue(Object value) {
		Object defaultValue = value;
		if (value.getClass().isArray()) {
			JSONArray array = new JSONArray();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				array.add(Array.get(value, i));
			}
			defaultValue = array;

		}
		return defaultValue;
	}

	public ConfigurationMetadata read(InputStream inputStream) throws IOException, ParseException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = parseJson(inputStream);
		JSONArray groups = getOrNull(object, "groups");
		if (groups != null) {
			for (int i = 0; i < groups.size(); i++) {
				metadata.add(toItemMetadata((JSONObject) groups.get(i), ItemType.GROUP));
			}
		}
		JSONArray properties = getOrNull(object, "properties");
		if (properties != null) {
			for (int i = 0; i < properties.size(); i++) {
				metadata.add(toItemMetadata((JSONObject) properties.get(i),
						ItemType.PROPERTY));
			}
		}
		JSONArray hints = getOrNull(object, "hints");
		if (hints != null) {
			for (int i = 0; i < hints.size(); i++) {
				metadata.add(toItemHint((JSONObject) hints.get(i)));
			}
		}
		return metadata;
	}

	private ItemMetadata toItemMetadata(JSONObject object, ItemType itemType) {
		String name = getMustExist(object, "name");
		String type = getOrNull(object, "type");
		String description = getOrNull(object, "description");
		String sourceType = getOrNull(object, "sourceType");
		String sourceMethod = getOrNull(object, "sourceMethod");
		Object defaultValue = readItemValue(getOrNull(object, "defaultValue"));
		ItemDeprecation deprecation = toItemDeprecation(object);
		return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod,
				description, defaultValue, deprecation);
	}

	private ItemDeprecation toItemDeprecation(JSONObject object) {
		if (object.containsKey("deprecation")) {
			JSONObject deprecationJsonObject = getMustExist(object, "deprecation");
			ItemDeprecation deprecation = new ItemDeprecation();
			deprecation.setReason(getOrNull(deprecationJsonObject, "reason"));
			deprecation
					.setReplacement(getOrNull(deprecationJsonObject, "replacement"));
			return deprecation;
		}
		final Boolean deprecated = getOrNull(object, "deprecated");
		return (deprecated != null && deprecated) ? new ItemDeprecation() : null;
	}

	private ItemHint toItemHint(JSONObject object) {
		String name = getMustExist(object, "name");
		List<ItemHint.ValueHint> values = new ArrayList<ItemHint.ValueHint>();
		if (object.containsKey("values")) {
			JSONArray valuesArray = getMustExist(object, "values");
			for (int i = 0; i < valuesArray.size(); i++) {
				values.add(toValueHint((JSONObject) valuesArray.get(i)));
			}
		}
		List<ItemHint.ValueProvider> providers = new ArrayList<ItemHint.ValueProvider>();
		if (object.containsKey("providers")) {
			JSONArray providersObject = getMustExist(object, "providers");
			for (int i = 0; i < providersObject.size(); i++) {
				providers.add(toValueProvider((JSONObject) providersObject.get(i)));
			}
		}
		return new ItemHint(name, values, providers);
	}

	private ItemHint.ValueHint toValueHint(JSONObject object) {
		Object value = readItemValue(getMustExist(object, "value"));
		String description = getOrNull(object, "description");
		return new ItemHint.ValueHint(value, description);
	}

	private ItemHint.ValueProvider toValueProvider(JSONObject object) {
		String name = getMustExist(object, "name");
		Map<String, Object> parameters = new HashMap<String, Object>();
		if (object.containsKey("parameters")) {
			JSONObject parametersObject = getMustExist(object, "parameters");
			for (Object k : parametersObject.keySet()) {
				String key = (String) k;
				Object value = readItemValue(parametersObject.get(key));
				parameters.put(key, value);
			}
		}
		return new ItemHint.ValueProvider(name, parameters);
	}

	private Object readItemValue(Object value) {
		if (value instanceof JSONArray) {
			JSONArray array = (JSONArray) value;
			Object[] content = new Object[array.size()];
			for (int i = 0; i < array.size(); i++) {
				content[i] = array.get(i);
			}
			return content;
		}
		return value;
	}

	private JSONObject parseJson(InputStream inputStream) throws IOException, ParseException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		JSONParser parser = new JSONParser();
		parser.parse(out.toString());
		return (JSONObject) parser.parse(out.toString());
	}

	@SuppressWarnings("unchecked")
	private <T> T get(JSONObject source, String key, boolean mustExist) {
		final T value = (T) source.get(key);
		if (value != null) {
			return value;
		}

		if (mustExist) {
			throw new IllegalStateException("Key " + key + "not found.");
		}

		return null;
	}

	private <T> T getOrNull(JSONObject source, String key) {
		return get(source, key, false);
	}

	private <T> T getMustExist(JSONObject source, String key) {
		return get(source, key, true);
	}

	/**
	 * Extension to {@link JSONObject} that remembers the order of inserts.
	 */
	@SuppressWarnings("rawtypes")
	private static class JSONOrderedObject extends JSONObject {
		private static final long serialVersionUID = -2562835478249976579L;

		private Set<String> keys = new LinkedHashSet<String>();

		@Override
		public Object put(Object key, Object value) {
			this.keys.add((String) key);
			return super.put(key, value);
		}

		@Override
		public Set keySet() {
			return this.keys;
		}
	}
}
