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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	throws IOException, ParseException {
		JSONObject json = readJson(in, charset);
		List<ConfigurationMetadataSource> groups = parseAllSources(json);
		List<ConfigurationMetadataItem> items = parseAllItems(json);
		List<ConfigurationMetadataHint> hints = parseAllHints(json);
		return new RawConfigurationMetadata(groups, items, hints);
	}

	private List<ConfigurationMetadataSource> parseAllSources(JSONObject root) {
		return parse(root, this::parseSource, "groups");
	}

	private List<ConfigurationMetadataItem> parseAllItems(JSONObject root) {
		return parse(root, this::parseItem, "properties");
	}

	private List<ConfigurationMetadataHint> parseAllHints(JSONObject root) {
		return parse(root, this::parseHint, "hints");
	}

	private <T> List<T> parse(JSONObject root, Function<JSONObject, T> parser, String key) {
		if (!root.containsKey(key)) {
			return Collections.emptyList();
		}

		List<JSONObject> items = getMustExist(root, (key));
		return items.stream().map(parser).collect(Collectors.toList());
	}

	private ConfigurationMetadataSource parseSource(JSONObject json) {
		ConfigurationMetadataSource source = new ConfigurationMetadataSource();
		source.setGroupId(getMustExist(json, "name"));
		source.setType(getOrNull(json, "type"));
		String description = getOrNull(json, "description");
		source.setDescription(description);
		source.setShortDescription(
				this.descriptionExtractor.getShortDescription(description));
		source.setSourceType(getOrNull(json, "sourceType"));
		source.setSourceMethod(getOrNull(json, "sourceMethod"));
		return source;
	}

	private ConfigurationMetadataItem parseItem(JSONObject json) {
		ConfigurationMetadataItem item = new ConfigurationMetadataItem();
		item.setId(getMustExist(json, "name"));
		item.setType(getOrNull(json, "type"));
		String description = getOrNull(json, "description");
		item.setDescription(description);
		item.setShortDescription(
				this.descriptionExtractor.getShortDescription(description));
		item.setDefaultValue(readItemValue(getOrNull(json, "defaultValue")));
		item.setDeprecation(parseDeprecation(json));
		item.setSourceType(getOrNull(json, "sourceType"));
		item.setSourceMethod(getOrNull(json, "sourceMethod"));
		return item;
	}

	private ConfigurationMetadataHint parseHint(JSONObject json) {
		ConfigurationMetadataHint hint = new ConfigurationMetadataHint();
		hint.setId(getMustExist(json, ("name")));
		if (json.containsKey("values")) {
			JSONArray values = getMustExist(json, "values");
			for (int i = 0; i < values.size(); i++) {
				JSONObject value = (JSONObject) values.get(i);
				ValueHint valueHint = new ValueHint();
				valueHint.setValue(readItemValue(getMustExist(value, "value")));
				String description = getOrNull(value, "description");
				valueHint.setDescription(description);
				valueHint.setShortDescription(
						this.descriptionExtractor.getShortDescription(description));
				hint.getValueHints().add(valueHint);
			}
		}
		if (json.containsKey("providers")) {
			JSONArray providers = getMustExist(json, ("providers"));
			for (int i = 0; i < providers.size(); i++) {
				JSONObject provider = (JSONObject) providers.get(i);
				ValueProvider valueProvider = new ValueProvider();
				valueProvider.setName(getMustExist(provider, "name"));
				if (provider.containsKey("parameters")) {
					JSONObject parameters = getMustExist(provider, "parameters");
					Set<String> keys = parameters.keySet();
					keys.forEach(key -> {
						valueProvider.getParameters().put(key,
							readItemValue(getMustExist(parameters, key)));
					});
				}
				hint.getValueProviders().add(valueProvider);
			}
		}
		return hint;
	}

	private Deprecation parseDeprecation(JSONObject object) {
		if (object.containsKey("deprecation")) {
			JSONObject deprecationJsonObject =  getMustExist(object, "deprecation");
			Deprecation deprecation = new Deprecation();
			deprecation.setReason(getOrNull(deprecationJsonObject, "reason"));
			deprecation
					.setReplacement(getOrNull(deprecationJsonObject, "replacement"));
			return deprecation;
		}
		final Boolean deprecated = getOrNull(object, "deprecated");
		return (deprecated != null && deprecated) ? new Deprecation() : null;
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

	private JSONObject readJson(InputStream in, Charset charset) throws IOException, ParseException {
		try {
			StringWriter out = new StringWriter();
			InputStreamReader reader = new InputStreamReader(in, charset);
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = reader.read(buffer)) != -1) {
				out.append(new String(buffer), 0, bytesRead);
			}
			JSONParser parser = new JSONParser();
			return (JSONObject) parser.parse(out.toString());
		}
		finally {
			in.close();
		}
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
}
