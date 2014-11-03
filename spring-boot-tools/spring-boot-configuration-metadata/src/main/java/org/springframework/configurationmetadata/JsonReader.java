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

package org.springframework.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read standard json metadata format as {@link ConfigurationMetadataRepository}
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class JsonReader {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final int BUFFER_SIZE = 4096;

	public RawConfigurationMetadata read(InputStream in) throws IOException {
		return read(in, DEFAULT_CHARSET);
	}

	public RawConfigurationMetadata read(InputStream in, Charset charset) throws IOException {
		JSONObject json = readJson(in, charset);
		List<ConfigurationMetadataSource> groups = parseAllSources(json);
		List<ConfigurationMetadataItem> items = parseAllItems(json);
		return new RawConfigurationMetadata(groups, items);
	}

	private List<ConfigurationMetadataSource> parseAllSources(JSONObject root) {
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

	private List<ConfigurationMetadataItem> parseAllItems(JSONObject root) {
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

	private ConfigurationMetadataSource parseSource(JSONObject json) {
		ConfigurationMetadataSource source = new ConfigurationMetadataSource();
		source.setGroupId(json.getString("name"));
		source.setType(json.optString("type", null));
		source.setDescription(json.optString("description", null));
		source.setSourceType(json.optString("sourceType", null));
		source.setSourceMethod(json.optString("sourceMethod", null));
		return source;
	}

	private ConfigurationMetadataItem parseItem(JSONObject json) {
		ConfigurationMetadataItem item = new ConfigurationMetadataItem();
		item.setId(json.getString("name"));
		item.setType(json.optString("type", null));
		item.setDescription(json.optString("description", null));
		item.setDefaultValue(json.opt("defaultValue"));
		item.setDeprecated(json.optBoolean("deprecated", false));
		item.setSourceType(json.optString("sourceType", null));
		item.setSourceMethod(json.optString("sourceMethod", null));
		return item;
	}

	private JSONObject readJson(InputStream in, Charset charset) throws IOException {
		try {
			StringBuilder out = new StringBuilder();
			InputStreamReader reader = new InputStreamReader(in, charset);
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead = -1;
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
