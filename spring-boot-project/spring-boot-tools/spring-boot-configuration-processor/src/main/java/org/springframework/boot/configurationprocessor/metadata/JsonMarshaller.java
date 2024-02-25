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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Marshaller to read and write {@link ConfigurationMetadata} as JSON.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 1.2.0
 */
public class JsonMarshaller {

	/**
     * Writes the given ConfigurationMetadata to the specified OutputStream in JSON format.
     * The metadata includes groups, properties, and hints.
     *
     * @param metadata     the ConfigurationMetadata to be written
     * @param outputStream the OutputStream to write the JSON data to
     * @throws IOException if an I/O error occurs while writing the data
     */
    public void write(ConfigurationMetadata metadata, OutputStream outputStream) throws IOException {
		try {
			JSONObject object = new JSONObject();
			JsonConverter converter = new JsonConverter();
			object.put("groups", converter.toJsonArray(metadata, ItemType.GROUP));
			object.put("properties", converter.toJsonArray(metadata, ItemType.PROPERTY));
			object.put("hints", converter.toJsonArray(metadata.getHints()));
			outputStream.write(object.toString(2).getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception ex) {
			if (ex instanceof IOException ioException) {
				throw ioException;
			}
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Reads the configuration metadata from the given input stream.
     * 
     * @param inputStream the input stream to read from
     * @return the configuration metadata
     * @throws Exception if an error occurs while reading the input stream
     */
    public ConfigurationMetadata read(InputStream inputStream) throws Exception {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = new JSONObject(toString(inputStream));
		JsonPath path = JsonPath.root();
		checkAllowedKeys(object, path, "groups", "properties", "hints");
		JSONArray groups = object.optJSONArray("groups");
		if (groups != null) {
			for (int i = 0; i < groups.length(); i++) {
				metadata
					.add(toItemMetadata((JSONObject) groups.get(i), path.resolve("groups").index(i), ItemType.GROUP));
			}
		}
		JSONArray properties = object.optJSONArray("properties");
		if (properties != null) {
			for (int i = 0; i < properties.length(); i++) {
				metadata.add(toItemMetadata((JSONObject) properties.get(i), path.resolve("properties").index(i),
						ItemType.PROPERTY));
			}
		}
		JSONArray hints = object.optJSONArray("hints");
		if (hints != null) {
			for (int i = 0; i < hints.length(); i++) {
				metadata.add(toItemHint((JSONObject) hints.get(i), path.resolve("hints").index(i)));
			}
		}
		return metadata;
	}

	/**
     * Converts a JSONObject to an ItemMetadata object.
     * 
     * @param object the JSONObject to convert
     * @param path the JsonPath of the object
     * @param itemType the type of the item (GROUP or PROPERTY)
     * @return the converted ItemMetadata object
     * @throws Exception if an error occurs during conversion
     */
    private ItemMetadata toItemMetadata(JSONObject object, JsonPath path, ItemType itemType) throws Exception {
		switch (itemType) {
			case GROUP -> checkAllowedKeys(object, path, "name", "type", "description", "sourceType", "sourceMethod");
			case PROPERTY -> checkAllowedKeys(object, path, "name", "type", "description", "sourceType", "defaultValue",
					"deprecation", "deprecated");
		}
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = readItemValue(object.opt("defaultValue"));
		ItemDeprecation deprecation = toItemDeprecation(object, path);
		return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod, description, defaultValue,
				deprecation);
	}

	/**
     * Converts a JSONObject to an ItemDeprecation object.
     * 
     * @param object the JSONObject to convert
     * @param path the JsonPath of the object
     * @return the converted ItemDeprecation object
     * @throws Exception if an error occurs during conversion
     */
    private ItemDeprecation toItemDeprecation(JSONObject object, JsonPath path) throws Exception {
		if (object.has("deprecation")) {
			JSONObject deprecationJsonObject = object.getJSONObject("deprecation");
			checkAllowedKeys(deprecationJsonObject, path.resolve("deprecation"), "level", "reason", "replacement",
					"since");
			ItemDeprecation deprecation = new ItemDeprecation();
			deprecation.setLevel(deprecationJsonObject.optString("level", null));
			deprecation.setReason(deprecationJsonObject.optString("reason", null));
			deprecation.setReplacement(deprecationJsonObject.optString("replacement", null));
			deprecation.setSince(deprecationJsonObject.optString("since", null));
			return deprecation;
		}
		return object.optBoolean("deprecated") ? new ItemDeprecation() : null;
	}

	/**
     * Converts a JSONObject to an ItemHint object.
     * 
     * @param object the JSONObject to convert
     * @param path the JsonPath of the object
     * @return the converted ItemHint object
     * @throws Exception if there is an error during conversion
     */
    private ItemHint toItemHint(JSONObject object, JsonPath path) throws Exception {
		checkAllowedKeys(object, path, "name", "values", "providers");
		String name = object.getString("name");
		List<ItemHint.ValueHint> values = new ArrayList<>();
		if (object.has("values")) {
			JSONArray valuesArray = object.getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				values.add(toValueHint((JSONObject) valuesArray.get(i), path.resolve("values").index(i)));
			}
		}
		List<ItemHint.ValueProvider> providers = new ArrayList<>();
		if (object.has("providers")) {
			JSONArray providersObject = object.getJSONArray("providers");
			for (int i = 0; i < providersObject.length(); i++) {
				providers.add(toValueProvider((JSONObject) providersObject.get(i), path.resolve("providers").index(i)));
			}
		}
		return new ItemHint(name, values, providers);
	}

	/**
     * Converts a JSONObject to a ValueHint object.
     * 
     * @param object the JSONObject to convert
     * @param path the JsonPath of the object
     * @return the converted ValueHint object
     * @throws Exception if an error occurs during conversion
     */
    private ItemHint.ValueHint toValueHint(JSONObject object, JsonPath path) throws Exception {
		checkAllowedKeys(object, path, "value", "description");
		Object value = readItemValue(object.get("value"));
		String description = object.optString("description", null);
		return new ItemHint.ValueHint(value, description);
	}

	/**
     * Converts a JSONObject to a ValueProvider object.
     * 
     * @param object the JSONObject to convert
     * @param path the JsonPath of the object
     * @return the converted ValueProvider object
     * @throws Exception if there is an error during conversion
     */
    private ItemHint.ValueProvider toValueProvider(JSONObject object, JsonPath path) throws Exception {
		checkAllowedKeys(object, path, "name", "parameters");
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

	/**
     * Reads the value of an item in a JSON object or array.
     * 
     * @param value the value of the item to be read
     * @return the value of the item, or an array of values if the item is a JSON array
     * @throws Exception if an error occurs during the reading process
     */
    private Object readItemValue(Object value) throws Exception {
		if (value instanceof JSONArray array) {
			Object[] content = new Object[array.length()];
			for (int i = 0; i < array.length(); i++) {
				content[i] = array.get(i);
			}
			return content;
		}
		return value;
	}

	/**
     * Converts an InputStream to a String using UTF-8 encoding.
     * 
     * @param inputStream the InputStream to be converted
     * @return the converted String
     * @throws IOException if an I/O error occurs while reading the InputStream
     */
    private String toString(InputStream inputStream) throws IOException {
		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}

	/**
     * Checks if the given JSON object contains only the allowed keys.
     * 
     * @param object       the JSON object to check
     * @param path         the JSON path of the object
     * @param allowedKeys  the allowed keys
     * 
     * @throws IllegalStateException if additional keys are found in the JSON object
     */
    @SuppressWarnings("unchecked")
	private void checkAllowedKeys(JSONObject object, JsonPath path, String... allowedKeys) {
		Set<String> availableKeys = new TreeSet<>();
		object.keys().forEachRemaining((key) -> availableKeys.add((String) key));
		Arrays.stream(allowedKeys).forEach(availableKeys::remove);
		if (!availableKeys.isEmpty()) {
			throw new IllegalStateException("Expected only keys %s, but found additional keys %s. Path: %s"
				.formatted(new TreeSet<>(Arrays.asList(allowedKeys)), availableKeys, path));
		}
	}

	/**
     * JsonPath class.
     */
    private static final class JsonPath {

		private final String path;

		/**
         * Constructs a new JsonPath object with the specified path.
         * 
         * @param path the path to be used for the JsonPath object
         */
        private JsonPath(String path) {
			this.path = path;
		}

		/**
         * Resolves the given path and returns a new JsonPath object.
         * 
         * @param path the path to be resolved
         * @return a new JsonPath object with the resolved path
         */
        JsonPath resolve(String path) {
			if (this.path.endsWith(".")) {
				return new JsonPath(this.path + path);
			}
			return new JsonPath(this.path + "." + path);
		}

		/**
         * Returns a new JsonPath object representing the index at the specified position.
         *
         * @param index the position of the index
         * @return a new JsonPath object representing the index at the specified position
         */
        JsonPath index(int index) {
			return resolve("[%d]".formatted(index));
		}

		/**
         * Returns a string representation of the JsonPath object.
         * 
         * @return the path string of the JsonPath object
         */
        @Override
		public String toString() {
			return this.path;
		}

		/**
         * Returns a new instance of JsonPath representing the root path.
         * 
         * @return a new instance of JsonPath representing the root path
         */
        static JsonPath root() {
			return new JsonPath(".");
		}

	}

}
