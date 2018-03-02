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

package org.springframework.boot.actuate.endpoint.annotation;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
import org.springframework.core.style.ToStringCreator;

/**
 * Abstract base class for {@link Operation endpoints operations} discovered by a
 * {@link EndpointDiscoverer}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AbstractDiscoveredOperation implements Operation {

	private final OperationMethod operationMethod;

	private final OperationInvoker invoker;

	/**
	 * Create a new {@link AbstractDiscoveredOperation} instance.
	 * @param operationMethod the method backing the operation
	 * @param invoker the operation invoker to use
	 */
	public AbstractDiscoveredOperation(DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		this.operationMethod = operationMethod;
		this.invoker = invoker;
	}

	public OperationMethod getOperationMethod() {
		return this.operationMethod;
	}

	@Override
	public OperationType getType() {
		return this.operationMethod.getOperationType();
	}

	@Override
	public Object invoke(InvocationContext context) {
		return this.invoker.invoke(context);
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this)
				.append("operationMethod", this.operationMethod)
				.append("invoker", this.invoker);
		appendFields(creator);
		return creator.toString();
	}

	protected void appendFields(ToStringCreator creator) {
	}

}
