/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.lang.Contract;

/**
 * Tagging interface used to indicate that an operation result is intended to be returned
 * in the body of the response. Primarily intended to support JSON serialization using an
 * endpoint specific {@link JsonMapper}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
public interface OperationResponseBody {

	/**
	 * Return a {@link OperationResponseBody} {@link Map} instance containing entries from
	 * the given {@code map}.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the source map or {@code null}
	 * @return a {@link OperationResponseBody} version of the map or {@code null}
	 */
	@Contract("!null -> !null")
	static <K, V> @Nullable Map<K, V> of(@Nullable Map<K, V> map) {
		return (map != null) ? new OperationResponseBodyMap<>(map) : null;
	}

}
