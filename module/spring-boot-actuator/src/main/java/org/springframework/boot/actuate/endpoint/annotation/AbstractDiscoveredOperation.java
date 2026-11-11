/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import org.jspecify.annotations.Nullable;

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
	public AbstractDiscoveredOperation(DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
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
	public @Nullable Object invoke(InvocationContext context) {
		return this.invoker.invoke(context);
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this).append("operationMethod", this.operationMethod)
			.append("invoker", this.invoker);
		appendFields(creator);
		return creator.toString();
	}

	protected void appendFields(ToStringCreator creator) {
	}

}
