/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import org.jspecify.annotations.Nullable;

/**
 * Test {@link JmxOperationResponseMapper} implementation.
 *
 * @author Stephane Nicoll
 */
class TestJmxOperationResponseMapper implements JmxOperationResponseMapper {

	@Override
	public @Nullable Object mapResponse(@Nullable Object response) {
		return response;
	}

	@Override
	public Class<?> mapResponseType(Class<?> responseType) {
		return responseType;
	}

}
