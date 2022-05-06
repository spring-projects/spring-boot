/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.RoleVoter;
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

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.0.0
 */
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

	@Override
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod = super.createHandlerMethod(handler, method);
		return new WebFluxEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
	}

	private void registerMappingForOperation(ExposableWebEndpoint endpoint, WebOperation operation) {
		ReactiveWebOperation reactiveWebOperation = wrapReactiveWebOperation(endpoint, operation,
				new ReactiveWebOperationAdapter(operation));
		if (operation.getType() == OperationType.WRITE) {
			registerMapping(createRequestMappingInfo(operation), new WriteOperationHandler((reactiveWebOperation)),
					this.handleWriteMethod);
		}
		else {
			registerMapping(createRequestMappingInfo(operation), new ReadOperationHandler((reactiveWebOperation)),
					this.handleReadMethod);
		}
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

	private RequestMappingInfo createRequestMappingInfo(WebOperation operation) {
		WebOperationRequestPredicate predicate = operation.getRequestPredicate();
		String path = this.endpointMapping.createSubPath(predicate.getPath());
		RequestMethod method = RequestMethod.valueOf(predicate.getHttpMethod().name());
		String[] consumes = StringUtils.toStringArray(predicate.getConsumes());
		String[] produces = StringUtils.toStringArray(predicate.getProduces());
		return RequestMappingInfo.paths(path).methods(method).consumes(consumes).produces(produces).build();
	}

	private void registerLinksMapping() {
		String path = this.endpointMapping.getPath();
		String[] produces = StringUtils.toStringArray(this.endpointMediaTypes.getProduced());
		RequestMappingInfo mapping = RequestMappingInfo.paths(path).methods(RequestMethod.GET).produces(produces)
				.build();
		LinksHandler linksHandler = getLinksHandler();
		registerMapping(mapping, linksHandler,
				ReflectionUtils.findMethod(linksHandler.getClass(), "links", ServerWebExchange.class));
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

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

		public ElasticSchedulerInvoker(OperationInvoker invoker) {
			this.invoker = invoker;
		}

		@Override
		public Object invoke(InvocationContext context) {
			return Mono.fromCallable(() -> this.invoker.invoke(context)).subscribeOn(Schedulers.boundedElastic());
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

		private ReactiveWebOperationAdapter(WebOperation operation) {
			this.operation = operation;
			this.invoker = getInvoker(operation);
			this.securityContextSupplier = getSecurityContextSupplier();
		}

		private OperationInvoker getInvoker(WebOperation operation) {
			OperationInvoker invoker = operation::invoke;
			if (operation.isBlocking()) {
				invoker = new ElasticSchedulerInvoker(invoker);
			}
			return invoker;
		}

		private Supplier<Mono<? extends SecurityContext>> getSecurityContextSupplier() {
			if (ClassUtils.isPresent("org.springframework.security.core.context.ReactiveSecurityContextHolder",
					getClass().getClassLoader())) {
				return this::springSecurityContext;
			}
			return this::emptySecurityContext;
		}

		Mono<? extends SecurityContext> springSecurityContext() {
			return ReactiveSecurityContextHolder.getContext()
					.map((securityContext) -> new ReactiveSecurityContext(securityContext.getAuthentication()))
					.switchIfEmpty(Mono.just(new ReactiveSecurityContext(null)));
		}

		Mono<SecurityContext> emptySecurityContext() {
			return Mono.just(SecurityContext.NONE);
		}

		@Override
		public Mono<ResponseEntity<Object>> handle(ServerWebExchange exchange, Map<String, String> body) {
			Map<String, Object> arguments = getArguments(exchange, body);
			String matchAllRemainingPathSegmentsVariable = this.operation.getRequestPredicate()
					.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				arguments.put(matchAllRemainingPathSegmentsVariable,
						tokenizePathSegments((String) arguments.get(matchAllRemainingPathSegmentsVariable)));
			}
			return this.securityContextSupplier.get()
					.map((securityContext) -> new InvocationContext(securityContext, arguments,
							new ProducibleOperationArgumentResolver(
									() -> exchange.getRequest().getHeaders().get("Accept"))))
					.flatMap((invocationContext) -> handleResult((Publisher<?>) this.invoker.invoke(invocationContext),
							exchange.getRequest().getMethod()));
		}

		private String[] tokenizePathSegments(String path) {
			String[] segments = StringUtils.tokenizeToStringArray(path, PATH_SEPARATOR, false, true);
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].contains("%")) {
					segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
				}
			}
			return segments;
		}

		private Map<String, Object> getArguments(ServerWebExchange exchange, Map<String, String> body) {
			Map<String, Object> arguments = new LinkedHashMap<>(getTemplateVariables(exchange));
			if (body != null) {
				arguments.putAll(body);
			}
			exchange.getRequest().getQueryParams()
					.forEach((name, values) -> arguments.put(name, (values.size() != 1) ? values : values.get(0)));
			return arguments;
		}

		private Map<String, String> getTemplateVariables(ServerWebExchange exchange) {
			return exchange.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		}

		private Mono<ResponseEntity<Object>> handleResult(Publisher<?> result, HttpMethod httpMethod) {
			if (result instanceof Flux) {
				result = ((Flux<?>) result).collectList();
			}
			return Mono.from(result).map(this::toResponseEntity)
					.onErrorMap(InvalidEndpointRequestException.class,
							(ex) -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getReason()))
					.defaultIfEmpty(new ResponseEntity<>(
							(httpMethod != HttpMethod.GET) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND));
		}

		private ResponseEntity<Object> toResponseEntity(Object response) {
			if (!(response instanceof WebEndpointResponse)) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
			MediaType contentType = (webEndpointResponse.getContentType() != null)
					? new MediaType(webEndpointResponse.getContentType()) : null;
			return ResponseEntity.status(webEndpointResponse.getStatus()).contentType(contentType)
					.body(webEndpointResponse.getBody());
		}

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

		WriteOperationHandler(ReactiveWebOperation operation) {
			this.operation = operation;
		}

		@ResponseBody
		Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange,
				@RequestBody(required = false) Map<String, String> body) {
			return this.operation.handle(exchange, body);
		}

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

		ReadOperationHandler(ReactiveWebOperation operation) {
			this.operation = operation;
		}

		@ResponseBody
		Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange) {
			return this.operation.handle(exchange, null);
		}

		@Override
		public String toString() {
			return this.operation.toString();
		}

	}

	private static class WebFluxEndpointHandlerMethod extends HandlerMethod {

		WebFluxEndpointHandlerMethod(Object bean, Method method) {
			super(bean, method);
		}

		@Override
		public String toString() {
			return getBean().toString();
		}

		@Override
		public HandlerMethod createWithResolvedBean() {
			HandlerMethod handlerMethod = super.createWithResolvedBean();
			return new WebFluxEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
		}

	}

	private static final class ReactiveSecurityContext implements SecurityContext {

		private final RoleVoter roleVoter = new RoleVoter();

		private final Authentication authentication;

		ReactiveSecurityContext(Authentication authentication) {
			this.authentication = authentication;
		}

		@Override
		public Principal getPrincipal() {
			return this.authentication;
		}

		@Override
		public boolean isUserInRole(String role) {
			if (!role.startsWith(this.roleVoter.getRolePrefix())) {
				role = this.roleVoter.getRolePrefix() + role;
			}
			return this.roleVoter.vote(this.authentication, null,
					Collections.singletonList(new SecurityConfig(role))) == AccessDecisionVoter.ACCESS_GRANTED;
		}

	}

}
