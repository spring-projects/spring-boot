/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;

/**
 * Interface used to supply the Jackson 2 {@link ObjectMapper} that should be used when
 * serializing endpoint results.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see OperationResponseBody
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
 */
@FunctionalInterface
@Deprecated(since = "4.0.0", forRemoval = true)
public interface EndpointJackson2ObjectMapper {

	/**
	 * Return the {@link JsonMapper} that should be used to serialize
	 * {@link OperationResponseBody} endpoint results.
	 * @return the object mapper
	 */
	ObjectMapper get();

}
