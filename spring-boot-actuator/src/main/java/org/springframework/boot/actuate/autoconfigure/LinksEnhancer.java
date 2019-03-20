/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.List;

import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Adds endpoint links to {@link ResourceSupport}.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 */
class LinksEnhancer {

	private final String rootPath;

	private final MvcEndpoints endpoints;

	LinksEnhancer(String rootPath, MvcEndpoints endpoints) {
		this.rootPath = rootPath;
		this.endpoints = endpoints;
	}

	public void addEndpointLinks(ResourceSupport resource, String self) {
		if (!resource.hasLink("self")) {
			resource.add(linkTo(LinksEnhancer.class).slash(this.rootPath + self)
					.withSelfRel());
		}
		MultiValueMap<String, String> added = new LinkedMultiValueMap<String, String>();
		for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
			if (!endpoint.getPath().equals(self)) {
				String rel = getRel(endpoint);
				List<String> paths = added.get(rel);
				if (paths == null || !paths.contains(endpoint.getPath())) {
					addEndpointLink(resource, endpoint, rel);
					added.add(rel, endpoint.getPath());
				}
			}
		}
	}

	private String getRel(MvcEndpoint endpoint) {
		if (endpoint instanceof NamedMvcEndpoint) {
			return ((NamedMvcEndpoint) endpoint).getName();
		}
		String path = endpoint.getPath();
		return (path.startsWith("/") ? path.substring(1) : path);
	}

	private void addEndpointLink(ResourceSupport resource, MvcEndpoint endpoint,
			String rel) {
		Class<?> type = endpoint.getEndpointType();
		type = (type != null) ? type : Object.class;
		if (StringUtils.hasText(rel)) {
			String href = this.rootPath + endpoint.getPath();
			resource.add(linkTo(type).slash(href).withRel(rel));
		}
	}

}
