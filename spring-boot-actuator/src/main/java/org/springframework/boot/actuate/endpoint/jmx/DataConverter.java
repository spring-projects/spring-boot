/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Internal converter that uses an {@link ObjectMapper} to convert to JSON.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DataConverter {

	private final ObjectMapper objectMapper;

	private final JavaType listObject;

	private final JavaType mapStringObject;

	DataConverter(ObjectMapper objectMapper) {
		this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
		this.listObject = this.objectMapper.getTypeFactory()
				.constructParametricType(List.class, Object.class);
		this.mapStringObject = this.objectMapper.getTypeFactory()
				.constructParametricType(Map.class, String.class, Object.class);

	}

	public Object convert(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return data;
		}
		if (data.getClass().isArray() || data instanceof List) {
			return this.objectMapper.convertValue(data, this.listObject);
		}
		return this.objectMapper.convertValue(data, this.mapStringObject);
	}

}
