/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import java.util.stream.Stream;

/**
 * A collection of {@link OperationParameter operation parameters}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface OperationParameters extends Iterable<OperationParameter> {

	/**
	 * Return {@code true} if there is at least one parameter.
	 * @return if there are parameters
	 */
	default boolean hasParameters() {
		return getParameterCount() > 0;
	}

	/**
	 * Return the total number of parameters.
	 * @return the total number of parameters
	 */
	int getParameterCount();

	/**
	 * Return if any of the contained parameters are
	 * {@link OperationParameter#isMandatory() mandatory}.
	 * @return if any parameters are mandatory
	 */
	default boolean hasMandatoryParameter() {
		return stream().anyMatch(OperationParameter::isMandatory);
	}

	/**
	 * Return the parameter at the specified index.
	 * @param index the parameter index
	 * @return the parameter
	 */
	OperationParameter get(int index);

	/**
	 * Return a stream of the contained parameters.
	 * @return a stream of the parameters
	 */
	Stream<OperationParameter> stream();

}
