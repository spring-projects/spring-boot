/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import org.jspecify.annotations.Nullable;

/**
 * Maps parameter values to the required type when invoking an endpoint.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@FunctionalInterface
public interface ParameterValueMapper {

	/**
	 * A {@link ParameterValueMapper} that does nothing.
	 */
	ParameterValueMapper NONE = (parameter, value) -> value;

	/**
	 * Map the specified {@code input} parameter to the given {@code parameterType}.
	 * @param parameter the parameter to map
	 * @param value a parameter value
	 * @return a value suitable for that parameter
	 * @throws ParameterMappingException when a mapping failure occurs
	 */
	@Nullable Object mapParameterValue(OperationParameter parameter, @Nullable Object value) throws ParameterMappingException;

}
