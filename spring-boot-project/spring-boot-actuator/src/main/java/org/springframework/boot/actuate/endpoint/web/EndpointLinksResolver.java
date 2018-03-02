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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;

/**
 * A resolver for {@link Link links} to web endpoints.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointLinksResolver {

	private final Collection<? extends ExposableEndpoint<?>> endpoints;

	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints) {
		this.endpoints = endpoints;
	}

	/**
	 * Resolves links to the known endpoints based on a request with the given
	 * {@code requestUrl}.
	 * @param requestUrl the url of the request for the endpoint links
	 * @return the links
	 */
	public Map<String, Link> resolveLinks(String requestUrl) {
		String normalizedUrl = normalizeRequestUrl(requestUrl);
		Map<String, Link> links = new LinkedHashMap<>();
		links.put("self", new Link(normalizedUrl));
		for (ExposableEndpoint<?> endpoint : this.endpoints) {
			if (endpoint instanceof ExposableWebEndpoint) {
				collectLinks(links, (ExposableWebEndpoint) endpoint, normalizedUrl);
			}
			else if (endpoint instanceof PathMappedEndpoint) {
				links.put(endpoint.getId(), createLink(normalizedUrl,
						((PathMappedEndpoint) endpoint).getRootPath()));
			}
		}
		return links;
	}

	private String normalizeRequestUrl(String requestUrl) {
		if (requestUrl.endsWith("/")) {
			return requestUrl.substring(0, requestUrl.length() - 1);
		}
		return requestUrl;
	}

	private void collectLinks(Map<String, Link> links, ExposableWebEndpoint endpoint,
			String normalizedUrl) {
		for (WebOperation operation : endpoint.getOperations()) {
			links.put(operation.getId(), createLink(normalizedUrl, operation));
		}
	}

	private Link createLink(String requestUrl, WebOperation operation) {
		return createLink(requestUrl, operation.getRequestPredicate().getPath());
	}

	private Link createLink(String requestUrl, String path) {
		return new Link(requestUrl + (path.startsWith("/") ? path : "/" + path));
	}

}
