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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.HashMap;
import java.util.Map;

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
	public String getRootPath(EndpointId endpointId) {
		String path = this.pathMapping.get(endpointId);
		return StringUtils.hasText(path) ? path : null;
	}

}
