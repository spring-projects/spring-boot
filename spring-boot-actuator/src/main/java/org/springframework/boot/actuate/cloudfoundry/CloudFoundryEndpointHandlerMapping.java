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

import java.util.Iterator;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.AbstractEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.HalJsonMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to Cloud Foundry specific URLs.
 *
 * @author Madhura Bhave
 */
class CloudFoundryEndpointHandlerMapping
		extends AbstractEndpointHandlerMapping<NamedMvcEndpoint> {

	CloudFoundryEndpointHandlerMapping(Set<? extends NamedMvcEndpoint> endpoints,
			CorsConfiguration corsConfiguration, HandlerInterceptor securityInterceptor) {
		super(endpoints, corsConfiguration);
		setSecurityInterceptor(securityInterceptor);
	}

	@Override
	protected void postProcessEndpoints(Set<NamedMvcEndpoint> endpoints) {
		super.postProcessEndpoints(endpoints);
		Iterator<NamedMvcEndpoint> iterator = endpoints.iterator();
		HealthMvcEndpoint healthMvcEndpoint = null;
		while (iterator.hasNext()) {
			NamedMvcEndpoint endpoint = iterator.next();
			if (endpoint instanceof HalJsonMvcEndpoint) {
				iterator.remove();
			}
			else if (endpoint instanceof HealthMvcEndpoint) {
				iterator.remove();
				healthMvcEndpoint = (HealthMvcEndpoint) endpoint;
			}
		}
		if (healthMvcEndpoint != null) {
			endpoints.add(
					new CloudFoundryHealthMvcEndpoint(healthMvcEndpoint.getDelegate()));
		}
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		detectHandlerMethods(new CloudFoundryDiscoveryMvcEndpoint(getEndpoints()));
	}

	@Override
	protected String getPath(MvcEndpoint endpoint) {
		if (endpoint instanceof NamedMvcEndpoint) {
			return "/" + ((NamedMvcEndpoint) endpoint).getName();
		}
		return super.getPath(endpoint);
	}

}
