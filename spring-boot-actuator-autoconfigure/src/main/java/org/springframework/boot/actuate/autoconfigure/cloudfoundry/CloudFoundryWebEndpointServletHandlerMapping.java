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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available on
 * Cloudfoundry specific URLS over HTTP using Spring MVC.
 *
 * @author Madhura Bhave
 */
class CloudFoundryWebEndpointServletHandlerMapping
		extends AbstractWebMvcEndpointHandlerMapping {

	private final Method handle = ReflectionUtils.findMethod(OperationHandler.class,
			"handle", HttpServletRequest.class, Map.class);

	private final Method links = ReflectionUtils.findMethod(
			CloudFoundryWebEndpointServletHandlerMapping.class, "links",
			HttpServletRequest.class, HttpServletResponse.class);

	private static final Log logger = LogFactory
			.getLog(CloudFoundryWebEndpointServletHandlerMapping.class);

	private final CloudFoundrySecurityInterceptor securityInterceptor;

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	CloudFoundryWebEndpointServletHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints,
			CorsConfiguration corsConfiguration,
			CloudFoundrySecurityInterceptor securityInterceptor) {
		super(endpointMapping, webEndpoints, corsConfiguration);
		this.securityInterceptor = securityInterceptor;
	}

	@Override
	protected Method getLinks() {
		return this.links;
	}

	@ResponseBody
	private Map<String, Map<String, Link>> links(HttpServletRequest request,
			HttpServletResponse response) {
		CloudFoundrySecurityInterceptor.SecurityResponse securityResponse = this.securityInterceptor
				.preHandle(request, "");
		if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
			sendFailureResponse(response, securityResponse);
		}
		AccessLevel accessLevel = AccessLevel.get(request);
		Map<String, Link> links = this.endpointLinksResolver.resolveLinks(getEndpoints(),
				request.getRequestURL().toString());
		Map<String, Link> filteredLinks = new LinkedHashMap<>();
		if (accessLevel == null) {
			return Collections.singletonMap("_links", filteredLinks);
		}
		filteredLinks = links.entrySet().stream()
				.filter((e) -> e.getKey().equals("self")
						|| accessLevel.isAccessAllowed(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return Collections.singletonMap("_links", filteredLinks);
	}

	private void sendFailureResponse(HttpServletResponse response,
			CloudFoundrySecurityInterceptor.SecurityResponse securityResponse) {
		try {
			response.sendError(securityResponse.getStatus().value(),
					securityResponse.getMessage());
		}
		catch (Exception ex) {
			logger.debug("Failed to send error response", ex);
		}
	}

	@Override
	protected void registerMappingForOperation(WebEndpointOperation operation) {
		registerMapping(createRequestMappingInfo(operation),
				new OperationHandler(operation.getInvoker(), operation.getId(),
						this.securityInterceptor),
				this.handle);
	}

	/**
	 * Handler which has the handler method and security interceptor.
	 */
	final class OperationHandler {

		private final OperationInvoker operationInvoker;

		private final String endpointId;

		private final CloudFoundrySecurityInterceptor securityInterceptor;

		OperationHandler(OperationInvoker operationInvoker, String id,
				CloudFoundrySecurityInterceptor securityInterceptor) {
			this.operationInvoker = operationInvoker;
			this.endpointId = id;
			this.securityInterceptor = securityInterceptor;
		}

		@SuppressWarnings("unchecked")
		@ResponseBody
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			CloudFoundrySecurityInterceptor.SecurityResponse securityResponse = this.securityInterceptor
					.preHandle(request, this.endpointId);
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				return failureResponse(securityResponse);
			}
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

		private Object failureResponse(
				CloudFoundrySecurityInterceptor.SecurityResponse response) {
			return handleResult(new WebEndpointResponse<>(response.getMessage(),
					response.getStatus().value()));
		}

		private Object handleResult(Object result) {
			return handleResult(result, null);
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
