/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.boot.json.jackson.ObjectMapperProvider;
import org.springframework.boot.json.jackson.WrappedObjectMapperProvider;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin wrapper to adapt Jackson 2 {@link ObjectMapper} to {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Dan Paquette
 * @see JsonParserFactory
 */
public class JacksonJsonParser implements JsonParser {

	private final ObjectMapperProvider objectMapperProvider;

	/**
	 * Creates an instance that an out of the box {@link ObjectMapper}.
	 */
	public JacksonJsonParser() {
		this(WrappedObjectMapperProvider.createBasicProvider());
	}

	/**
	 * Creates an instance using a custom {@link ObjectMapperProvider}.
	 *
	 * @param objectMapperProvider
	 */
	public JacksonJsonParser(final ObjectMapperProvider objectMapperProvider) {
		Assert.notNull(objectMapperProvider, "ObjectMapperProvider must not be null");
		this.objectMapperProvider = objectMapperProvider;
	}

	/**
	 * Creates an instance using a custom {@link ObjectMapper}.
	 *
	 * @param objectMapper
	 */
	public JacksonJsonParser(final ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapperProvider = new WrappedObjectMapperProvider(objectMapper);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> parseMap(String json) {
		try {
			return this.objectMapperProvider.getObjectMapper().readValue(json, Map.class);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Cannot parse JSON", ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> parseList(String json) {
		try {
			return this.objectMapperProvider.getObjectMapper().readValue(json, List.class);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Cannot parse JSON", ex);
		}
	}

}
