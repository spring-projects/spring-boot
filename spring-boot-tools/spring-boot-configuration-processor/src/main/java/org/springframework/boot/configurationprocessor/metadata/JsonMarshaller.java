/*
 * Copyright 2012-2014 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
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

	private JSONObject toJsonObject(ItemMetadata item) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", item.getName());
		putIfPresent(jsonObject, "type", item.getType());
		putIfPresent(jsonObject, "description", item.getDescription());
		putIfPresent(jsonObject, "sourceType", item.getSourceType());
		putIfPresent(jsonObject, "sourceMethod", item.getSourceMethod());
		putIfPresent(jsonObject, "defaultValue", item.getDefaultValue());
		if (item.isDeprecated()) {
			jsonObject.put("deprecated", true);
		}
		return jsonObject;
	}

	private void putIfPresent(JSONObject jsonObject, String name, Object value) {
		if (value != null) {
			jsonObject.put(name, value);
		}
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
		return metadata;
	}

	private ItemMetadata toItemMetadata(JSONObject object, ItemType itemType) {
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = object.opt("defaultValue");
		boolean deprecated = object.optBoolean("deprecated");
		return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod,
				description, defaultValue, deprecated);
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
