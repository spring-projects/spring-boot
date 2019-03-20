/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.mvc.AbstractMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link MvcEndpoint} to expose HAL-formatted JSON for Cloud Foundry specific actuator
 * endpoints.
 *
 * @author Madhura Bhave
 */
class CloudFoundryDiscoveryMvcEndpoint extends AbstractMvcEndpoint {

	private final Set<NamedMvcEndpoint> endpoints;

	CloudFoundryDiscoveryMvcEndpoint(Set<NamedMvcEndpoint> endpoints) {
		super("", false);
		this.endpoints = endpoints;
	}

	@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Map<String, Link>> links(HttpServletRequest request) {
		Map<String, Link> links = new LinkedHashMap<String, Link>();
		String url = request.getRequestURL().toString();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		links.put("self", Link.withHref(url));
		AccessLevel accessLevel = AccessLevel.get(request);
		for (NamedMvcEndpoint endpoint : this.endpoints) {
			if (accessLevel != null && accessLevel.isAccessAllowed(endpoint.getPath())) {
				links.put(endpoint.getName(),
						Link.withHref(url + "/" + endpoint.getName()));
			}
		}
		return Collections.singletonMap("_links", links);
	}

	/**
	 * Details for a link in the HAL response.
	 */
	static class Link {

		private String href;

		public String getHref() {
			return this.href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		static Link withHref(Object href) {
			Link link = new Link();
			link.setHref(href.toString());
			return link;
		}

	}

}
