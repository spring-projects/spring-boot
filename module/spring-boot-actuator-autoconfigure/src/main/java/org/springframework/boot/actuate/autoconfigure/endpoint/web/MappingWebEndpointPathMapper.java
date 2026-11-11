/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * A {@link PathMapper} implementation that uses a simple {@link Map} to determine the
 * endpoint path.
 *
 * @author Stephane Nicoll
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class MappingWebEndpointPathMapper implements PathMapper {

	private final Map<EndpointId, String> pathMapping;

	MappingWebEndpointPathMapper(Map<String, String> pathMapping) {
		this.pathMapping = new HashMap<>();
		pathMapping.forEach((id, path) -> this.pathMapping.put(EndpointId.fromPropertyValue(id), path));
	}

	@Override
	public @Nullable String getRootPath(EndpointId endpointId) {
		String path = this.pathMapping.get(endpointId);
		return StringUtils.hasText(path) ? path : null;
	}

}
