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

package org.springframework.boot.docker.compose.core;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Support class used to handle JSON returned from the {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
final class DockerJson {

	private static final ObjectMapper objectMapper = JsonMapper.builder()
		.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.addModule(new ParameterNamesModule())
		.build();

	private DockerJson() {
	}

	/**
	 * Deserialize JSON to a list. Handles JSON arrays and multiple JSON objects in
	 * separate lines.
	 * @param <T> the item type
	 * @param json the source JSON
	 * @param itemType the item type
	 * @return a list of items
	 */
	static <T> List<T> deserializeToList(String json, Class<T> itemType) {
		if (json.startsWith("[")) {
			JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
			return deserialize(json, javaType);
		}
		return json.trim().lines().map((line) -> deserialize(line, itemType)).toList();
	}

	/**
	 * Deserialize JSON to an object instance.
	 * @param <T> the result type
	 * @param json the source JSON
	 * @param type the result type
	 * @return the deserialized result
	 */
	static <T> T deserialize(String json, Class<T> type) {
		return deserialize(json, objectMapper.getTypeFactory().constructType(type));
	}

	private static <T> T deserialize(String json, JavaType type) {
		try {
			return objectMapper.readValue(json.trim(), type);
		}
		catch (IOException ex) {
			throw new DockerOutputParseException(json, ex);
		}
	}

}
