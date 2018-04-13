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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;

/**
 * A discovered {@link ExposableWebEndpoint web endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredWebEndpoint extends AbstractDiscoveredEndpoint<WebOperation>
		implements ExposableWebEndpoint {

	private final String rootPath;

	DiscoveredWebEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean,
			String id, String rootPath, boolean enabledByDefault,
			Collection<WebOperation> operations) {
		super(discoverer, endpointBean, id, enabledByDefault, operations);
		this.rootPath = rootPath;
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}
}
