/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.SecurityResponse;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet.CloudFoundryWebEndpointServletHandlerMapping.CloudFoundryWebEndpointServletHandlerMappingRuntimeHints;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available on
 * Cloud Foundry specific URLs over HTTP using Spring MVC.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 */
@ImportRuntimeHints(CloudFoundryWebEndpointServletHandlerMappingRuntimeHints.class)
class CloudFoundryWebEndpointServletHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private static final Log logger = LogFactory.getLog(CloudFoundryWebEndpointServletHandlerMapping.class);

	private final CloudFoundrySecurityInterceptor securityInterceptor;

	private final EndpointLinksResolver linksResolver;

	private final Collection<ExposableEndpoint<?>> allEndpoints;

	/**
	 * Constructs a new CloudFoundryWebEndpointServletHandlerMapping with the specified
	 * parameters.
	 * @param endpointMapping the endpoint mapping strategy
	 * @param endpoints the collection of exposable web endpoints
	 * @param endpointMediaTypes the media types for the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 * @param securityInterceptor the security interceptor for Cloud Foundry
	 * @param allEndpoints the collection of all endpoints
	 */
	CloudFoundryWebEndpointServletHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
			CorsConfiguration corsConfiguration, CloudFoundrySecurityInterceptor securityInterceptor,
			Collection<ExposableEndpoint<?>> allEndpoints) {
		super(endpointMapping, endpoints, endpointMediaTypes, corsConfiguration, true);
		this.securityInterceptor = securityInterceptor;
		this.linksResolver = new EndpointLinksResolver(allEndpoints);
		this.allEndpoints = allEndpoints;
	}

	/**
	 * Wraps the given ServletWebOperation with a SecureServletWebOperation, adding
	 * security interception for the specified endpoint.
	 * @param endpoint The ExposableWebEndpoint associated with the operation
	 * @param operation The WebOperation to be wrapped
	 * @param servletWebOperation The ServletWebOperation to be wrapped
	 * @return The wrapped SecureServletWebOperation
	 */
	@Override
	protected ServletWebOperation wrapServletWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
			ServletWebOperation servletWebOperation) {
		return new SecureServletWebOperation(servletWebOperation, this.securityInterceptor, endpoint.getEndpointId());
	}

	/**
	 * Returns the LinksHandler for CloudFoundryWebEndpointServletHandlerMapping.
	 * @return the LinksHandler instance for CloudFoundryWebEndpointServletHandlerMapping
	 */
	@Override
	protected LinksHandler getLinksHandler() {
		return new CloudFoundryLinksHandler();
	}

	/**
	 * Returns a collection of all the exposable endpoints.
	 * @return a collection of exposable endpoints
	 */
	Collection<ExposableEndpoint<?>> getAllEndpoints() {
		return this.allEndpoints;
	}

	/**
	 * CloudFoundryLinksHandler class.
	 */
	class CloudFoundryLinksHandler implements LinksHandler {

		/**
		 * Retrieves the links for the Cloud Foundry web endpoint.
		 * @param request the HttpServletRequest object representing the incoming request
		 * @param response the HttpServletResponse object representing the outgoing
		 * response
		 * @return a Map containing the links for the web endpoint
		 */
		@Override
		@ResponseBody
		@Reflective
		public Map<String, Map<String, Link>> links(HttpServletRequest request, HttpServletResponse response) {
			SecurityResponse securityResponse = CloudFoundryWebEndpointServletHandlerMapping.this.securityInterceptor
				.preHandle(request, null);
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				sendFailureResponse(response, securityResponse);
			}
			AccessLevel accessLevel = (AccessLevel) request.getAttribute(AccessLevel.REQUEST_ATTRIBUTE);
			Map<String, Link> filteredLinks = new LinkedHashMap<>();
			if (accessLevel == null) {
				return Collections.singletonMap("_links", filteredLinks);
			}
			Map<String, Link> links = CloudFoundryWebEndpointServletHandlerMapping.this.linksResolver
				.resolveLinks(request.getRequestURL().toString());
			filteredLinks = links.entrySet()
				.stream()
				.filter((e) -> e.getKey().equals("self") || accessLevel.isAccessAllowed(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return Collections.singletonMap("_links", filteredLinks);
		}

		/**
		 * Returns a string representation of the object.
		 * @return a string representation of the object
		 */
		@Override
		public String toString() {
			return "Actuator root web endpoint";
		}

		/**
		 * Sends a failure response with the given security response to the
		 * HttpServletResponse.
		 * @param response The HttpServletResponse object to send the response to.
		 * @param securityResponse The SecurityResponse object containing the status and
		 * message for the failure response.
		 */
		private void sendFailureResponse(HttpServletResponse response, SecurityResponse securityResponse) {
			try {
				response.sendError(securityResponse.getStatus().value(), securityResponse.getMessage());
			}
			catch (Exception ex) {
				logger.debug("Failed to send error response", ex);
			}
		}

	}

	/**
	 * {@link ServletWebOperation} wrapper to add security.
	 */
	private static class SecureServletWebOperation implements ServletWebOperation {

		private final ServletWebOperation delegate;

		private final CloudFoundrySecurityInterceptor securityInterceptor;

		private final EndpointId endpointId;

		/**
		 * Constructs a new SecureServletWebOperation with the specified delegate,
		 * securityInterceptor, and endpointId.
		 * @param delegate the delegate ServletWebOperation to be used
		 * @param securityInterceptor the CloudFoundrySecurityInterceptor to be used
		 * @param endpointId the EndpointId to be used
		 */
		SecureServletWebOperation(ServletWebOperation delegate, CloudFoundrySecurityInterceptor securityInterceptor,
				EndpointId endpointId) {
			this.delegate = delegate;
			this.securityInterceptor = securityInterceptor;
			this.endpointId = endpointId;
		}

		/**
		 * Handles the HTTP request and returns the response.
		 * @param request the HttpServletRequest object representing the incoming request
		 * @param body a Map containing the request body parameters
		 * @return the response object
		 */
		@Override
		public Object handle(HttpServletRequest request, Map<String, String> body) {
			SecurityResponse securityResponse = this.securityInterceptor.preHandle(request, this.endpointId);
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				return new ResponseEntity<Object>(securityResponse.getMessage(), securityResponse.getStatus());
			}
			return this.delegate.handle(request, body);
		}

	}

	/**
	 * CloudFoundryWebEndpointServletHandlerMappingRuntimeHints class.
	 */
	static class CloudFoundryWebEndpointServletHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
		 * Registers the runtime hints for the
		 * CloudFoundryWebEndpointServletHandlerMapping class.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for reflection
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.reflectiveRegistrar.registerRuntimeHints(hints, CloudFoundryLinksHandler.class);
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Link.class);
		}

	}

}
