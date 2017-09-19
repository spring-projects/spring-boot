/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * An operation on a JMX endpoint.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxEndpointOperation extends Operation {

	private final String operationName;

	private final Class<?> outputType;

	private final String description;

	private final List<JmxEndpointOperationParameterInfo> parameters;

	/**
	 * Creates a new {@code JmxEndpointOperation} for an operation of the given
	 * {@code type}. The operation can be performed using the given {@code invoker}.
	 * @param type the type of the operation
	 * @param invoker used to perform the operation
	 * @param operationName the name of the operation
	 * @param outputType the type of the output of the operation
	 * @param description the description of the operation
	 * @param parameters the parameters of the operation
	 */
	public JmxEndpointOperation(OperationType type, OperationInvoker invoker,
			String operationName, Class<?> outputType, String description,
			List<JmxEndpointOperationParameterInfo> parameters) {
		super(type, invoker, true);
		this.operationName = operationName;
		this.outputType = outputType;
		this.description = description;
		this.parameters = parameters;
	}

	/**
	 * Returns the name of the operation.
	 * @return the operation name
	 */
	public String getOperationName() {
		return this.operationName;
	}

	/**
	 * Returns the type of the output of the operation.
	 * @return the output type
	 */
	public Class<?> getOutputType() {
		return this.outputType;
	}

	/**
	 * Returns the description of the operation.
	 * @return the operation description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Returns the parameters of the operation.
	 * @return the operation parameters
	 */
	public List<JmxEndpointOperationParameterInfo> getParameters() {
		return Collections.unmodifiableList(this.parameters);
	}

}
