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

package org.springframework.boot.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * Really basic JSON parser for when you have nothing else available. Comes with some
 * limitations with respect to the JSON specification (e.g. only supports String values),
 * so users will probably prefer to have a library handle things instead (Jackson or Snake
 * YAML are supported).
 *
 * @author Dave Syer
 * @see JsonParserFactory
 * @since 1.2.0
 */
public class BasicJsonParser implements JsonParser {

	@Override
	public Map<String, Object> parseMap(String json) {
		if (json != null) {
			json = json.trim();
			if (json.startsWith("{")) {
				return parseMapInternal(json);
			}
			else if (json.equals("")) {
				return new HashMap<String, Object>();
			}
		}
		return null;
	}

	@Override
	public List<Object> parseList(String json) {
		if (json != null) {
			json = json.trim();
			if (json.startsWith("[")) {
				return parseListInternal(json);
			}
			else if (json.trim().equals("")) {
				return new ArrayList<Object>();
			}
		}
		return null;
	}

	private List<Object> parseListInternal(String json) {
		List<Object> list = new ArrayList<Object>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, ']'), '[');
		for (String value : tokenize(json)) {
			list.add(parseInternal(value));
		}
		return list;
	}

	private Object parseInternal(String json) {
		if (json.startsWith("[")) {
			return parseListInternal(json);
		}

		if (json.startsWith("{")) {
			return parseMapInternal(json);
		}

		if (json.startsWith("\"")) {
			return trimTrailingCharacter(trimLeadingCharacter(json, '"'), '"');
		}

		try {
			return Long.valueOf(json);
		}
		catch (NumberFormatException ex) {
			// ignore
		}

		try {
			return Double.valueOf(json);
		}
		catch (NumberFormatException ex) {
			// ignore
		}

		return json;
	}

	private static String trimTrailingCharacter(String string, char c) {
		if (string.length() >= 0 && string.charAt(string.length() - 1) == c) {
			return string.substring(0, string.length() - 1);
		}
		return string;
	}

	private static String trimLeadingCharacter(String string, char c) {
		if (string.length() >= 0 && string.charAt(0) == c) {
			return string.substring(1);
		}
		return string;
	}

	private Map<String, Object> parseMapInternal(String json) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, '}'), '{');
		for (String pair : tokenize(json)) {
			String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
			String key = trimLeadingCharacter(trimTrailingCharacter(values[0], '"'), '"');
			Object value = null;
			if (values.length > 0) {
				String string = trimLeadingCharacter(
						trimTrailingCharacter(values[1], '"'), '"');
				value = parseInternal(string);
			}
			map.put(key, value);
		}
		return map;
	}

	private List<String> tokenize(String json) {
		List<String> list = new ArrayList<String>();
		int index = 0;
		int inObject = 0;
		int inList = 0;
		StringBuilder build = new StringBuilder();
		while (index < json.length()) {
			char current = json.charAt(index);
			if (current == '{') {
				inObject++;
			}
			if (current == '}') {
				inObject--;
			}
			if (current == '[') {
				inList++;
			}
			if (current == ']') {
				inList--;
			}
			if (current == ',' && inObject == 0 && inList == 0) {
				list.add(build.toString());
				build.setLength(0);
			}
			else {
				build.append(current);
			}
			index++;
		}
		if (build.length() > 0) {
			list.add(build.toString());
		}
		return list;
	}

}
