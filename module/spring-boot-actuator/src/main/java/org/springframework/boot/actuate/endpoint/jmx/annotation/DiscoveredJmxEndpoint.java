/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;

/**
 * A discovered {@link ExposableJmxEndpoint JMX endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredJmxEndpoint extends AbstractDiscoveredEndpoint<JmxOperation> implements ExposableJmxEndpoint {

	DiscoveredJmxEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<JmxOperation> operations) {
		super(discoverer, endpointBean, id, defaultAccess, operations);
	}

}
