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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collections;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;

/**
 * A discovered {@link ExposableControllerEndpoint controller endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredControllerEndpoint extends AbstractDiscoveredEndpoint<Operation>
		implements ExposableControllerEndpoint {

	private final String rootPath;

	/**
     * Constructs a new DiscoveredControllerEndpoint with the specified parameters.
     * 
     * @param discoverer the EndpointDiscoverer used to discover the endpoint
     * @param endpointBean the bean representing the endpoint
     * @param id the unique identifier for the endpoint
     * @param rootPath the root path for the endpoint
     * @param enabledByDefault indicates whether the endpoint is enabled by default
     */
    DiscoveredControllerEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id,
			String rootPath, boolean enabledByDefault) {
		super(discoverer, endpointBean, id, enabledByDefault, Collections.emptyList());
		this.rootPath = rootPath;
	}

	/**
     * Returns the controller object associated with this discovered controller endpoint.
     * 
     * @return the controller object
     */
    @Override
	public Object getController() {
		return getEndpointBean();
	}

	/**
     * Returns the root path of the controller endpoint.
     *
     * @return the root path of the controller endpoint
     */
    @Override
	public String getRootPath() {
		return this.rootPath;
	}

}
