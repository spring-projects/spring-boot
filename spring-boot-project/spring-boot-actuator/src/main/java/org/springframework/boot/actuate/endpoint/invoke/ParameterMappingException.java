/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;

/**
 * A {@code ParameterMappingException} is thrown when a failure occurs during
 * {@link ParameterValueMapper#mapParameterValue operation parameter mapping}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class ParameterMappingException extends InvalidEndpointRequestException {

	private final OperationParameter parameter;

	private final Object value;

	/**
	 * Creates a new {@code ParameterMappingException} for a failure that occurred when
	 * trying to map the given {@code input} to the given {@code type}.
	 * @param parameter the parameter being mapping
	 * @param value the value being mapped
	 * @param cause the cause of the mapping failure
	 */
	public ParameterMappingException(OperationParameter parameter, Object value,
			Throwable cause) {
		super("Failed to map " + value + " of type " + value.getClass() + " to "
				+ parameter, "Parameter mapping failure", cause);
		this.parameter = parameter;
		this.value = value;
	}

	/**
	 * Return the parameter being mapped.
	 * @return the parameter
	 */
	public OperationParameter getParameter() {
		return this.parameter;
	}

	/**
	 * Return the value being mapped.
	 * @return the value
	 */
	public Object getValue() {
		return this.value;
	}

}
