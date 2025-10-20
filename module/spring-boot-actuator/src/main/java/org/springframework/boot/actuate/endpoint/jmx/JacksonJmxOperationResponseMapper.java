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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.lang.Contract;

/**
 * {@link JmxOperationResponseMapper} that delegates to a Jackson {@link JsonMapper} to
 * return a JSON response.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JacksonJmxOperationResponseMapper implements JmxOperationResponseMapper {

	private final JsonMapper jsonMapper;

	private final JavaType listType;

	private final JavaType mapType;

	public JacksonJmxOperationResponseMapper(@Nullable JsonMapper jsonMapper) {
		this.jsonMapper = (jsonMapper != null) ? jsonMapper : new JsonMapper();
		this.listType = this.jsonMapper.getTypeFactory().constructParametricType(List.class, Object.class);
		this.mapType = this.jsonMapper.getTypeFactory().constructParametricType(Map.class, String.class, Object.class);
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
	@Contract("!null -> !null")
	public @Nullable Object mapResponse(@Nullable Object response) {
		if (response == null) {
			return null;
		}
		if (response instanceof CharSequence) {
			return response.toString();
		}
		if (response.getClass().isArray() || response instanceof Collection) {
			return this.jsonMapper.convertValue(response, this.listType);
		}
		return this.jsonMapper.convertValue(response, this.mapType);
	}

}
