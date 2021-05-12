/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.json;

import java.util.ArrayList;
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
 * @author Jean de Klerk
 * @author Stephane Nicoll
 * @since 1.2.0
 * @see JsonParserFactory
 */
public class BasicJsonParser extends AbstractJsonParser {

	@Override
	public Map<String, Object> parseMap(String json) {
		return parseMap(json, this::parseMapInternal);
	}

	@Override
	public List<Object> parseList(String json) {
		return parseList(json, this::parseListInternal);
	}

	private List<Object> parseListInternal(String json) {
		List<Object> list = new ArrayList<>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, ']'), '[').trim();
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
		if (!string.isEmpty() && string.charAt(string.length() - 1) == c) {
			return string.substring(0, string.length() - 1);
		}
		return string;
	}

	private static String trimLeadingCharacter(String string, char c) {
		if (!string.isEmpty() && string.charAt(0) == c) {
			return string.substring(1);
		}
		return string;
	}

	private Map<String, Object> parseMapInternal(String json) {
		Map<String, Object> map = new LinkedHashMap<>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, '}'), '{').trim();
		for (String pair : tokenize(json)) {
			String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
			String key = trimLeadingCharacter(trimTrailingCharacter(values[0], '"'), '"');
			Object value = parseInternal(values[1]);
			map.put(key, value);
		}
		return map;
	}

	private List<String> tokenize(String json) {
		List<String> list = new ArrayList<>();
		int index = 0;
		int inObject = 0;
		int inList = 0;
		boolean inValue = false;
		boolean inEscape = false;
		StringBuilder build = new StringBuilder();
		while (index < json.length()) {
			char current = json.charAt(index);
			if (inEscape) {
				build.append(current);
				index++;
				inEscape = false;
				continue;
			}
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
			if (current == '"') {
				inValue = !inValue;
			}
			if (current == ',' && inObject == 0 && inList == 0 && !inValue) {
				list.add(build.toString());
				build.setLength(0);
			}
			else if (current == '\\') {
				inEscape = true;
			}
			else {
				build.append(current);
			}
			index++;
		}
		if (build.length() > 0) {
			list.add(build.toString().trim());
		}
		return list;
	}

}
