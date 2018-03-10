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

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base class for parsers wrapped or implemented in this package.
 *
 * @author Anton Telechev
 */
abstract class AbstractJsonParser implements JsonParser {

	/** Start symbol of a JSON map. **/
	private static final String START_MAP = "{";

	/** Start symbol of a JSON list. **/
	private static final String START_LIST = "[";

	/**
	 * Parses the specified JSON string and returns the extracted contents as a Map of
	 * String to Object.
	 *
	 * @param json the JSON string to parse.
	 * @param parser the parser function.
	 * @return Map&lt;String, Object&gt; parsed contents
	 * @throws IllegalArgumentException if the json String cannot be parsed as a
	 * Map&lt;String, Object&gt;
	 */
	Map<String, Object> parseMap(String json,
			Function<String, Map<String, Object>> parser) {
		assert parser != null;

		return trimIfStartsWith(json, START_MAP).map(parser::apply)
				.orElseThrow(AbstractJsonParser::cannotParseJson);
	}

	/**
	 * Parses the specified JSON string and returns the extracted contents as a List of Objects.
	 *
	 * @param json the JSON string to parse.
	 * @param parser the parser function.
	 * @return List&lt;Object&gt; parsed contents
	 * @throws IllegalArgumentException if the json String cannot be parsed as a
	 * List&lt;Object&gt;
	 */
	List<Object> parseList(String json, Function<String, List<Object>> parser) {
		assert parser != null;

		return trimIfStartsWith(json, START_LIST).map(parser::apply)
				.orElseThrow(AbstractJsonParser::cannotParseJson);
	}

	private static IllegalArgumentException cannotParseJson() {
		return cannotParseJson(null);
	}

	static IllegalArgumentException cannotParseJson(Exception cause) {
		return new IllegalArgumentException("Cannot parse JSON", cause);
	}

	private static Optional<String> trimIfStartsWith(String json, String expectedPrefix) {
		assert expectedPrefix != null;

		if (json != null) {
			final String trimmed = json.trim();
			if (trimmed.startsWith(expectedPrefix)) {
				return Optional.of(trimmed);
			}
		}
		return Optional.empty();
	}

}
