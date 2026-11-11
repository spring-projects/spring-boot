/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Parser that can read JSON formatted strings into {@link Map}s or {@link List}s.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see JsonParserFactory
 * @see BasicJsonParser
 * @see JacksonJsonParser
 * @see GsonJsonParser
 */
public interface JsonParser {

	/**
	 * Parse the specified JSON string into a Map.
	 * @param json the JSON to parse
	 * @return the parsed JSON as a map
	 * @throws JsonParseException if the JSON cannot be parsed
	 */
	Map<String, Object> parseMap(@Nullable String json) throws JsonParseException;

	/**
	 * Parse the specified JSON string into a List.
	 * @param json the JSON to parse
	 * @return the parsed JSON as a list
	 * @throws JsonParseException if the JSON cannot be parsed
	 */
	List<Object> parseList(@Nullable String json) throws JsonParseException;

}
