/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.health.contributor.Status;
import org.springframework.lang.Contract;
import org.springframework.util.CollectionUtils;

/**
 * Simple {@link HttpCodeStatusMapper} backed by map of {@link Status#getCode() status
 * code} to HTTP status code.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
 * {@link HttpCodeStatusMapper#of}
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class SimpleHttpCodeStatusMapper implements HttpCodeStatusMapper {

	static final SimpleHttpCodeStatusMapper DEFAULT_MAPPINGS;
	static {
		Map<String, Integer> mappings = new HashMap<>();
		mappings.put(Status.DOWN.getCode(), WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		mappings.put(Status.OUT_OF_SERVICE.getCode(), WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		DEFAULT_MAPPINGS = new SimpleHttpCodeStatusMapper(mappings);
	}

	private final Map<String, Integer> mappings;

	/**
	 * Create a new {@link SimpleHttpCodeStatusMapper} instance using default mappings.
	 */
	public SimpleHttpCodeStatusMapper() {
		this((Map<String, Integer>) null);
	}

	/**
	 * Create a new {@link SimpleHttpCodeStatusMapper} with the specified mappings.
	 * @param mappings the mappings to use or {@code null} to use the default mappings
	 */
	public SimpleHttpCodeStatusMapper(@Nullable Map<String, Integer> mappings) {
		this.mappings = CollectionUtils.isEmpty(mappings) ? DEFAULT_MAPPINGS.mappings : getUniformMappings(mappings);
	}

	@Override
	public int getStatusCode(Status status) {
		String code = getUniformCode(status.getCode());
		return this.mappings.getOrDefault(code, WebEndpointResponse.STATUS_OK);
	}

	private static Map<String, Integer> getUniformMappings(Map<String, Integer> mappings) {
		Map<String, Integer> result = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
			String code = getUniformCode(entry.getKey());
			if (code != null) {
				result.putIfAbsent(code, entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	@Contract("!null -> !null")
	private static @Nullable String getUniformCode(@Nullable String code) {
		return (code != null) ? code.codePoints()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString() : null;
	}

}
