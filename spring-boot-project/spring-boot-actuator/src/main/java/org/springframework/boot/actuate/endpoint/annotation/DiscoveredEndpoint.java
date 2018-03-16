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

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;

/**
 * An {@link ExposableEndpoint endpoint} discovered by a {@link EndpointDiscoverer}.
 *
 * @param <O> The operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface DiscoveredEndpoint<O extends Operation> extends ExposableEndpoint<O> {

	/**
	 * Return {@code true} if the endpoint was discovered by the specified discoverer.
	 * @param discoverer the discoverer type
	 * @return {@code true} if discovered using the specified discoverer
	 */
	boolean wasDiscoveredBy(Class<? extends EndpointDiscoverer<?, ?>> discoverer);

	/**
	 * Return the source bean that was used to construct the {@link DiscoveredEndpoint}.
	 * @return the source endpoint bean
	 */
	Object getEndpointBean();

}
