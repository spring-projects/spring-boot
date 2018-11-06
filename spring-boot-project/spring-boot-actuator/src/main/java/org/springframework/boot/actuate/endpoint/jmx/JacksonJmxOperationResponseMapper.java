/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link JmxOperationResponseMapper} that delegates to a Jackson {@link ObjectMapper} to
 * return a JSON response.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JacksonJmxOperationResponseMapper implements JmxOperationResponseMapper {

	private final ObjectMapper objectMapper;

	private final JavaType listType;

	private final JavaType mapType;

	public JacksonJmxOperationResponseMapper(ObjectMapper objectMapper) {
		this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
		this.listType = this.objectMapper.getTypeFactory()
				.constructParametricType(List.class, Object.class);
		this.mapType = this.objectMapper.getTypeFactory()
				.constructParametricType(Map.class, String.class, Object.class);
	}

	@Override
	public Class<?> mapResponseType(Class<?> responseType) {
		if (CharSequence.class.isAssignableFrom(responseType)) {
			return String.class;
		}
		if (responseType.isArray() || Collection.class.isAssignableFrom(responseType)) {
			return List.class;
		}
		return Map.class;
	}

	@Override
	public Object mapResponse(Object response) {
		if (response == null) {
			return null;
		}
		if (response instanceof CharSequence) {
			return response.toString();
		}
		if (response.getClass().isArray() || response instanceof Collection) {
			return this.objectMapper.convertValue(response, this.listType);
		}
		return this.objectMapper.convertValue(response, this.mapType);
	}

}
