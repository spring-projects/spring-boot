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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointProvider;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.util.Assert;

/**
 * Default {@link EndpointPathProvider} implementation.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class DefaultEndpointPathProvider implements EndpointPathProvider {

	private final Collection<EndpointInfo<WebEndpointOperation>> endpoints;

	private final String contextPath;

	public DefaultEndpointPathProvider(EndpointProvider<WebEndpointOperation> provider,
			ManagementServerProperties managementServerProperties) {
		this.endpoints = provider.getEndpoints();
		this.contextPath = managementServerProperties.getContextPath();
	}

	@Override
	public List<String> getPaths() {
		return this.endpoints.stream().map(this::getPath).collect(Collectors.toList());
	}

	@Override
	public String getPath(String id) {
		Assert.notNull(id, "ID must not be null");
		return this.endpoints.stream().filter((info) -> id.equals(info.getId()))
				.findFirst().map(this::getPath).orElse(null);
	}

	private String getPath(EndpointInfo<WebEndpointOperation> endpointInfo) {
		return this.contextPath + "/" + endpointInfo.getId();
	}

}
