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

package org.springframework.boot.actuate.endpoint.web.annotation;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.WebOperation;

/**
 * {@link EndpointFilter} for endpoints discovered by
 * {@link WebAnnotationEndpointDiscoverer}.
 *
 * @author Phillip Webb
 */
class WebEndpointFilter implements EndpointFilter<WebOperation> {

	@Override
	public boolean match(EndpointInfo<WebOperation> info,
			EndpointDiscoverer<WebOperation> discoverer) {
		return (discoverer instanceof WebAnnotationEndpointDiscoverer);
	}

}
