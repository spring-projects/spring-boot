/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * @return an potentially new operation invoker with support for additional features
	 * @since 2.0.6
	 */
	default OperationInvoker apply(EndpointId endpointId, OperationType operationType, OperationParameters parameters,
			OperationInvoker invoker) {
		return apply((endpointId != null) ? endpointId.toString() : null, operationType, parameters, invoker);
	}

	/**
	 * Apply additional functionality to the given invoker.
	 * @param endpointId the endpoint ID
	 * @param operationType the operation type
	 * @param parameters the operation parameters
	 * @param invoker the invoker to advise
	 * @return an potentially new operation invoker with support for additional features
	 * @deprecated since 2.0.6 in favor of
	 * {@link #apply(EndpointId, OperationType, OperationParameters, OperationInvoker)}
	 */
	@Deprecated
	OperationInvoker apply(String endpointId, OperationType operationType, OperationParameters parameters,
			OperationInvoker invoker);

}
