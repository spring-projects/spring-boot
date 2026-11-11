/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Allows additional functionality to be applied to an {@link OperationInvoker}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface OperationInvokerAdvisor {

	/**
	 * Apply additional functionality to the given invoker.
	 * @param endpointId the endpoint ID
	 * @param operationType the operation type
	 * @param parameters the operation parameters
	 * @param invoker the invoker to advise
	 * @return a potentially new operation invoker with support for additional features
	 */
	OperationInvoker apply(EndpointId endpointId, OperationType operationType, OperationParameters parameters,
			OperationInvoker invoker);

}
