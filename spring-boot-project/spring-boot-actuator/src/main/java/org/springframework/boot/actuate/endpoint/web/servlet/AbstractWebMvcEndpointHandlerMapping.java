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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.ProducibleOperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping.AbstractWebMvcEndpointHandlerMappingRuntimeHints;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link HandlerMapping} that makes {@link ExposableWebEndpoint web endpoints}
 * available over HTTP using Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.0.0
 */
@ImportRuntimeHints(AbstractWebMvcEndpointHandlerMappingRuntimeHints.class)
public abstract class AbstractWebMvcEndpointHandlerMapping extends RequestMappingInfoHandlerMapping
		implements InitializingBean {

	private final EndpointMapping endpointMapping;

	private final Collection<ExposableWebEndpoint> endpoints;

	private final EndpointMediaTypes endpointMediaTypes;

	private final CorsConfiguration corsConfiguration;

	private final boolean shouldRegisterLinksMapping;

	private final Method handleMethod = ReflectionUtils.findMethod(OperationHandler.class, "handle",
			HttpServletRequest.class, Map.class);

	private RequestMappingInfo.BuilderConfiguration builderConfig = new RequestMappingInfo.BuilderConfiguration();

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 */
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
			boolean shouldRegisterLinksMapping) {
		this(endpointMapping, endpoints, endpointMediaTypes, null, shouldRegisterLinksMapping);
	}

	/**
	 * Creates a new {@code AbstractWebMvcEndpointHandlerMapping} that provides mappings
	 * for the operations of the given endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 */
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
			CorsConfiguration corsConfiguration, boolean shouldRegisterLinksMapping) {
		this.endpointMapping = endpointMapping;
		this.endpoints = endpoints;
		this.endpointMediaTypes = endpointMediaTypes;
		this.corsConfiguration = corsConfiguration;
		this.shouldRegisterLinksMapping = shouldRegisterLinksMapping;
		setOrder(-100);
	}

	/**
     * Callback method that is invoked after all bean properties have been set.
     * Initializes the builder configuration for the request mapping info.
     * 
     * @throws Exception if an error occurs during initialization
     */
    @Override
	public void afterPropertiesSet() {
		this.builderConfig = new RequestMappingInfo.BuilderConfiguration();
		this.builderConfig.setPatternParser(getPatternParser());
		super.afterPropertiesSet();
	}

	/**
     * Initializes the handler methods for the web endpoints.
     * 
     * This method iterates through each {@link ExposableWebEndpoint} and its operations,
     * and registers the mapping for each operation using the {@link #registerMappingForOperation(ExposableWebEndpoint, WebOperation)} method.
     * 
     * If the {@code shouldRegisterLinksMapping} flag is set to true, it also registers the links mapping using the {@link #registerLinksMapping()} method.
     */
    @Override
	protected void initHandlerMethods() {
		for (ExposableWebEndpoint endpoint : this.endpoints) {
			for (WebOperation operation : endpoint.getOperations()) {
				registerMappingForOperation(endpoint, operation);
			}
		}
		if (this.shouldRegisterLinksMapping) {
			registerLinksMapping();
		}
	}

	/**
     * Creates a handler method for the given handler and method.
     * 
     * @param handler The handler object.
     * @param method The method object.
     * @return The created handler method.
     */
    @Override
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod = super.createHandlerMethod(handler, method);
		return new WebMvcEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
	}

	/**
     * Registers a mapping for the given operation on the specified endpoint.
     * 
     * @param endpoint the exposable web endpoint
     * @param operation the web operation
     */
    private void registerMappingForOperation(ExposableWebEndpoint endpoint, WebOperation operation) {
		WebOperationRequestPredicate predicate = operation.getRequestPredicate();
		String path = predicate.getPath();
		String matchAllRemainingPathSegmentsVariable = predicate.getMatchAllRemainingPathSegmentsVariable();
		if (matchAllRemainingPathSegmentsVariable != null) {
			path = path.replace("{*" + matchAllRemainingPathSegmentsVariable + "}", "**");
		}
		registerMapping(endpoint, predicate, operation, path);
	}

	/**
     * Registers a mapping for a web endpoint.
     * 
     * @param endpoint the exposable web endpoint
     * @param predicate the web operation request predicate
     * @param operation the web operation
     * @param path the path for the mapping
     */
    protected void registerMapping(ExposableWebEndpoint endpoint, WebOperationRequestPredicate predicate,
			WebOperation operation, String path) {
		ServletWebOperation servletWebOperation = wrapServletWebOperation(endpoint, operation,
				new ServletWebOperationAdapter(operation));
		registerMapping(createRequestMappingInfo(predicate, path), new OperationHandler(servletWebOperation),
				this.handleMethod);
	}

	/**
	 * Hook point that allows subclasses to wrap the {@link ServletWebOperation} before
	 * it's called. Allows additional features, such as security, to be added.
	 * @param endpoint the source endpoint
	 * @param operation the source operation
	 * @param servletWebOperation the servlet web operation to wrap
	 * @return a wrapped servlet web operation
	 */
	protected ServletWebOperation wrapServletWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
			ServletWebOperation servletWebOperation) {
		return servletWebOperation;
	}

	/**
     * Creates a RequestMappingInfo object based on the given WebOperationRequestPredicate and path.
     * 
     * @param predicate the WebOperationRequestPredicate object representing the request predicate
     * @param path the path for the request mapping
     * @return the created RequestMappingInfo object
     */
    private RequestMappingInfo createRequestMappingInfo(WebOperationRequestPredicate predicate, String path) {
		String subPath = this.endpointMapping.createSubPath(path);
		List<String> paths = new ArrayList<>();
		paths.add(subPath);
		if (!StringUtils.hasLength(subPath)) {
			paths.add("/");
		}
		return RequestMappingInfo.paths(paths.toArray(new String[0]))
			.options(this.builderConfig)
			.methods(RequestMethod.valueOf(predicate.getHttpMethod().name()))
			.consumes(predicate.getConsumes().toArray(new String[0]))
			.produces(predicate.getProduces().toArray(new String[0]))
			.build();
	}

	/**
     * Registers the links mapping for the endpoint.
     * 
     * This method creates a mapping for the links path of the endpoint and registers it with the provided links handler.
     * The mapping is configured to handle GET requests and produce the specified media types.
     * 
     * @see RequestMappingInfo
     * @see RequestMethod
     * @see LinksHandler
     * @see HttpServletRequest
     * @see HttpServletResponse
     */
    private void registerLinksMapping() {
		String path = this.endpointMapping.getPath();
		String linksPath = (StringUtils.hasLength(path)) ? this.endpointMapping.createSubPath("/") : "/";
		RequestMappingInfo mapping = RequestMappingInfo.paths(linksPath)
			.methods(RequestMethod.GET)
			.produces(this.endpointMediaTypes.getProduced().toArray(new String[0]))
			.options(this.builderConfig)
			.build();
		LinksHandler linksHandler = getLinksHandler();
		registerMapping(mapping, linksHandler, ReflectionUtils.findMethod(linksHandler.getClass(), "links",
				HttpServletRequest.class, HttpServletResponse.class));
	}

	/**
     * Determines if the specified handler has a CORS configuration source.
     * 
     * @param handler the handler object
     * @return true if the handler has a CORS configuration source, false otherwise
     */
    @Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	/**
     * Initializes the CORS configuration for the given handler, method, and mapping.
     * 
     * @param handler the handler object
     * @param method the method object
     * @param mapping the mapping information
     * @return the CORS configuration
     */
    @Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	/**
     * Determines if the specified bean type is a handler.
     * 
     * @param beanType the class of the bean to check
     * @return {@code true} if the bean type is a handler, {@code false} otherwise
     */
    @Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	/**
     * Retrieves the mapping information for a given method and handler type.
     * 
     * @param method the method to retrieve mapping information for
     * @param handlerType the handler type to retrieve mapping information for
     * @return the mapping information for the given method and handler type, or null if not found
     */
    @Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		return null;
	}

	/**
     * Extends the list of interceptors for this endpoint handler mapping.
     * 
     * @param interceptors the list of interceptors to be extended
     */
    @Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

	/**
	 * Return the Handler providing actuator links at the root endpoint.
	 * @return the links handler
	 */
	protected abstract LinksHandler getLinksHandler();

	/**
	 * Return the web endpoints being mapped.
	 * @return the endpoints
	 */
	public Collection<ExposableWebEndpoint> getEndpoints() {
		return this.endpoints;
	}

	/**
	 * Handler providing actuator links at the root endpoint.
	 */
	@FunctionalInterface
	protected interface LinksHandler {

		Object links(HttpServletRequest request, HttpServletResponse response);

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
	private static class ServletWebOperationAdapter implements ServletWebOperation {

		private static final String PATH_SEPARATOR = AntPathMatcher.DEFAULT_PATH_SEPARATOR;

		private static final List<Function<Object, Object>> BODY_CONVERTERS;

		static {
			List<Function<Object, Object>> converters = new ArrayList<>();
			if (ClassUtils.isPresent("reactor.core.publisher.Flux",
					ServletWebOperationAdapter.class.getClassLoader())) {
				converters.add(new FluxBodyConverter());
			}
			BODY_CONVERTERS = Collections.unmodifiableList(converters);
		}

		private final WebOperation operation;

		/**
         * Constructs a new ServletWebOperationAdapter with the specified WebOperation.
         * 
         * @param operation the WebOperation to be adapted
         */
        ServletWebOperationAdapter(WebOperation operation) {
			this.operation = operation;
		}

		/**
         * Handles the HTTP request and returns the result.
         * 
         * @param request The HttpServletRequest object representing the HTTP request.
         * @param body The request body as a Map of key-value pairs. It is optional and can be null.
         * @return The result of handling the request.
         * @throws InvalidEndpointBadRequestException if the endpoint request is invalid.
         */
        @Override
		public Object handle(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
			HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
			Map<String, Object> arguments = getArguments(request, body);
			try {
				ServletSecurityContext securityContext = new ServletSecurityContext(request);
				ProducibleOperationArgumentResolver producibleOperationArgumentResolver = new ProducibleOperationArgumentResolver(
						() -> headers.get("Accept"));
				OperationArgumentResolver serverNamespaceArgumentResolver = OperationArgumentResolver
					.of(WebServerNamespace.class, () -> {
						WebApplicationContext applicationContext = WebApplicationContextUtils
							.getRequiredWebApplicationContext(request.getServletContext());
						return WebServerNamespace
							.from(WebServerApplicationContext.getServerNamespace(applicationContext));
					});
				InvocationContext invocationContext = new InvocationContext(securityContext, arguments,
						serverNamespaceArgumentResolver, producibleOperationArgumentResolver);
				return handleResult(this.operation.invoke(invocationContext), HttpMethod.valueOf(request.getMethod()));
			}
			catch (InvalidEndpointRequestException ex) {
				throw new InvalidEndpointBadRequestException(ex);
			}
		}

		/**
         * Returns a string representation of the Actuator web endpoint.
         * 
         * @return a string representation of the Actuator web endpoint
         */
        @Override
		public String toString() {
			return "Actuator web endpoint '" + this.operation.getId() + "'";
		}

		/**
         * Retrieves the arguments from the HttpServletRequest and the request body.
         * 
         * @param request The HttpServletRequest object containing the request information.
         * @param body The request body as a Map of key-value pairs.
         * @return A Map of arguments extracted from the HttpServletRequest and the request body.
         */
        private Map<String, Object> getArguments(HttpServletRequest request, Map<String, String> body) {
			Map<String, Object> arguments = new LinkedHashMap<>(getTemplateVariables(request));
			String matchAllRemainingPathSegmentsVariable = this.operation.getRequestPredicate()
				.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				arguments.put(matchAllRemainingPathSegmentsVariable, getRemainingPathSegments(request));
			}
			if (body != null && HttpMethod.POST.name().equals(request.getMethod())) {
				arguments.putAll(body);
			}
			request.getParameterMap()
				.forEach((name, values) -> arguments.put(name,
						(values.length != 1) ? Arrays.asList(values) : values[0]));
			return arguments;
		}

		/**
         * Retrieves the remaining path segments from the given HttpServletRequest.
         * 
         * @param request the HttpServletRequest object from which to extract the remaining path segments
         * @return an Object representing the remaining path segments
         * @throws IllegalStateException if unable to extract the remaining path segments
         */
        private Object getRemainingPathSegments(HttpServletRequest request) {
			String[] pathTokens = tokenize(request, HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, true);
			String[] patternTokens = tokenize(request, HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, false);
			int numberOfRemainingPathSegments = pathTokens.length - patternTokens.length + 1;
			Assert.state(numberOfRemainingPathSegments >= 0, "Unable to extract remaining path segments");
			String[] remainingPathSegments = new String[numberOfRemainingPathSegments];
			System.arraycopy(pathTokens, patternTokens.length - 1, remainingPathSegments, 0,
					numberOfRemainingPathSegments);
			return remainingPathSegments;
		}

		/**
         * Tokenizes the value of the specified attribute in the given HttpServletRequest.
         * 
         * @param request the HttpServletRequest object
         * @param attributeName the name of the attribute to retrieve the value from
         * @param decode true if the segments should be URI decoded, false otherwise
         * @return an array of segments obtained by tokenizing the attribute value
         */
        private String[] tokenize(HttpServletRequest request, String attributeName, boolean decode) {
			String value = (String) request.getAttribute(attributeName);
			String[] segments = StringUtils.tokenizeToStringArray(value, PATH_SEPARATOR, false, true);
			if (decode) {
				for (int i = 0; i < segments.length; i++) {
					if (segments[i].contains("%")) {
						segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
					}
				}
			}
			return segments;
		}

		/**
         * Retrieves the template variables from the given HttpServletRequest object.
         * 
         * @param request the HttpServletRequest object from which to retrieve the template variables
         * @return a Map containing the template variables, with the variable names as keys and their corresponding values as values
         */
        @SuppressWarnings("unchecked")
		private Map<String, String> getTemplateVariables(HttpServletRequest request) {
			return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		}

		/**
         * Handles the result of a web endpoint operation.
         * 
         * @param result     The result of the operation.
         * @param httpMethod The HTTP method used for the operation.
         * @return An object representing the result of the operation.
         */
        private Object handleResult(Object result, HttpMethod httpMethod) {
			if (result == null) {
				return new ResponseEntity<>(
						(httpMethod != HttpMethod.GET) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND);
			}
			if (!(result instanceof WebEndpointResponse<?> response)) {
				return convertIfNecessary(result);
			}
			MediaType contentType = (response.getContentType() != null) ? new MediaType(response.getContentType())
					: null;
			return ResponseEntity.status(response.getStatus())
				.contentType(contentType)
				.body(convertIfNecessary(response.getBody()));
		}

		/**
         * Converts the given object if necessary using the registered body converters.
         * 
         * @param body the object to be converted
         * @return the converted object
         */
        private Object convertIfNecessary(Object body) {
			for (Function<Object, Object> converter : BODY_CONVERTERS) {
				body = converter.apply(body);
			}
			return body;
		}

		/**
         * FluxBodyConverter class.
         */
        private static final class FluxBodyConverter implements Function<Object, Object> {

			/**
             * Converts the body of a request to a Flux if it is not already a Flux.
             * If the body is already a Flux, it collects all elements into a List.
             *
             * @param body the body of the request
             * @return the converted body as a Flux or a List of elements
             */
            @Override
			public Object apply(Object body) {
				if (!(body instanceof Flux)) {
					return body;
				}
				return ((Flux<?>) body).collectList();
			}

		}

	}

	/**
	 * Handler for a {@link ServletWebOperation}.
	 */
	private static final class OperationHandler {

		private final ServletWebOperation operation;

		/**
         * Constructs a new OperationHandler with the specified ServletWebOperation.
         * 
         * @param operation the ServletWebOperation to be handled
         */
        OperationHandler(ServletWebOperation operation) {
			this.operation = operation;
		}

		/**
         * Handles the HTTP request and returns the response.
         * 
         * @param request the HttpServletRequest object representing the HTTP request
         * @param body the request body as a Map of key-value pairs (optional)
         * @return the response object
         */
        @ResponseBody
		@Reflective
		Object handle(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
			return this.operation.handle(request, body);
		}

		/**
         * Returns a string representation of the OperationHandler object.
         * 
         * @return a string representation of the OperationHandler object
         */
        @Override
		public String toString() {
			return this.operation.toString();
		}

	}

	/**
	 * {@link HandlerMethod} subclass for endpoint information logging.
	 */
	private static class WebMvcEndpointHandlerMethod extends HandlerMethod {

		/**
         * Constructs a new WebMvcEndpointHandlerMethod with the specified bean and method.
         *
         * @param bean   the object representing the bean
         * @param method the method to be invoked
         */
        WebMvcEndpointHandlerMethod(Object bean, Method method) {
			super(bean, method);
		}

		/**
         * Returns a string representation of the object.
         * 
         * @return a string representation of the object
         */
        @Override
		public String toString() {
			return getBean().toString();
		}

		/**
         * Creates a new instance of the {@link HandlerMethod} class with the resolved bean.
         * 
         * @return The new instance of the {@link HandlerMethod} class with the resolved bean.
         */
        @Override
		public HandlerMethod createWithResolvedBean() {
			return this;
		}

	}

	/**
	 * Nested exception used to wrap an {@link InvalidEndpointRequestException} and
	 * provide a {@link HttpStatus#BAD_REQUEST} status.
	 */
	private static class InvalidEndpointBadRequestException extends ResponseStatusException {

		/**
         * Constructs a new InvalidEndpointBadRequestException with the specified cause.
         * 
         * @param cause the cause of the exception, an instance of InvalidEndpointRequestException
         */
        InvalidEndpointBadRequestException(InvalidEndpointRequestException cause) {
			super(HttpStatus.BAD_REQUEST, cause.getReason(), cause);
		}

	}

	/**
     * ServletSecurityContext class.
     */
    private static final class ServletSecurityContext implements SecurityContext {

		private final HttpServletRequest request;

		/**
         * Constructs a new ServletSecurityContext object with the provided HttpServletRequest.
         *
         * @param request the HttpServletRequest object to be associated with the ServletSecurityContext
         */
        private ServletSecurityContext(HttpServletRequest request) {
			this.request = request;
		}

		/**
         * Returns the principal associated with the current request.
         * 
         * @return the principal associated with the current request
         */
        @Override
		public Principal getPrincipal() {
			return this.request.getUserPrincipal();
		}

		/**
         * Returns a boolean indicating whether the current user is in the specified role.
         * 
         * @param role the role to check
         * @return true if the current user is in the specified role, false otherwise
         */
        @Override
		public boolean isUserInRole(String role) {
			return this.request.isUserInRole(role);
		}

	}

	/**
     * AbstractWebMvcEndpointHandlerMappingRuntimeHints class.
     */
    static class AbstractWebMvcEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

		/**
         * Registers the runtime hints for the given {@link OperationHandler} class using the provided {@link RuntimeHints} and {@link ClassLoader}.
         * 
         * @param hints the {@link RuntimeHints} containing the hints to be registered
         * @param classLoader the {@link ClassLoader} to be used for loading the {@link OperationHandler} class
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.reflectiveRegistrar.registerRuntimeHints(hints, OperationHandler.class);
		}

	}

}
