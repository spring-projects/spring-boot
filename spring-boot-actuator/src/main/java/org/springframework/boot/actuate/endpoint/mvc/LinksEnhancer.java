/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 *
 */
public class LinksEnhancer {

	private MvcEndpoints endpoints;

	private String rootPath;

	public LinksEnhancer(MvcEndpoints endpoints, String rootPath) {
		this.endpoints = endpoints;
		this.rootPath = rootPath;
	}

	public void addEndpointLinks(ResourceSupport resource, String self) {
		if (!resource.hasLink("self")) {
			resource.add(linkTo(LinksEnhancer.class).slash(
					this.rootPath + self).withSelfRel());
		}
		for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
			if (endpoint.getPath().equals(self)) {
				continue;
			}
			Class<?> type = endpoint.getEndpointType();
			if (type == null) {
				type = Object.class;
			}
			String path = endpoint.getPath();
			String rel = path.startsWith("/") ? path.substring(1) : path;
			if (StringUtils.hasText(rel)) {
				resource.add(linkTo(type).slash(this.rootPath + endpoint.getPath())
						.withRel(rel));
			}
		}
	}

}
