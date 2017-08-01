/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getId()}.
 * The semantics of {@code @RequestMapping} should be identical to a normal
 * {@code @Controller}, but the endpoints should not be annotated as {@code @Controller}
 * (otherwise they will be mapped by the normal MVC mechanisms).
 * <p>
 * One of the aims of the mapping is to support endpoints that work as HTTP endpoints but
 * can still provide useful service interfaces when there is no HTTP server (and no Spring
 * MVC on the classpath). Note that any endpoints having method signatures will break in a
 * non-servlet environment.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Dave Syer
 */
public class EndpointHandlerMapping extends AbstractEndpointHandlerMapping<MvcEndpoint> {

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}. The endpoints will not accept CORS
	 * requests.
	 * @param endpoints the endpoints
	 */
	public EndpointHandlerMapping(Collection<? extends MvcEndpoint> endpoints) {
		super(endpoints);
	}

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}. The endpoints will accepts CORS
	 * requests based on the given {@code corsConfiguration}.
	 * @param endpoints the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 * @since 1.3.0
	 */
	public EndpointHandlerMapping(Collection<? extends MvcEndpoint> endpoints,
			CorsConfiguration corsConfiguration) {
		super(endpoints, corsConfiguration);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		detectHandlerMethods(new EndpointLinksMvcEndpoint(
				getEndpoints().stream().filter(NamedMvcEndpoint.class::isInstance)
						.map(NamedMvcEndpoint.class::cast).collect(Collectors.toSet())));
	}

	/**
	 * {@link MvcEndpoint} to provide HAL-formatted links to all the
	 * {@link NamedMvcEndpoint named endpoints}.
	 *
	 * @author Madhura Bhave
	 * @author Andy Wilkinson
	 */
	private static final class EndpointLinksMvcEndpoint extends AbstractMvcEndpoint {

		private final Set<NamedMvcEndpoint> endpoints;

		private EndpointLinksMvcEndpoint(Set<NamedMvcEndpoint> endpoints) {
			super("");
			this.endpoints = endpoints;
		}

		@ResponseBody
		@ActuatorGetMapping
		public Map<String, Map<String, Link>> links(HttpServletRequest request) {
			Map<String, Link> links = new LinkedHashMap<>();
			String url = request.getRequestURL().toString();
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			links.put("self", Link.withHref(url));
			for (NamedMvcEndpoint endpoint : this.endpoints) {
				links.put(endpoint.getName(),
						Link.withHref(url + "/" + endpoint.getName()));
			}
			return Collections.singletonMap("_links", links);
		}

	}

	/**
	 * Details for a link in the HAL response.
	 */
	static final class Link {

		private final String href;

		private Link(String href) {
			this.href = href;
		}

		public String getHref() {
			return this.href;
		}

		static Link withHref(Object href) {
			return new Link(href.toString());
		}

	}

}
