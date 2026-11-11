/*
 * Copyright 2012-present the original author or authors.
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
		List<String> additionalPaths = mapper.getAdditionalPaths(getEndpointId(), webServerNamespace);
		return (additionalPaths != null) ? additionalPaths.stream() : Stream.empty();
	}

}
