/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Strategy interface used to provide a mapping between an endpoint ID and the root path
 * where it will be exposed.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface PathMapper {

	/**
	 * Resolve the root path for the specified {@code endpointId}.
	 * @param endpointId the id of an endpoint
	 * @return the path of the endpoint or {@code null} if this mapper doesn't support the
	 * given endpoint ID
	 */
	@Nullable String getRootPath(EndpointId endpointId);

	/**
	 * Resolve the root path for the specified {@code endpointId} from the given path
	 * mappers. If no mapper matches then the ID itself is returned.
	 * @param pathMappers the path mappers (may be {@code null})
	 * @param endpointId the id of an endpoint
	 * @return the path of the endpoint
	 */
	static String getRootPath(@Nullable List<PathMapper> pathMappers, EndpointId endpointId) {
		Assert.notNull(endpointId, "'endpointId' must not be null");
		if (pathMappers != null) {
			for (PathMapper mapper : pathMappers) {
				String path = mapper.getRootPath(endpointId);
				if (StringUtils.hasText(path)) {
					return path;
				}
			}
		}
		return endpointId.toString();
	}

}
