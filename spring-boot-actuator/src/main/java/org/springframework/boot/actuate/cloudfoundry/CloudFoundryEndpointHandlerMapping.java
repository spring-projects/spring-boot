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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.AbstractEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.HalJsonMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to Cloud Foundry specific URLs.
 *
 * @author Madhura Bhave
 */
class CloudFoundryEndpointHandlerMapping
		extends AbstractEndpointHandlerMapping<NamedMvcEndpoint> {

	private final HandlerInterceptor securityInterceptor;

	private final CorsConfiguration corsConfiguration;

	CloudFoundryEndpointHandlerMapping(Set<? extends NamedMvcEndpoint> endpoints,
			CorsConfiguration corsConfiguration, HandlerInterceptor securityInterceptor) {
		super(endpoints, corsConfiguration);
		this.securityInterceptor = securityInterceptor;
		this.corsConfiguration = corsConfiguration;
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

	@Override
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler,
			HttpServletRequest request) {
		HandlerExecutionChain chain = super.getHandlerExecutionChain(handler, request);
		HandlerInterceptor[] interceptors = addSecurityInterceptor(
				chain.getInterceptors());
		return new HandlerExecutionChain(chain.getHandler(), interceptors);
	}

	private HandlerInterceptor[] addSecurityInterceptor(HandlerInterceptor[] existing) {
		List<HandlerInterceptor> interceptors = new ArrayList<HandlerInterceptor>();
		interceptors.add(new CorsInterceptor(this.corsConfiguration));
		interceptors.add(this.securityInterceptor);
		if (existing != null) {
			interceptors.addAll(Arrays.asList(existing));
		}
		return interceptors.toArray(new HandlerInterceptor[interceptors.size()]);
	}

	/**
	 * {@link HandlerInterceptor} that processes the response for CORS.
	 */
	class CorsInterceptor extends HandlerInterceptorAdapter {

		private final CorsConfiguration config;

		CorsInterceptor(CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler) throws Exception {
			return getCorsProcessor().processRequest(this.config, request, response);
		}

	}

}
