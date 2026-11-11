/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import org.jspecify.annotations.Nullable;

/**
 * Maps an operation's response to a JMX-friendly form.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public interface JmxOperationResponseMapper {

	/**
	 * Map the response type to its JMX compliant counterpart.
	 * @param responseType the operation's response type
	 * @return the JMX compliant type
	 */
	Class<?> mapResponseType(Class<?> responseType);

	/**
	 * Map the operation's response so that it can be consumed by a JMX compliant client.
	 * @param response the operation's response
	 * @return the {@code response}, in a JMX compliant format
	 */
	@Nullable Object mapResponse(@Nullable Object response);

}
