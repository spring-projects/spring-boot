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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring MVC.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebMvcEndpointHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final Method handle = ReflectionUtils.findMethod(OperationHandler.class,
			"handle", HttpServletRequest.class, Map.class);

	private final Method links = ReflectionUtils.findMethod(
			WebMvcEndpointHandlerMapping.class, "links", HttpServletRequest.class);

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param collection the web endpoints operations
	 */
	public WebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebEndpointOperation>> collection) {
		this(endpointMapping, collection, null);
	}

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param webEndpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 */
	public WebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints,
			CorsConfiguration corsConfiguration) {
		super(endpointMapping, webEndpoints, corsConfiguration);
		setOrder(-100);
	}

	@Override
	protected void registerMappingForOperation(WebEndpointOperation operation) {
		registerMapping(createRequestMappingInfo(operation),
				new OperationHandler(operation.getInvoker()), this.handle);
	}

	@Override
	protected Method getLinks() {
		return this.links;
	}

	@ResponseBody
	private Map<String, Map<String, Link>> links(HttpServletRequest request) {
		return Collections.singletonMap("_links", this.endpointLinksResolver
				.resolveLinks(getEndpoints(), request.getRequestURL().toString()));
	}

	/**
	 * A handler for an endpoint operation.
	 */
	final class OperationHandler {

		private final OperationInvoker operationInvoker;

		OperationHandler(OperationInvoker operationInvoker) {
			this.operationInvoker = operationInvoker;
		}

		@SuppressWarnings("unchecked")
		@ResponseBody
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>((Map<String, String>) request
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
			if (body != null && HttpMethod.POST == httpMethod) {
				arguments.putAll(body);
			}
			request.getParameterMap().forEach((name, values) -> arguments.put(name,
					values.length == 1 ? values[0] : Arrays.asList(values)));
			try {
				return handleResult(this.operationInvoker.invoke(arguments), httpMethod);
			}
			catch (ParameterMappingException ex) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
		}

		private Object handleResult(Object result, HttpMethod httpMethod) {
			if (result == null) {
				return new ResponseEntity<>(httpMethod == HttpMethod.GET
						? HttpStatus.NOT_FOUND : HttpStatus.NO_CONTENT);
			}
			if (!(result instanceof WebEndpointResponse)) {
				return result;
			}
			WebEndpointResponse<?> response = (WebEndpointResponse<?>) result;
			return new ResponseEntity<Object>(response.getBody(),
					HttpStatus.valueOf(response.getStatus()));
		}

	}

}
