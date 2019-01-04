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

package org.springframework.boot.actuate.endpoint.web;

import java.util.List;

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
	String getRootPath(EndpointId endpointId);

	/**
	 * Returns an {@link PathMapper} that uses the endpoint ID as the path.
	 * @return an {@link PathMapper} that uses the lowercase endpoint ID as the path
	 * @deprecated since 2.1.0 in favor of {@link #getRootPath(List, EndpointId)} with a
	 * {@code null} list
	 */
	@Deprecated
	static PathMapper useEndpointId() {
		return EndpointId::toString;
	}

	/**
	 * Resolve the root path for the specified {@code endpointId} from the given path
	 * mappers. If no mapper matches then the ID itself is returned.
	 * @param pathMappers the path mappers (may be {@code null})
	 * @param endpointId the id of an endpoint
	 * @return the path of the endpoint
	 */
	static String getRootPath(List<PathMapper> pathMappers, EndpointId endpointId) {
		Assert.notNull(endpointId, "EndpointId must not be null");
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
