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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * An {@link OperationFilter} that filters based on the allowed {@link Access access} as
 * determined by an {@link EndpointAccessResolver access resolver}.
 *
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public class EndpointAccessOperationFilter<O extends Operation> implements OperationFilter<O> {

	private final EndpointAccessResolver accessResolver;

	public EndpointAccessOperationFilter(EndpointAccessResolver accessResolver) {
		this.accessResolver = accessResolver;
	}

	@Override
	public boolean match(O operation, EndpointId endpointId, Access defaultAccess) {
		Access access = this.accessResolver.accessFor(endpointId, defaultAccess);
		return switch (access) {
			case NONE -> false;
			case READ_ONLY -> operation.getType() == OperationType.READ;
			case UNRESTRICTED -> true;
		};
	}

}
