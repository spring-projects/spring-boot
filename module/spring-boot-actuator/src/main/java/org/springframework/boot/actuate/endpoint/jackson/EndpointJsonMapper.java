/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jackson;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;

/**
 * Interface used to supply the {@link JsonMapper} that should be used when serializing
 * endpoint results.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see OperationResponseBody
 */
@FunctionalInterface
public interface EndpointJsonMapper {

	/**
	 * Return the {@link JsonMapper} that should be used to serialize
	 * {@link OperationResponseBody} endpoint results.
	 * @return the object mapper
	 */
	JsonMapper get();

}
