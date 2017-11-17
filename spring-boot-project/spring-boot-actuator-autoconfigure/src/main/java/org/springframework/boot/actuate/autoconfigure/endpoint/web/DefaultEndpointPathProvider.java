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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.util.Assert;

/**
 * Default {@link EndpointPathProvider} implementation.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class DefaultEndpointPathProvider implements EndpointPathProvider {

	private final String basePath;

	private final EndpointDiscoverer<WebOperation> endpointDiscoverer;

	public DefaultEndpointPathProvider(
			EndpointDiscoverer<WebOperation> endpointDiscoverer,
			WebEndpointProperties webEndpointProperties) {
		this.endpointDiscoverer = endpointDiscoverer;
		this.basePath = webEndpointProperties.getBasePath();
	}

	@Override
	public List<String> getPaths() {
		return getEndpoints().map(this::getPath).collect(Collectors.toList());
	}

	@Override
	public String getPath(String id) {
		Assert.notNull(id, "ID must not be null");
		return getEndpoints().filter((info) -> id.equals(info.getId())).findFirst()
				.map(this::getPath).orElse(null);
	}

	private Stream<EndpointInfo<WebOperation>> getEndpoints() {
		return this.endpointDiscoverer.discoverEndpoints().stream();
	}

	private String getPath(EndpointInfo<WebOperation> endpointInfo) {
		return this.basePath + "/" + endpointInfo.getId();
	}

}
