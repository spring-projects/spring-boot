/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.actuator.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;

class MyExtensionWebMvcEndpointHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private static final String PATH = "/myextension";

	private final EndpointLinksResolver linksResolver;

	MyExtensionWebMvcEndpointHandlerMapping(Collection<ExposableWebEndpoint> endpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration) {
		super(new EndpointMapping(PATH), endpoints, endpointMediaTypes, corsConfiguration, true);
		this.linksResolver = new EndpointLinksResolver(endpoints, PATH);
		setOrder(-100);
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return new WebMvcLinksHandler();
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		super.extendInterceptors(interceptors);
		interceptors.add(0, new MyExtensionSecurityInterceptor());
	}

	class WebMvcLinksHandler implements LinksHandler {

		@Override
		@ResponseBody
		public Map<String, Map<String, Link>> links(HttpServletRequest request, HttpServletResponse response) {
			return Collections.singletonMap("_links", MyExtensionWebMvcEndpointHandlerMapping.this.linksResolver
				.resolveLinks(request.getRequestURL().toString()));
		}

		@Override
		public String toString() {
			return "Actuator extension root web endpoint";
		}

	}

}
