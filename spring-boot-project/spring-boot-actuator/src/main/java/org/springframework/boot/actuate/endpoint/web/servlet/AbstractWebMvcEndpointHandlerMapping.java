/*
 * Copyright 2012-2018 the original author or authors.
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
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link HandlerMapping} that makes {@link ExposableWebEndpoint web endpoints}
 * available over HTTP using Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AbstractWebMvcEndpointHandlerMapping
		extends RequestMappingInfoHandlerMapping implements InitializingBean {

	private final EndpointMapping endpointMapping;

	private final Collection<ExposableWebEndpoint> endpoints;

	private final EndpointMediaTypes endpointMediaTypes;

	private final CorsConfiguration corsConfiguration;

	private final Method linksMethod = ReflectionUtils.findMethod(getClass(), "links",
			HttpServletRequest.class, HttpServletResponse.class);

	private final Method handleMethod = ReflectionUtils.findMethod(OperationHandler.class,
			"handle", HttpServletRequest.class, Map.class);

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 */
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints,
			EndpointMediaTypes endpointMediaTypes) {
		this(endpointMapping, endpoints, endpointMediaTypes, null);
	}

	/**
	 * Creates a new {@code AbstractWebMvcEndpointHandlerMapping} that provides mappings
	 * for the operations of the given endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration) {
		this.endpointMapping = endpointMapping;
		this.endpoints = endpoints;
		this.endpointMediaTypes = endpointMediaTypes;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	@Override
	protected void initHandlerMethods() {
		for (ExposableWebEndpoint endpoint : this.endpoints) {
			for (WebOperation operation : endpoint.getOperations()) {
				registerMappingForOperation(endpoint, operation);
			}
		}
		if (StringUtils.hasText(this.endpointMapping.getPath())) {
			registerLinksMapping();
		}
	}

	private void registerMappingForOperation(ExposableWebEndpoint endpoint,
			WebOperation operation) {
		OperationInvoker invoker = operation::invoke;
		ServletWebOperation servletWebOperation = wrapServletWebOperation(endpoint,
				operation, new ServletWebOperationAdapter(invoker));
		registerMapping(createRequestMappingInfo(operation),
				new OperationHandler(servletWebOperation), this.handleMethod);
	}

	/**
	 * Hook point that allows subclasses to wrap the {@link ServletWebOperation} before
	 * it's called. Allows additional features, such as security, to be added.
	 * @param endpoint the source endpoint
	 * @param operation the source operation
	 * @param servletWebOperation the servlet web operation to wrap
	 * @return a wrapped servlet web operation
	 */

	protected ServletWebOperation wrapServletWebOperation(ExposableWebEndpoint endpoint,
			WebOperation operation, ServletWebOperation servletWebOperation) {
		return servletWebOperation;
	}

	private RequestMappingInfo createRequestMappingInfo(WebOperation operation) {
		WebOperationRequestPredicate predicate = operation.getRequestPredicate();
		PatternsRequestCondition patterns = patternsRequestConditionForPattern(
				predicate.getPath());
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.valueOf(predicate.getHttpMethod().name()));
		ConsumesRequestCondition consumes = new ConsumesRequestCondition(
				StringUtils.toStringArray(predicate.getConsumes()));
		ProducesRequestCondition produces = new ProducesRequestCondition(
				StringUtils.toStringArray(predicate.getProduces()));
		return new RequestMappingInfo(null, patterns, methods, null, null, consumes,
				produces, null);
	}

	private void registerLinksMapping() {
		PatternsRequestCondition patterns = patternsRequestConditionForPattern("");
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.GET);
		ProducesRequestCondition produces = new ProducesRequestCondition(
				this.endpointMediaTypes.getProduced().toArray(StringUtils
						.toStringArray(this.endpointMediaTypes.getProduced())));
		RequestMappingInfo mapping = new RequestMappingInfo(patterns, methods, null, null,
				null, produces, null);
		registerMapping(mapping, this, this.linksMethod);
	}

	private PatternsRequestCondition patternsRequestConditionForPattern(String path) {
		String[] patterns = new String[] { this.endpointMapping.createSubPath(path) };
		return new PatternsRequestCondition(patterns, null, null, false, true);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	protected RequestMappingInfo getMappingForMethod(Method method,
			Class<?> handlerType) {
		return null;
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

	protected abstract Object links(HttpServletRequest request,
			HttpServletResponse response);

	/**
	 * Return the web endpoints being mapped.
	 * @return the endpoints
	 */
	public Collection<ExposableWebEndpoint> getEndpoints() {
		return this.endpoints;
	}

	/**
	 * A servlet web operation that can be handled by Spring MVC.
	 */
	@FunctionalInterface
	protected interface ServletWebOperation {

		Object handle(HttpServletRequest request, Map<String, String> body);

	}

	/**
	 * Adapter class to convert an {@link OperationInvoker} into a
	 * {@link ServletWebOperation}.
	 */
	private class ServletWebOperationAdapter implements ServletWebOperation {

		private final OperationInvoker invoker;

		ServletWebOperationAdapter(OperationInvoker invoker) {
			this.invoker = invoker;
		}

		@Override
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			Map<String, Object> arguments = getArguments(request, body);
			try {
				return handleResult(
						this.invoker.invoke(new InvocationContext(
								new ServletSecurityContext(request), arguments)),
						HttpMethod.valueOf(request.getMethod()));
			}
			catch (InvalidEndpointRequestException ex) {
				throw new BadOperationRequestException(ex.getReason());
			}
		}

		private Map<String, Object> getArguments(HttpServletRequest request,
				Map<String, String> body) {
			Map<String, Object> arguments = new LinkedHashMap<>();
			arguments.putAll(getTemplateVariables(request));
			if (body != null && HttpMethod.POST.name().equals(request.getMethod())) {
				arguments.putAll(body);
			}
			request.getParameterMap().forEach((name, values) -> arguments.put(name,
					values.length == 1 ? values[0] : Arrays.asList(values)));
			return arguments;
		}

		@SuppressWarnings("unchecked")
		private Map<String, String> getTemplateVariables(HttpServletRequest request) {
			return (Map<String, String>) request
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
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

	/**
	 * Handler for a {@link ServletWebOperation}.
	 */
	private final class OperationHandler {

		private final ServletWebOperation operation;

		OperationHandler(ServletWebOperation operation) {
			this.operation = operation;
		}

		@ResponseBody
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			return this.operation.handle(request, body);
		}

	}

	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	private static class BadOperationRequestException extends RuntimeException {

		BadOperationRequestException(String message) {
			super(message);
		}

	}

	private static final class ServletSecurityContext implements SecurityContext {

		private final HttpServletRequest request;

		private ServletSecurityContext(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public Principal getPrincipal() {
			return this.request.getUserPrincipal();
		}

		@Override
		public boolean isUserInRole(String role) {
			return this.request.isUserInRole(role);
		}

	}

}
