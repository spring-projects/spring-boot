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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.AdditionalPathsMapper;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;

/**
 * A discovered {@link ExposableWebEndpoint web endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredWebEndpoint extends AbstractDiscoveredEndpoint<WebOperation> implements ExposableWebEndpoint {

	private final String rootPath;

	private Collection<AdditionalPathsMapper> additionalPathsMappers;

	DiscoveredWebEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, String rootPath,
			Access defaultAccess, Collection<WebOperation> operations,
			Collection<AdditionalPathsMapper> additionalPathsMappers) {
		super(discoverer, endpointBean, id, defaultAccess, operations);
		this.rootPath = rootPath;
		this.additionalPathsMappers = additionalPathsMappers;
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}

	@Override
	public List<String> getAdditionalPaths(WebServerNamespace webServerNamespace) {
		return this.additionalPathsMappers.stream()
			.flatMap((mapper) -> getAdditionalPaths(webServerNamespace, mapper))
			.toList();
	}

	private Stream<String> getAdditionalPaths(WebServerNamespace webServerNamespace, AdditionalPathsMapper mapper) {
		return mapper.getAdditionalPaths(getEndpointId(), webServerNamespace).stream();
	}

}
