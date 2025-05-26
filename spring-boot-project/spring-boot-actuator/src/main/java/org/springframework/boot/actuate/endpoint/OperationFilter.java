/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

/**
 * Strategy class that can be used to filter {@link Operation operations}.
 *
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @since 3.4.0
 */
@FunctionalInterface
public interface OperationFilter<O extends Operation> {

	/**
	 * Return {@code true} if the filter matches.
	 * @param endpointId the ID of the endpoint to which the operation belongs
	 * @param operation the operation to check
	 * @param defaultAccess the default permitted level of access to the endpoint
	 * @return {@code true} if the filter matches
	 */
	boolean match(O operation, EndpointId endpointId, Access defaultAccess);

	/**
	 * Return an {@link OperationFilter} that filters based on the allowed {@link Access
	 * access} as determined by an {@link EndpointAccessResolver access resolver}.
	 * @param <O> the operation type
	 * @param accessResolver the access resolver
	 * @return a new {@link OperationFilter}
	 */
	static <O extends Operation> OperationFilter<O> byAccess(EndpointAccessResolver accessResolver) {
		return (operation, endpointId, defaultAccess) -> {
			Access access = accessResolver.accessFor(endpointId, defaultAccess);
			return switch (access) {
				case NONE -> false;
				case READ_ONLY -> operation.getType() == OperationType.READ;
				case UNRESTRICTED -> true;
			};
		};
	}

}
