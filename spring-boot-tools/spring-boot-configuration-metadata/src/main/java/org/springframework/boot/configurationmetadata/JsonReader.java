/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read standard json metadata format as {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class JsonReader {

	private static final int BUFFER_SIZE = 4096;

	private final DescriptionExtractor descriptionExtractor = new DescriptionExtractor();

	public RawConfigurationMetadata read(InputStream in, Charset charset)
			throws IOException {
		try {
			JSONObject json = readJson(in, charset);
			List<ConfigurationMetadataSource> groups = parseAllSources(json);
			List<ConfigurationMetadataItem> items = parseAllItems(json);
			List<ConfigurationMetadataHint> hints = parseAllHints(json);
			return new RawConfigurationMetadata(groups, items, hints);
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

	private List<ConfigurationMetadataSource> parseAllSources(JSONObject root)
			throws Exception {
		List<ConfigurationMetadataSource> result = new ArrayList<ConfigurationMetadataSource>();
		if (!root.has("groups")) {
			return result;
		}
		JSONArray sources = root.getJSONArray("groups");
		for (int i = 0; i < sources.length(); i++) {
			JSONObject source = sources.getJSONObject(i);
			result.add(parseSource(source));
		}
		return result;
	}

	private List<ConfigurationMetadataItem> parseAllItems(JSONObject root)
			throws Exception {
		List<ConfigurationMetadataItem> result = new ArrayList<ConfigurationMetadataItem>();
		if (!root.has("properties")) {
			return result;
		}
		JSONArray items = root.getJSONArray("properties");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			result.add(parseItem(item));
		}
		return result;
	}

	private List<ConfigurationMetadataHint> parseAllHints(JSONObject root)
			throws Exception {
		List<ConfigurationMetadataHint> result = new ArrayList<ConfigurationMetadataHint>();
		if (!root.has("hints")) {
			return result;
		}
		JSONArray items = root.getJSONArray("hints");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			result.add(parseHint(item));
		}
		return result;
	}

	private ConfigurationMetadataSource parseSource(JSONObject json) throws Exception {
		ConfigurationMetadataSource source = new ConfigurationMetadataSource();
		source.setGroupId(json.getString("name"));
		source.setType(json.optString("type", null));
		String description = json.optString("description", null);
		source.setDescription(description);
		source.setShortDescription(
				this.descriptionExtractor.getShortDescription(description));
		source.setSourceType(json.optString("sourceType", null));
		source.setSourceMethod(json.optString("sourceMethod", null));
		return source;
	}

	private ConfigurationMetadataItem parseItem(JSONObject json) throws Exception {
		ConfigurationMetadataItem item = new ConfigurationMetadataItem();
		item.setId(json.getString("name"));
		item.setType(json.optString("type", null));
		String description = json.optString("description", null);
		item.setDescription(description);
		item.setShortDescription(
				this.descriptionExtractor.getShortDescription(description));
		item.setDefaultValue(readItemValue(json.opt("defaultValue")));
		item.setDeprecation(parseDeprecation(json));
		item.setSourceType(json.optString("sourceType", null));
		item.setSourceMethod(json.optString("sourceMethod", null));
		return item;
	}

	private ConfigurationMetadataHint parseHint(JSONObject json) throws Exception {
		ConfigurationMetadataHint hint = new ConfigurationMetadataHint();
		hint.setId(json.getString("name"));
		if (json.has("values")) {
			JSONArray values = json.getJSONArray("values");
			for (int i = 0; i < values.length(); i++) {
				JSONObject value = values.getJSONObject(i);
				ValueHint valueHint = new ValueHint();
				valueHint.setValue(readItemValue(value.get("value")));
				String description = value.optString("description", null);
				valueHint.setDescription(description);
				valueHint.setShortDescription(
						this.descriptionExtractor.getShortDescription(description));
				hint.getValueHints().add(valueHint);
			}
		}
		if (json.has("providers")) {
			JSONArray providers = json.getJSONArray("providers");
			for (int i = 0; i < providers.length(); i++) {
				JSONObject provider = providers.getJSONObject(i);
				ValueProvider valueProvider = new ValueProvider();
				valueProvider.setName(provider.getString("name"));
				if (provider.has("parameters")) {
					JSONObject parameters = provider.getJSONObject("parameters");
					Iterator<?> keys = parameters.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						valueProvider.getParameters().put(key,
								readItemValue(parameters.get(key)));
					}
				}
				hint.getValueProviders().add(valueProvider);
			}
		}
		return hint;
	}

	private Deprecation parseDeprecation(JSONObject object) throws Exception {
		if (object.has("deprecation")) {
			JSONObject deprecationJsonObject = object.getJSONObject("deprecation");
			Deprecation deprecation = new Deprecation();
			deprecation.setLevel(parseDeprecationLevel(
					deprecationJsonObject.optString("level", null)));
			deprecation.setReason(deprecationJsonObject.optString("reason", null));
			deprecation
					.setReplacement(deprecationJsonObject.optString("replacement", null));
			return deprecation;
		}
		return (object.optBoolean("deprecated") ? new Deprecation() : null);
	}

	private Deprecation.Level parseDeprecationLevel(String value) {
		if (value != null) {
			try {
				return Deprecation.Level.valueOf(value.toUpperCase(Locale.ENGLISH));
			}
			catch (IllegalArgumentException ex) {
				// let's use the default
			}
		}
		return Deprecation.Level.WARNING;
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

	private JSONObject readJson(InputStream in, Charset charset) throws Exception {
		try {
			StringBuilder out = new StringBuilder();
			InputStreamReader reader = new InputStreamReader(in, charset);
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = reader.read(buffer)) != -1) {
				out.append(buffer, 0, bytesRead);
			}
			return new JSONObject(out.toString());
		}
		finally {
			in.close();
		}
	}

}
