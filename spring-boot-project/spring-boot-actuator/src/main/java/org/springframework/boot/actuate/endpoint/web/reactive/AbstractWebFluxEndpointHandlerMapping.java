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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.OperationType;
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
import org.springframework.boot.actuate.endpoint.web.reactive.AbstractWebFluxEndpointHandlerMapping.AbstractWebFluxEndpointHandlerMappingRuntimeHints;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.0.0
 */
@ImportRuntimeHints(AbstractWebFluxEndpointHandlerMappingRuntimeHints.class)
public abstract class AbstractWebFluxEndpointHandlerMapping extends RequestMappingInfoHandlerMapping {

	private final EndpointMapping endpointMapping;

	private final Collection<ExposableWebEndpoint> endpoints;

	private final EndpointMediaTypes endpointMediaTypes;

	private final CorsConfiguration corsConfiguration;

	private final Method handleWriteMethod = ReflectionUtils.findMethod(WriteOperationHandler.class, "handle",
			ServerWebExchange.class, Map.class);

	private final Method handleReadMethod = ReflectionUtils.findMethod(ReadOperationHandler.class, "handle",
			ServerWebExchange.class);

	private final boolean shouldRegisterLinksMapping;

	/**
	 * Creates a new {@code AbstractWebFluxEndpointHandlerMapping} that provides mappings
	 * for the operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 */
	public AbstractWebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
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
     * Initializes the handler methods for the web endpoints.
     * 
     * This method iterates through each {@link ExposableWebEndpoint} and its associated {@link WebOperation},
     * and registers the mapping for each operation.
     * 
     * If the {@code shouldRegisterLinksMapping} flag is set to true, it also registers the links mapping.
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
     * @param handler the handler object
     * @param method the method object
     * @return the created handler method
     */
    @Override
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod = super.createHandlerMethod(handler, method);
		return new WebFluxEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
	}

	/**
     * Registers a mapping for the given operation on the specified endpoint.
     * 
     * @param endpoint The exposable web endpoint.
     * @param operation The web operation to register.
     */
    private void registerMappingForOperation(ExposableWebEndpoint endpoint, WebOperation operation) {
		RequestMappingInfo requestMappingInfo = createRequestMappingInfo(operation);
		if (operation.getType() == OperationType.WRITE) {
			ReactiveWebOperation reactiveWebOperation = wrapReactiveWebOperation(endpoint, operation,
					new ReactiveWebOperationAdapter(operation));
			registerMapping(requestMappingInfo, new WriteOperationHandler((reactiveWebOperation)),
					this.handleWriteMethod);
		}
		else {
			registerReadMapping(requestMappingInfo, endpoint, operation);
		}
	}

	/**
     * Registers a read mapping for the given request mapping information, endpoint, and operation.
     * 
     * @param requestMappingInfo the request mapping information for the mapping
     * @param endpoint the exposable web endpoint
     * @param operation the web operation
     */
    protected void registerReadMapping(RequestMappingInfo requestMappingInfo, ExposableWebEndpoint endpoint,
			WebOperation operation) {
		ReactiveWebOperation reactiveWebOperation = wrapReactiveWebOperation(endpoint, operation,
				new ReactiveWebOperationAdapter(operation));
		registerMapping(requestMappingInfo, new ReadOperationHandler((reactiveWebOperation)), this.handleReadMethod);
	}

	/**
	 * Hook point that allows subclasses to wrap the {@link ReactiveWebOperation} before
	 * it's called. Allows additional features, such as security, to be added.
	 * @param endpoint the source endpoint
	 * @param operation the source operation
	 * @param reactiveWebOperation the reactive web operation to wrap
	 * @return a wrapped reactive web operation
	 */
	protected ReactiveWebOperation wrapReactiveWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
			ReactiveWebOperation reactiveWebOperation) {
		return reactiveWebOperation;
	}

	/**
     * Creates a RequestMappingInfo object based on the given WebOperation.
     * 
     * @param operation the WebOperation object to create the RequestMappingInfo from
     * @return the created RequestMappingInfo object
     */
    private RequestMappingInfo createRequestMappingInfo(WebOperation operation) {
		WebOperationRequestPredicate predicate = operation.getRequestPredicate();
		String path = this.endpointMapping.createSubPath(predicate.getPath());
		List<String> paths = new ArrayList<>();
		paths.add(path);
		if (!StringUtils.hasText(path)) {
			paths.add("/");
		}
		RequestMethod method = RequestMethod.valueOf(predicate.getHttpMethod().name());
		String[] consumes = StringUtils.toStringArray(predicate.getConsumes());
		String[] produces = StringUtils.toStringArray(predicate.getProduces());
		return RequestMappingInfo.paths(paths.toArray(new String[0]))
			.methods(method)
			.consumes(consumes)
			.produces(produces)
			.build();
	}

	/**
     * Registers the links mapping for the endpoint.
     * 
     * @since 1.0.0
     */
    private void registerLinksMapping() {
		String path = this.endpointMapping.getPath();
		String linksPath = StringUtils.hasLength(path) ? path : "/";
		String[] produces = StringUtils.toStringArray(this.endpointMediaTypes.getProduced());
		RequestMappingInfo mapping = RequestMappingInfo.paths(linksPath)
			.methods(RequestMethod.GET)
			.produces(produces)
			.build();
		LinksHandler linksHandler = getLinksHandler();
		registerMapping(mapping, linksHandler,
				ReflectionUtils.findMethod(linksHandler.getClass(), "links", ServerWebExchange.class));
	}

	/**
     * Determines if the specified handler has a CORS configuration source.
     * 
     * @param handler the handler object
     * @return {@code true} if the handler has a CORS configuration source, {@code false} otherwise
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
     * @param beanType the bean type to check
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
	 * An {@link OperationInvoker} that performs the invocation of a blocking operation on
	 * a separate thread using Reactor's {@link Schedulers#boundedElastic() bounded
	 * elastic scheduler}.
	 */
	protected static final class ElasticSchedulerInvoker implements OperationInvoker {

		private final OperationInvoker invoker;

		/**
         * Constructs a new ElasticSchedulerInvoker with the specified OperationInvoker.
         * 
         * @param invoker the OperationInvoker to be used by the ElasticSchedulerInvoker
         */
        public ElasticSchedulerInvoker(OperationInvoker invoker) {
			this.invoker = invoker;
		}

		/**
         * Invokes the given context using the invoker and returns the result as a Mono.
         * The invocation is performed on a bounded elastic scheduler.
         *
         * @param context the invocation context
         * @return a Mono representing the result of the invocation
         */
        @Override
		public Object invoke(InvocationContext context) {
			return Mono.fromCallable(() -> this.invoker.invoke(context)).subscribeOn(Schedulers.boundedElastic());
		}

	}

	/**
     * ExceptionCapturingInvoker class.
     */
    protected static final class ExceptionCapturingInvoker implements OperationInvoker {

		private final OperationInvoker invoker;

		/**
         * Constructs a new ExceptionCapturingInvoker with the specified OperationInvoker.
         * 
         * @param invoker the OperationInvoker to be used by this ExceptionCapturingInvoker
         */
        public ExceptionCapturingInvoker(OperationInvoker invoker) {
			this.invoker = invoker;
		}

		/**
         * Invokes the given InvocationContext and captures any exceptions that occur.
         * 
         * @param context the InvocationContext to be invoked
         * @return the result of invoking the InvocationContext
         * @throws Exception if an exception occurs during invocation
         */
        @Override
		public Object invoke(InvocationContext context) {
			try {
				return this.invoker.invoke(context);
			}
			catch (Exception ex) {
				return Mono.error(ex);
			}
		}

	}

	/**
	 * Reactive handler providing actuator links at the root endpoint.
	 */
	@FunctionalInterface
	protected interface LinksHandler {

		Object links(ServerWebExchange exchange);

	}

	/**
	 * A reactive web operation that can be handled by WebFlux.
	 */
	@FunctionalInterface
	protected interface ReactiveWebOperation {

		Mono<ResponseEntity<Object>> handle(ServerWebExchange exchange, Map<String, String> body);

	}

	/**
	 * Adapter class to convert an {@link OperationInvoker} into a
	 * {@link ReactiveWebOperation}.
	 */
	private static final class ReactiveWebOperationAdapter implements ReactiveWebOperation {

		private static final String PATH_SEPARATOR = AntPathMatcher.DEFAULT_PATH_SEPARATOR;

		private final WebOperation operation;

		private final OperationInvoker invoker;

		private final Supplier<Mono<? extends SecurityContext>> securityContextSupplier;

		/**
         * Constructs a new ReactiveWebOperationAdapter with the given WebOperation.
         * 
         * @param operation the WebOperation to be adapted
         */
        private ReactiveWebOperationAdapter(WebOperation operation) {
			this.operation = operation;
			this.invoker = getInvoker(operation);
			this.securityContextSupplier = getSecurityContextSupplier();
		}

		/**
         * Returns an instance of OperationInvoker based on the given WebOperation.
         * 
         * @param operation the WebOperation to get the invoker for
         * @return the OperationInvoker instance
         */
        private OperationInvoker getInvoker(WebOperation operation) {
			OperationInvoker invoker = operation::invoke;
			if (operation.isBlocking()) {
				return new ElasticSchedulerInvoker(invoker);
			}
			return new ExceptionCapturingInvoker(invoker);
		}

		/**
         * Returns a supplier of Mono that provides a SecurityContext.
         * If the ReactiveSecurityContextHolder class is present in the class loader,
         * the supplier will return the SecurityContext from the ReactiveSecurityContextHolder.
         * Otherwise, the supplier will return an empty SecurityContext.
         *
         * @return a supplier of Mono that provides a SecurityContext
         */
        private Supplier<Mono<? extends SecurityContext>> getSecurityContextSupplier() {
			if (ClassUtils.isPresent("org.springframework.security.core.context.ReactiveSecurityContextHolder",
					getClass().getClassLoader())) {
				return this::springSecurityContext;
			}
			return this::emptySecurityContext;
		}

		/**
         * Retrieves the Spring Security context as a reactive Mono.
         * 
         * @return a Mono that emits the current Spring Security context as a ReactiveSecurityContext object
         *         or emits an empty ReactiveSecurityContext if the context is empty
         */
        Mono<? extends SecurityContext> springSecurityContext() {
			return ReactiveSecurityContextHolder.getContext()
				.map((securityContext) -> new ReactiveSecurityContext(securityContext.getAuthentication()))
				.switchIfEmpty(Mono.just(new ReactiveSecurityContext(null)));
		}

		/**
         * Returns an empty Mono of SecurityContext.
         * 
         * @return an empty Mono of SecurityContext
         */
        Mono<SecurityContext> emptySecurityContext() {
			return Mono.just(SecurityContext.NONE);
		}

		/**
         * Handles the incoming server web exchange and body map by resolving the operation arguments,
         * security context, and producing the operation argument resolver. It then invokes the operation
         * using the invocation context and handles the result by mapping it to a response entity.
         *
         * @param exchange The server web exchange containing the request and response.
         * @param body The body map containing the request body.
         * @return A Mono of ResponseEntity representing the response entity of the operation.
         */
        @Override
		public Mono<ResponseEntity<Object>> handle(ServerWebExchange exchange, Map<String, String> body) {
			Map<String, Object> arguments = getArguments(exchange, body);
			OperationArgumentResolver serverNamespaceArgumentResolver = OperationArgumentResolver
				.of(WebServerNamespace.class, () -> WebServerNamespace
					.from(WebServerApplicationContext.getServerNamespace(exchange.getApplicationContext())));
			return this.securityContextSupplier.get()
				.map((securityContext) -> new InvocationContext(securityContext, arguments,
						serverNamespaceArgumentResolver,
						new ProducibleOperationArgumentResolver(
								() -> exchange.getRequest().getHeaders().get("Accept"))))
				.flatMap((invocationContext) -> handleResult((Publisher<?>) this.invoker.invoke(invocationContext),
						exchange.getRequest().getMethod()));
		}

		/**
         * Retrieves the arguments for the given server web exchange and request body.
         * 
         * @param exchange The server web exchange.
         * @param body The request body.
         * @return A map containing the arguments.
         */
        private Map<String, Object> getArguments(ServerWebExchange exchange, Map<String, String> body) {
			Map<String, Object> arguments = new LinkedHashMap<>(getTemplateVariables(exchange));
			String matchAllRemainingPathSegmentsVariable = this.operation.getRequestPredicate()
				.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				arguments.put(matchAllRemainingPathSegmentsVariable, getRemainingPathSegments(exchange));
			}
			if (body != null) {
				arguments.putAll(body);
			}
			exchange.getRequest()
				.getQueryParams()
				.forEach((name, values) -> arguments.put(name, (values.size() != 1) ? values : values.get(0)));
			return arguments;
		}

		/**
         * Retrieves the remaining path segments from the given ServerWebExchange.
         * 
         * @param exchange the ServerWebExchange object representing the current HTTP request and response
         * @return an Object containing the remaining path segments
         */
        private Object getRemainingPathSegments(ServerWebExchange exchange) {
			PathPattern pathPattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			if (pathPattern.hasPatternSyntax()) {
				String remainingSegments = pathPattern
					.extractPathWithinPattern(exchange.getRequest().getPath().pathWithinApplication())
					.value();
				return tokenizePathSegments(remainingSegments);
			}
			return tokenizePathSegments(pathPattern.toString());
		}

		/**
         * Tokenizes the given path value into segments.
         * 
         * @param value the path value to tokenize
         * @return an array of path segments
         */
        private String[] tokenizePathSegments(String value) {
			String[] segments = StringUtils.tokenizeToStringArray(value, PATH_SEPARATOR, false, true);
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].contains("%")) {
					segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
				}
			}
			return segments;
		}

		/**
         * Retrieves the template variables from the given ServerWebExchange.
         * 
         * @param exchange the ServerWebExchange from which to retrieve the template variables
         * @return a Map containing the template variables, where the key is the variable name and the value is the variable value
         */
        private Map<String, String> getTemplateVariables(ServerWebExchange exchange) {
			return exchange.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		}

		/**
         * Handles the result of a reactive operation and returns a Mono of ResponseEntity<Object>.
         * 
         * @param result The result of the reactive operation.
         * @param httpMethod The HTTP method used for the operation.
         * @return A Mono of ResponseEntity<Object> representing the result of the operation.
         */
        private Mono<ResponseEntity<Object>> handleResult(Publisher<?> result, HttpMethod httpMethod) {
			if (result instanceof Flux) {
				result = ((Flux<?>) result).collectList();
			}
			return Mono.from(result)
				.map(this::toResponseEntity)
				.onErrorMap(InvalidEndpointRequestException.class,
						(ex) -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getReason()))
				.defaultIfEmpty(new ResponseEntity<>(
						(httpMethod != HttpMethod.GET) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND));
		}

		/**
         * Converts the given response object to a ResponseEntity object.
         * If the response object is not an instance of WebEndpointResponse, it will be wrapped in a ResponseEntity with HttpStatus.OK.
         * If the response object is an instance of WebEndpointResponse, it will be converted to a ResponseEntity with the appropriate status, content type, and body.
         *
         * @param response the response object to convert
         * @return a ResponseEntity object representing the converted response
         */
        private ResponseEntity<Object> toResponseEntity(Object response) {
			if (!(response instanceof WebEndpointResponse<?> webEndpointResponse)) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			MediaType contentType = (webEndpointResponse.getContentType() != null)
					? new MediaType(webEndpointResponse.getContentType()) : null;
			return ResponseEntity.status(webEndpointResponse.getStatus())
				.contentType(contentType)
				.body(webEndpointResponse.getBody());
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

	}

	/**
	 * Handler for a {@link ReactiveWebOperation}.
	 */
	private static final class WriteOperationHandler {

		private final ReactiveWebOperation operation;

		/**
         * Constructs a new WriteOperationHandler with the specified ReactiveWebOperation.
         * 
         * @param operation the ReactiveWebOperation to be handled
         */
        WriteOperationHandler(ReactiveWebOperation operation) {
			this.operation = operation;
		}

		/**
         * Handles the write operation for the server web exchange.
         * 
         * @param exchange the server web exchange
         * @param body the request body as a map of string key-value pairs (optional)
         * @return a publisher that emits a response entity object
         */
        @ResponseBody
		@Reflective
		Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange,
				@RequestBody(required = false) Map<String, String> body) {
			return this.operation.handle(exchange, body);
		}

		/**
         * Returns a string representation of the WriteOperationHandler object.
         * 
         * @return a string representation of the WriteOperationHandler object
         */
        @Override
		public String toString() {
			return this.operation.toString();
		}

	}

	/**
	 * Handler for a {@link ReactiveWebOperation}.
	 */
	private static final class ReadOperationHandler {

		private final ReactiveWebOperation operation;

		/**
         * Constructs a new ReadOperationHandler with the specified ReactiveWebOperation.
         * 
         * @param operation the ReactiveWebOperation to be used by the handler
         */
        ReadOperationHandler(ReactiveWebOperation operation) {
			this.operation = operation;
		}

		/**
         * Handles the server web exchange and returns a publisher of ResponseEntity objects.
         * 
         * @param exchange the server web exchange
         * @return a publisher of ResponseEntity objects
         */
        @ResponseBody
		@Reflective
		Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange) {
			return this.operation.handle(exchange, null);
		}

		/**
         * Returns a string representation of the ReadOperationHandler object.
         * 
         * @return a string representation of the ReadOperationHandler object
         */
        @Override
		public String toString() {
			return this.operation.toString();
		}

	}

	/**
     * WebFluxEndpointHandlerMethod class.
     */
    private static class WebFluxEndpointHandlerMethod extends HandlerMethod {

		/**
         * Constructs a new WebFluxEndpointHandlerMethod with the specified bean and method.
         *
         * @param bean   the object representing the bean that contains the method
         * @param method the method to be invoked
         */
        WebFluxEndpointHandlerMethod(Object bean, Method method) {
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
         * Creates a new {@link HandlerMethod} with a resolved bean.
         * 
         * @return the created {@link HandlerMethod} with a resolved bean
         */
        @Override
		public HandlerMethod createWithResolvedBean() {
			HandlerMethod handlerMethod = super.createWithResolvedBean();
			return new WebFluxEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
		}

	}

	/**
     * ReactiveSecurityContext class.
     */
    private static final class ReactiveSecurityContext implements SecurityContext {

		private static final String ROLE_PREFIX = "ROLE_";

		private final Authentication authentication;

		/**
         * Constructs a new ReactiveSecurityContext with the provided Authentication.
         * 
         * @param authentication the Authentication object representing the security context
         */
        ReactiveSecurityContext(Authentication authentication) {
			this.authentication = authentication;
		}

		/**
         * Returns the authentication object associated with the current security context.
         *
         * @return the authentication object
         */
        private Authentication getAuthentication() {
			return this.authentication;
		}

		/**
         * Returns the principal associated with this security context.
         *
         * @return the principal associated with this security context
         */
        @Override
		public Principal getPrincipal() {
			return this.authentication;
		}

		/**
         * Checks if the current user has the specified role.
         * 
         * @param role the role to check
         * @return true if the user has the role, false otherwise
         */
        @Override
		public boolean isUserInRole(String role) {
			String authority = (!role.startsWith(ROLE_PREFIX)) ? ROLE_PREFIX + role : role;
			return AuthorityAuthorizationManager.hasAuthority(authority)
				.check(this::getAuthentication, null)
				.isGranted();
		}

	}

	/**
     * AbstractWebFluxEndpointHandlerMappingRuntimeHints class.
     */
    static class AbstractWebFluxEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

		/**
         * Registers the runtime hints for the specified WriteOperationHandler and ReadOperationHandler classes.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for reflective registration
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.reflectiveRegistrar.registerRuntimeHints(hints, WriteOperationHandler.class,
					ReadOperationHandler.class);
		}

	}

}
