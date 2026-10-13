/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.util.CollectionUtils;

/**
 * Strategy used to map a {@link Status health status} to an HTTP status code.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface HttpCodeStatusMapper {

	/**
	 * An {@link HttpCodeStatusMapper} instance using default mappings.
	 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of #getDefault()
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "4.1.0", forRemoval = true)
	HttpCodeStatusMapper DEFAULT = new SimpleHttpCodeStatusMapper();

	/**
	 * Return the HTTP status code that corresponds to the given {@link Status health
	 * status}.
	 * @param status the health status to map
	 * @return the corresponding HTTP status code
	 */
	int getStatusCode(Status status);

	/**
	 * Create a new {@link HttpCodeStatusMapper} with the specified mappings.
	 * @param mappings the mappings to use or {@code null} to use the default mappings
	 * @return a {@link HttpCodeStatusMapper} or {@link #getDefault()}
	 * @since 4.1.0
	 */
	@SuppressWarnings("removal")
	static HttpCodeStatusMapper of(@Nullable Map<String, Integer> mappings) {
		return CollectionUtils.isEmpty(mappings) ? SimpleHttpCodeStatusMapper.DEFAULT_MAPPINGS
				: new SimpleHttpCodeStatusMapper(mappings);
	}

	/**
	 * Return an {@link HttpCodeStatusMapper} instance using default mappings.
	 * @return a mapper using default mappings
	 * @since 4.1.0
	 */
	@SuppressWarnings("removal")
	static HttpCodeStatusMapper getDefault() {
		return SimpleHttpCodeStatusMapper.DEFAULT_MAPPINGS;
	}

}
