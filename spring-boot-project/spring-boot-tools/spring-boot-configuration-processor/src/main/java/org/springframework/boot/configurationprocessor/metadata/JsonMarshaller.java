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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
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

	private static final int BUFFER_SIZE = 4098;

	public void write(ConfigurationMetadata metadata, OutputStream outputStream)
			throws IOException {
		try {
			JSONObject object = new JSONOrderedObject();
			JsonConverter converter = new JsonConverter();
			object.put("groups", converter.toJsonArray(metadata, ItemType.GROUP));
			object.put("properties", converter.toJsonArray(metadata, ItemType.PROPERTY));
			object.put("hints", converter.toJsonArray(metadata.getHints()));
			outputStream.write(object.toString(2).getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception ex) {
			if (ex instanceof IOException) {
				throw (IOException) ex;
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			throw new IllegalStateException(ex);
		}
	}

	public ConfigurationMetadata read(InputStream inputStream) throws Exception {
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

	private ItemMetadata toItemMetadata(JSONObject object, ItemType itemType)
			throws Exception {
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = readItemValue(object.opt("defaultValue"));
		ItemDeprecation deprecation = toItemDeprecation(object);
		return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod,
				description, defaultValue, deprecation);
	}

	private ItemDeprecation toItemDeprecation(JSONObject object) throws Exception {
		if (object.has("deprecation")) {
			JSONObject deprecationJsonObject = object.getJSONObject("deprecation");
			ItemDeprecation deprecation = new ItemDeprecation();
			deprecation.setLevel(deprecationJsonObject.optString("level", null));
			deprecation.setReason(deprecationJsonObject.optString("reason", null));
			deprecation
					.setReplacement(deprecationJsonObject.optString("replacement", null));
			return deprecation;
		}
		return (object.optBoolean("deprecated") ? new ItemDeprecation() : null);
	}

	private ItemHint toItemHint(JSONObject object) throws Exception {
		String name = object.getString("name");
		List<ItemHint.ValueHint> values = new ArrayList<>();
		if (object.has("values")) {
			JSONArray valuesArray = object.getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				values.add(toValueHint((JSONObject) valuesArray.get(i)));
			}
		}
		List<ItemHint.ValueProvider> providers = new ArrayList<>();
		if (object.has("providers")) {
			JSONArray providersObject = object.getJSONArray("providers");
			for (int i = 0; i < providersObject.length(); i++) {
				providers.add(toValueProvider((JSONObject) providersObject.get(i)));
			}
		}
		return new ItemHint(name, values, providers);
	}

	private ItemHint.ValueHint toValueHint(JSONObject object) throws Exception {
		Object value = readItemValue(object.get("value"));
		String description = object.optString("description", null);
		return new ItemHint.ValueHint(value, description);
	}

	private ItemHint.ValueProvider toValueProvider(JSONObject object) throws Exception {
		String name = object.getString("name");
		Map<String, Object> parameters = new HashMap<>();
		if (object.has("parameters")) {
			JSONObject parametersObject = object.getJSONObject("parameters");
			for (Iterator<?> iterator = parametersObject.keys(); iterator.hasNext();) {
				String key = (String) iterator.next();
				Object value = readItemValue(parametersObject.get(key));
				parameters.put(key, value);
			}
		}
		return new ItemHint.ValueProvider(name, parameters);
	}

	private Object readItemValue(Object value) throws Exception {
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
		InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

}
