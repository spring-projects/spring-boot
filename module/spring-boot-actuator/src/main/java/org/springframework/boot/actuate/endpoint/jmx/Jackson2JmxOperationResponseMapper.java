/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * {@link JmxOperationResponseMapper} that delegates to a Jackson 2 {@link ObjectMapper}
 * to return a JSON response.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link JacksonJmxOperationResponseMapper}.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class Jackson2JmxOperationResponseMapper implements JmxOperationResponseMapper {

	private final ObjectMapper objectMapper;

	private final JavaType listType;

	private final JavaType mapType;

	public Jackson2JmxOperationResponseMapper(@Nullable ObjectMapper objectMapper) {
		this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
		this.listType = this.objectMapper.getTypeFactory().constructParametricType(List.class, Object.class);
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
	@Contract("!null -> !null")
	public @Nullable Object mapResponse(@Nullable Object response) {
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
