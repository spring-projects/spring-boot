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

package org.springframework.boot.endpoint;

/**
 * An operation on an endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointOperation {

	private final EndpointOperationType type;

	private final OperationInvoker operationInvoker;

	private final boolean blocking;

	/**
	 * Creates a new {@code EndpointOperation} for an operation of the given {@code type}.
	 * The operation can be performed using the given {@code operationInvoker}.
	 * @param type the type of the operation
	 * @param operationInvoker used to perform the operation
	 * @param blocking whether or not this is a blocking operation
	 */
	public EndpointOperation(EndpointOperationType type,
			OperationInvoker operationInvoker, boolean blocking) {
		this.type = type;
		this.operationInvoker = operationInvoker;
		this.blocking = blocking;
	}

	/**
	 * Returns the {@link EndpointOperationType type} of the operation.
	 * @return the type
	 */
	public EndpointOperationType getType() {
		return this.type;
	}

	/**
	 * Returns the {@code OperationInvoker} that can be used to invoke this endpoint
	 * operation.
	 * @return the operation invoker
	 */
	public OperationInvoker getOperationInvoker() {
		return this.operationInvoker;
	}

	/**
	 * Whether or not this is a blocking operation.
	 *
	 * @return {@code true} if it is a blocking operation, otherwise {@code false}.
	 */
	public boolean isBlocking() {
		return this.blocking;
	}

}
