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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.springframework.boot.actuate.autoconfigure.web.ManagementServerProperties;
import org.springframework.boot.endpoint.EndpointPathResolver;

/**
 * {@link EndpointPathResolver} implementation for resolving actuator endpoint paths based
 * on the endpoint id and management.context-path.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ManagementEndpointPathResolver implements EndpointPathResolver {

	private final String contextPath;

	public ManagementEndpointPathResolver(ManagementServerProperties properties) {
		this.contextPath = properties.getContextPath();
	}

	@Override
	public String resolvePath(String endpointId) {
		return this.contextPath + "/" + endpointId;
	}

}
