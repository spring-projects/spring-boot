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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;

/**
 * Information describing an endpoint that can be exposed in some technology specific way.
 *
 * @param <O> the type of the endpoint's operations
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface ExposableEndpoint<O extends Operation> {

	/**
	 * Returns the id of the endpoint.
	 * @return the id
	 * @deprecated since 2.0.6 in favor of {@link #getEndpointId()}
	 */
	@Deprecated
	String getId();

	/**
	 * Return the endpoint ID.
	 * @return the endpoint ID
	 * @since 2.0.6
	 */
	default EndpointId getEndpointId() {
		return EndpointId.of(getId());
	}

	/**
	 * Returns if the endpoint is enabled by default.
	 * @return if the endpoint is enabled by default
	 */
	boolean isEnableByDefault();

	/**
	 * Returns the operations of the endpoint.
	 * @return the operations
	 */
	Collection<O> getOperations();

}
