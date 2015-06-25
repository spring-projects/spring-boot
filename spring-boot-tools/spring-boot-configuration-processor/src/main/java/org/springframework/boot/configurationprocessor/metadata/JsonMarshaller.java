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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
		JSONObject object = new JSONObject();
		object.put("groups", toJsonArray(metadata, ItemType.GROUP));
		object.put("properties", toJsonArray(metadata, ItemType.PROPERTY));
		object.put("hints", toJsonArray(metadata.getHints()));
		outputStream.write(object.toString(2).getBytes(UTF_8));
	}

	private JSONArray toJsonArray(ConfigurationMetadata metadata, ItemType itemType) {
		JSONArray jsonArray = new JSONArray();
		for (ItemMetadata item : metadata.getItems()) {
			if (item.isOfItemType(itemType)) {
				jsonArray.put(toJsonObject(item));
			}
		}
		return jsonArray;
	}

	private JSONArray toJsonArray(Collection<ItemHint> hints) {
		JSONArray jsonArray = new JSONArray();
		for (ItemHint hint : hints) {
			jsonArray.put(toJsonObject(hint));
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
		if (item.isDeprecated()) {
			jsonObject.put("deprecated", true);
		}
		return jsonObject;
	}

	private JSONObject toJsonObject(ItemHint hint) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", hint.getName());
		if (!hint.getValues().isEmpty()) {
			JSONArray valuesArray = new JSONArray();
			for (ItemHint.ValueHint valueHint : hint.getValues()) {
				JSONObject valueObject = new JSONOrderedObject();
				putHintValue(valueObject, valueHint.getValue());
				putIfPresent(valueObject, "description", valueHint.getDescription());
				valuesArray.put(valueObject);
			}
			jsonObject.put("values", valuesArray);
		}
		if (!hint.getProviders().isEmpty()) {
			JSONArray providersArray = new JSONArray();
			for (ItemHint.ProviderHint providerHint : hint.getProviders()) {
				JSONObject providerHintObject = new JSONOrderedObject();
				providerHintObject.put("name", providerHint.getName());
				if (providerHint.getParameters() != null
						&& !providerHint.getParameters().isEmpty()) {
					JSONObject parametersObject = new JSONOrderedObject();
					for (Map.Entry<String, Object> entry : providerHint.getParameters()
							.entrySet()) {
						parametersObject.put(entry.getKey(),
								extractItemValue(entry.getValue()));
					}
					providerHintObject.put("parameters", parametersObject);
				}
				providersArray.put(providerHintObject);
			}
			jsonObject.put("providers", providersArray);
		}
		return jsonObject;
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
				array.put(Array.get(value, i));
			}
			defaultValue = array;

		}
		return defaultValue;
	}

	public ConfigurationMetadata read(InputStream inputStream) throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = new JSONObject(toString(inputStream));
		JSONArray groups = object.optJSONArray("groups");
		if (groups != null) {
			for (int i = 0; i < groups.length(); i++) {
				metadata.add(toItemMetadata((JSONObject) groups.get(i), ItemType.GROUP));
			}
		}
		JSONArray properties = object.optJSONArray("properties");
		if (properties != null) {
			for (int i = 0; i < properties.length(); i++) {
				metadata.add(toItemMetadata((JSONObject) properties.get(i),
						ItemType.PROPERTY));
			}
		}
		JSONArray hints = object.optJSONArray("hints");
		if (hints != null) {
			for (int i = 0; i < hints.length(); i++) {
				metadata.add(toItemHint((JSONObject) hints.get(i)));
			}
		}
		return metadata;
	}

	private ItemMetadata toItemMetadata(JSONObject object, ItemType itemType) {
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = readItemValue(object.opt("defaultValue"));
		boolean deprecated = object.optBoolean("deprecated");
		return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod,
				description, defaultValue, deprecated);
	}

	private ItemHint toItemHint(JSONObject object) {
		String name = object.getString("name");
		List<ItemHint.ValueHint> values = new ArrayList<ItemHint.ValueHint>();
		if (object.has("values")) {
			JSONArray valuesArray = object.getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				values.add(toValueHint((JSONObject) valuesArray.get(i)));
			}
		}
		List<ItemHint.ProviderHint> providers = new ArrayList<ItemHint.ProviderHint>();
		if (object.has("providers")) {
			JSONArray providersObject = object.getJSONArray("providers");
			for (int i = 0; i < providersObject.length(); i++) {
				providers.add(toProviderHint((JSONObject) providersObject.get(i)));
			}
		}
		return new ItemHint(name, values, providers);
	}

	private ItemHint.ValueHint toValueHint(JSONObject object) {
		Object value = readItemValue(object.get("value"));
		String description = object.optString("description", null);
		return new ItemHint.ValueHint(value, description);
	}

	private ItemHint.ProviderHint toProviderHint(JSONObject object) {
		String name = object.getString("name");
		Map<String, Object> parameters = new HashMap<String, Object>();
		if (object.has("parameters")) {
			JSONObject parametersObject = object.getJSONObject("parameters");
			for (Object k : parametersObject.keySet()) {
				String key = (String) k;
				Object value = readItemValue(parametersObject.get(key));
				parameters.put(key, value);
			}
		}
		return new ItemHint.ProviderHint(name, parameters);
	}

	private Object readItemValue(Object value) {
		if (value instanceof JSONArray) {
			JSONArray array = (JSONArray) value;
			Object[] content = new Object[array.length()];
			for (int i = 0; i < array.length(); i++) {
				content[i] = array.get(i);
			}
			return content;
		}
		return value;
	}

	private String toString(InputStream inputStream) throws IOException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

	/**
	 * Extension to {@link JSONObject} that remembers the order of inserts.
	 */
	@SuppressWarnings("rawtypes")
	private static class JSONOrderedObject extends JSONObject {

		private Set<String> keys = new LinkedHashSet<String>();

		@Override
		public JSONObject put(String key, Object value) throws JSONException {
			this.keys.add(key);
			return super.put(key, value);
		}

		@Override
		public Set keySet() {
			return this.keys;
		}

	}

}
