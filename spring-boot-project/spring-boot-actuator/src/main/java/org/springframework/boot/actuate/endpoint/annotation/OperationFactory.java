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

package org.springframework.boot.actuate.endpoint.annotation;

import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInfo;

/**
 * Factory to creates an {@link Operation} for an annotated method on an
 * {@link Endpoint @Endpoint}.
 *
 * @param <T> the {@link Operation} type
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@FunctionalInterface
public interface OperationFactory<T extends Operation> {

	/**
	 * Creates an {@link Operation} for an operation on an endpoint.
	 * @param endpointId the id of the endpoint
	 * @param target the target that implements the operation
	 * @param methodInfo the method on the bean that implements the operation
	 * @param invoker the invoker that should be used for the operation
	 * @return the operation info that describes the operation
	 */
	T createOperation(String endpointId, OperationMethodInfo methodInfo, Object target,
			OperationInvoker invoker);

}
