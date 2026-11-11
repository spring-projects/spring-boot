/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.List;

import org.springframework.boot.actuate.endpoint.Operation;

/**
 * An operation on a JMX endpoint.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface JmxOperation extends Operation {

	/**
	 * Returns the name of the operation.
	 * @return the operation name
	 */
	String getName();

	/**
	 * Returns the type of the output of the operation.
	 * @return the output type
	 */
	Class<?> getOutputType();

	/**
	 * Returns the description of the operation.
	 * @return the operation description
	 */
	String getDescription();

	/**
	 * Returns the parameters the operation expects in the order that they should be
	 * provided.
	 * @return the operation parameter names
	 */
	List<JmxOperationParameter> getParameters();

}
