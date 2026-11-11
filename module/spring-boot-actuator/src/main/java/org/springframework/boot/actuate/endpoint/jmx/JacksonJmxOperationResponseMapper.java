/*
 * Copyright 2012-present the original author or authors.
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
