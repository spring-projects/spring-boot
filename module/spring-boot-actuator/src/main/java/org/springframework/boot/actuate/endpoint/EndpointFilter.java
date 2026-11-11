/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

/**
 * Strategy class that can be used to filter {@link ExposableEndpoint endpoints}.
 *
 * @param <E> the endpoint type
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface EndpointFilter<E extends ExposableEndpoint<?>> {

	/**
	 * Return {@code true} if the filter matches.
	 * @param endpoint the endpoint to check
	 * @return {@code true} if the filter matches
	 */
	boolean match(E endpoint);

}
