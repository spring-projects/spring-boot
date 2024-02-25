/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.util.Assert;
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

	private static final int MAX_DEPTH = 1000;

	/**
     * Parses a JSON string into a Map object.
     * 
     * @param json the JSON string to parse
     * @return a Map object representing the parsed JSON
     * @throws Exception if an error occurs during parsing
     */
    @Override
	public Map<String, Object> parseMap(String json) {
		return tryParse(() -> parseMap(json, (jsonToParse) -> parseMapInternal(0, jsonToParse)), Exception.class);
	}

	/**
     * Parses a JSON string into a list of objects.
     * 
     * @param json the JSON string to parse
     * @return a list of objects parsed from the JSON string
     * @throws Exception if an error occurs during parsing
     */
    @Override
	public List<Object> parseList(String json) {
		return tryParse(() -> parseList(json, (jsonToParse) -> parseListInternal(0, jsonToParse)), Exception.class);
	}

	/**
     * Parses a JSON string representing a list and returns a List of Objects.
     * 
     * @param nesting the current nesting level of the JSON string
     * @param json the JSON string to be parsed
     * @return a List of Objects parsed from the JSON string
     */
    private List<Object> parseListInternal(int nesting, String json) {
		List<Object> list = new ArrayList<>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, ']'), '[').trim();
		for (String value : tokenize(json)) {
			list.add(parseInternal(nesting + 1, value));
		}
		return list;
	}

	/**
     * Parses the given JSON string and returns the corresponding object representation.
     * 
     * @param nesting the current nesting level of the JSON string
     * @param json the JSON string to be parsed
     * @return the parsed object representation of the JSON string
     * @throws IllegalStateException if the JSON string is too deeply nested
     */
    private Object parseInternal(int nesting, String json) {
		if (nesting > MAX_DEPTH) {
			throw new IllegalStateException("JSON is too deeply nested");
		}
		if (json.startsWith("[")) {
			return parseListInternal(nesting + 1, json);
		}
		if (json.startsWith("{")) {
			return parseMapInternal(nesting + 1, json);
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

	/**
     * Parses a JSON string and returns a Map representation of the JSON object.
     * 
     * @param nesting the current nesting level of the JSON object
     * @param json the JSON string to be parsed
     * @return a Map containing the key-value pairs of the JSON object
     * @throws IllegalArgumentException if the JSON string is not valid
     */
    private Map<String, Object> parseMapInternal(int nesting, String json) {
		Map<String, Object> map = new LinkedHashMap<>();
		json = trimLeadingCharacter(trimTrailingCharacter(json, '}'), '{').trim();
		for (String pair : tokenize(json)) {
			String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
			Assert.state(values[0].startsWith("\"") && values[0].endsWith("\""),
					"Expecting double-quotes around field names");
			String key = trimLeadingCharacter(trimTrailingCharacter(values[0], '"'), '"');
			Object value = parseInternal(nesting, values[1]);
			map.put(key, value);
		}
		return map;
	}

	/**
     * Removes the trailing occurrences of a specified character from a given string.
     * 
     * @param string the string to be trimmed
     * @param c the character to be removed from the end of the string
     * @return the trimmed string
     */
    private static String trimTrailingCharacter(String string, char c) {
		if (!string.isEmpty() && string.charAt(string.length() - 1) == c) {
			return string.substring(0, string.length() - 1);
		}
		return string;
	}

	/**
     * Removes the leading occurrences of a specified character from a given string.
     *
     * @param string the string to be trimmed
     * @param c the character to be removed from the beginning of the string
     * @return the trimmed string
     */
    private static String trimLeadingCharacter(String string, char c) {
		if (!string.isEmpty() && string.charAt(0) == c) {
			return string.substring(1);
		}
		return string;
	}

	/**
     * Tokenizes a JSON string into a list of individual tokens.
     * 
     * @param json the JSON string to tokenize
     * @return a list of tokens extracted from the JSON string
     */
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
		if (!build.isEmpty()) {
			list.add(build.toString().trim());
		}
		return list;
	}

}
