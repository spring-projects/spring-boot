/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Thin wrapper to adapt Jackson 2 {@link ObjectMapper} to {@link JsonParser}.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see JsonParserFactory
 */
public class JacksonJsonParser extends AbstractJsonParser {

	private static final MapTypeReference MAP_TYPE = new MapTypeReference();

	private static final ListTypeReference LIST_TYPE = new ListTypeReference();

	private @Nullable ObjectMapper objectMapper; // Late binding

	/**
	 * Creates an instance with the specified {@link ObjectMapper}.
	 * @param objectMapper the object mapper to use
	 */
	public JacksonJsonParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Creates an instance with a default {@link ObjectMapper} that is created lazily.
	 */
	public JacksonJsonParser() {
	}

	@Override
	public Map<String, Object> parseMap(@Nullable String json) {
		return tryParse(() -> getObjectMapper().readValue(json, MAP_TYPE), Exception.class);
	}

	@Override
	public List<Object> parseList(@Nullable String json) {
		return tryParse(() -> getObjectMapper().readValue(json, LIST_TYPE), Exception.class);
	}

	private ObjectMapper getObjectMapper() {
		if (this.objectMapper == null) {
			this.objectMapper = new ObjectMapper();
		}
		return this.objectMapper;
	}

	private static final class MapTypeReference extends TypeReference<Map<String, Object>> {

	}

	private static final class ListTypeReference extends TypeReference<List<Object>> {

	}

}
