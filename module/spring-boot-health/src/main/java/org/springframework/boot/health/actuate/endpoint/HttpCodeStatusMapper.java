/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import org.springframework.boot.health.contributor.Status;

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
	 */
	HttpCodeStatusMapper DEFAULT = new SimpleHttpCodeStatusMapper();

	/**
	 * Return the HTTP status code that corresponds to the given {@link Status health
	 * status}.
	 * @param status the health status to map
	 * @return the corresponding HTTP status code
	 */
	int getStatusCode(Status status);

}
