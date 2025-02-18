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

package org.springframework.boot.json;

import java.util.ArrayList;
import java.util.Arrays;
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

	@Override
	public Map<String, Object> parseMap(String json) {
		return tryParse(() -> parseMap(json, (jsonToParse) -> parseMapInternal(0, jsonToParse)), Exception.class);
	}

	@Override
	public List<Object> parseList(String json) {
		return tryParse(() -> parseList(json, (jsonToParse) -> parseListInternal(0, jsonToParse)), Exception.class);
	}

	private List<Object> parseListInternal(int nesting, String json) {
		List<Object> list = new ArrayList<>();
		json = trimEdges(json, '[', ']').trim();
		for (String value : tokenize(json)) {
			list.add(parseInternal(nesting + 1, value));
		}
		return list;
	}

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
			return trimEdges(json, '"', '"');
		}
		return parseNumber(json);
	}

	private Map<String, Object> parseMapInternal(int nesting, String json) {
		Map<String, Object> map = new LinkedHashMap<>();
		json = trimEdges(json, '{', '}').trim();
		for (String pair : tokenize(json)) {
			String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
			Assert.state(values[0].startsWith("\"") && values[0].endsWith("\""),
					"Expecting double-quotes around field names");
			String key = trimEdges(values[0], '"', '"');
			Object value = parseInternal(nesting, values[1]);
			map.put(key, value);
		}
		return map;
	}

	private Object parseNumber(String json) {
		try {
			return Long.valueOf(json);
		}
		catch (NumberFormatException ex) {
			try {
				return Double.valueOf(json);
			}
			catch (NumberFormatException ex2) {
				return json;
			}
		}
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

	private static String trimEdges(String string, char leadingChar, char trailingChar) {
		return trimTrailingCharacter(trimLeadingCharacter(string, leadingChar), trailingChar);
	}

	private List<String> tokenize(String json) {
		List<String> list = new ArrayList<>();
		Tracking tracking = new Tracking();
		StringBuilder build = new StringBuilder();
		int index = 0;
		while (index < json.length()) {
			char ch = json.charAt(index);
			if (tracking.in(Tracked.ESCAPE)) {
				build.append(ch);
				index++;
				tracking.set(Tracked.ESCAPE, 0);
				continue;
			}
			switch (ch) {
				case '{' -> tracking.update(Tracked.OBJECT, +1);
				case '}' -> tracking.update(Tracked.OBJECT, -1);
				case '[' -> tracking.update(Tracked.LIST, +1);
				case ']' -> tracking.update(Tracked.LIST, -1);
				case '"' -> tracking.toggle(Tracked.VALUE);
			}
			if (ch == ',' && !tracking.in(Tracked.OBJECT, Tracked.LIST, Tracked.VALUE)) {
				list.add(build.toString());
				build.setLength(0);
			}
			else if (ch == '\\') {
				tracking.set(Tracked.ESCAPE, 1);
			}
			else {
				build.append(ch);
			}
			index++;
		}
		if (!build.isEmpty()) {
			list.add(build.toString().trim());
		}
		return list;
	}

	private static final class Tracking {

		private final int[] counts = new int[Tracked.values().length];

		boolean in(Tracked... tracked) {
			return Arrays.stream(tracked).mapToInt(this::get).anyMatch((i) -> i > 0);
		}

		void toggle(Tracked tracked) {
			set(tracked, (get(tracked) != 0) ? 0 : 1);
		}

		void update(Tracked tracked, int delta) {
			set(tracked, get(tracked) + delta);
		}

		private int get(Tracked tracked) {
			return this.counts[tracked.ordinal()];
		}

		void set(Tracked tracked, int count) {
			this.counts[tracked.ordinal()] = count;
		}

	}

	private enum Tracked {

		OBJECT, LIST, VALUE, ESCAPE

	}

}
