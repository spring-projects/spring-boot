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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.util.ReflectionUtils;

/**
 * Base class for parsers wrapped or implemented in this package.
 *
 * @author Anton Telechev
 * @author Phillip Webb
 * @since 2.0.1
 */
public abstract class AbstractJsonParser implements JsonParser {

	/**
     * Parses a JSON string into a Map using the provided parser function.
     * 
     * @param json the JSON string to parse
     * @param parser the function used to parse each JSON object
     * @return a Map representing the parsed JSON
     */
    protected final Map<String, Object> parseMap(String json, Function<String, Map<String, Object>> parser) {
		return trimParse(json, "{", parser);
	}

	/**
     * Parses a JSON string into a list of objects.
     * 
     * @param json the JSON string to parse
     * @param parser the function used to parse each element in the list
     * @return the parsed list of objects
     */
    protected final List<Object> parseList(String json, Function<String, List<Object>> parser) {
		return trimParse(json, "[", parser);
	}

	/**
     * Trims the given JSON string, checks if it starts with the specified prefix, and parses it using the provided parser function.
     * 
     * @param json    the JSON string to be parsed (can be null)
     * @param prefix  the prefix that the JSON string should start with
     * @param parser  the function used to parse the trimmed JSON string
     * @param <T>     the type of the parsed result
     * @return        the parsed result of type T
     * @throws JsonParseException if the trimmed JSON string does not start with the specified prefix
     */
    protected final <T> T trimParse(String json, String prefix, Function<String, T> parser) {
		String trimmed = (json != null) ? json.trim() : "";
		if (trimmed.startsWith(prefix)) {
			return parser.apply(trimmed);
		}
		throw new JsonParseException();
	}

	/**
     * Tries to parse a value using the provided parser and handles any exceptions that occur.
     * 
     * @param parser the parser to use for parsing the value
     * @param check the exception class to check against
     * @return the parsed value
     * @throws JsonParseException if the parsing fails with an exception of the specified class
     * @throws IllegalStateException if the parsing fails with an exception other than the specified class
     */
    protected final <T> T tryParse(Callable<T> parser, Class<? extends Exception> check) {
		try {
			return parser.call();
		}
		catch (Exception ex) {
			if (check.isAssignableFrom(ex.getClass())) {
				throw new JsonParseException(ex);
			}
			ReflectionUtils.rethrowRuntimeException(ex);
			throw new IllegalStateException(ex);
		}
	}

}
