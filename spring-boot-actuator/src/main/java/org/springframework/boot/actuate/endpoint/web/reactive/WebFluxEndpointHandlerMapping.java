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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebFluxEndpointHandlerMapping extends RequestMappingInfoHandlerMapping
		implements InitializingBean {

	private static final PathPatternParser pathPatternParser = new PathPatternParser();

	private final Method handleRead = ReflectionUtils
			.findMethod(ReadOperationHandler.class, "handle", ServerWebExchange.class);

	private final Method handleWrite = ReflectionUtils.findMethod(
			WriteOperationHandler.class, "handle", ServerWebExchange.class, Map.class);

	private final Method links = ReflectionUtils.findMethod(getClass(), "links",
			ServerHttpRequest.class);

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	private final EndpointMapping endpointMapping;

	private final Collection<EndpointInfo<WebEndpointOperation>> webEndpoints;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param collection the web endpoints
	 */
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebEndpointOperation>> collection) {
		this(endpointMapping, collection, null);
	}

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the path beneath which all endpoints should be mapped
	 * @param webEndpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 */
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints,
			CorsConfiguration corsConfiguration) {
		this.endpointMapping = endpointMapping;
		this.webEndpoints = webEndpoints;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
		if (StringUtils.hasText(this.endpointMapping.getPath())) {
			registerLinksMapping();
		}
	}

	private void registerLinksMapping() {
		registerMapping(new RequestMappingInfo(
				new PatternsRequestCondition(
						pathPatternParser.parse(this.endpointMapping.getPath())),
				new RequestMethodsRequestCondition(RequestMethod.GET), null, null, null,
				null, null), this, this.links);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	private void registerMappingForOperation(WebEndpointOperation operation) {
		OperationType operationType = operation.getType();
		OperationInvoker operationInvoker = operation.getInvoker();
		if (operation.isBlocking()) {
			operationInvoker = new ElasticSchedulerOperationInvoker(operationInvoker);
		}
		registerMapping(createRequestMappingInfo(operation),
				operationType == OperationType.WRITE
						? new WriteOperationHandler(operationInvoker)
						: new ReadOperationHandler(operationInvoker),
				operationType == OperationType.WRITE ? this.handleWrite
						: this.handleRead);
	}

	private RequestMappingInfo createRequestMappingInfo(
			WebEndpointOperation operationInfo) {
		OperationRequestPredicate requestPredicate = operationInfo.getRequestPredicate();
		PatternsRequestCondition patterns = new PatternsRequestCondition(pathPatternParser
				.parse(this.endpointMapping.createSubPath(requestPredicate.getPath())));
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.valueOf(requestPredicate.getHttpMethod().name()));
		ConsumesRequestCondition consumes = new ConsumesRequestCondition(
				toStringArray(requestPredicate.getConsumes()));
		ProducesRequestCondition produces = new ProducesRequestCondition(
				toStringArray(requestPredicate.getProduces()));
		return new RequestMappingInfo(null, patterns, methods, null, null, consumes,
				produces, null);
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
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

	@ResponseBody
	private Map<String, Map<String, Link>> links(ServerHttpRequest request) {
		return Collections.singletonMap("_links",
				this.endpointLinksResolver.resolveLinks(this.webEndpoints,
						UriComponentsBuilder.fromUri(request.getURI()).replaceQuery(null)
								.toUriString()));
	}

	/**
	 * Base class for handlers for endpoint operations.
	 */
	abstract class AbstractOperationHandler {

		private final OperationInvoker operationInvoker;

		AbstractOperationHandler(OperationInvoker operationInvoker) {
			this.operationInvoker = operationInvoker;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Publisher<ResponseEntity<? extends Object>> doHandle(ServerWebExchange exchange,
				Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>((Map<String, String>) exchange
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			if (body != null) {
				arguments.putAll(body);
			}
			exchange.getRequest().getQueryParams().forEach((name, values) -> arguments
					.put(name, values.size() == 1 ? values.get(0) : values));
			return (Publisher) handleResult(
					(Publisher<?>) this.operationInvoker.invoke(arguments),
					exchange.getRequest().getMethod());
		}

		private Publisher<ResponseEntity<Object>> handleResult(Publisher<?> result,
				HttpMethod httpMethod) {
			return Mono.from(result).map(this::toResponseEntity)
					.onErrorReturn(ParameterMappingException.class,
							new ResponseEntity<>(HttpStatus.BAD_REQUEST))
					.defaultIfEmpty(new ResponseEntity<>(httpMethod == HttpMethod.GET
							? HttpStatus.NOT_FOUND : HttpStatus.NO_CONTENT));
		}

		private ResponseEntity<Object> toResponseEntity(Object response) {
			if (!(response instanceof WebEndpointResponse)) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
			return new ResponseEntity<>(webEndpointResponse.getBody(),
					HttpStatus.valueOf(webEndpointResponse.getStatus()));
		}

	}

	/**
	 * A handler for an endpoint write operation.
	 */
	final class WriteOperationHandler extends AbstractOperationHandler {

		WriteOperationHandler(OperationInvoker operationInvoker) {
			super(operationInvoker);
		}

		@ResponseBody
		public Publisher<ResponseEntity<?>> handle(ServerWebExchange exchange,
				@RequestBody(required = false) Map<String, String> body) {
			return doHandle(exchange, body);
		}

	}

	/**
	 * A handler for an endpoint write operation.
	 */
	final class ReadOperationHandler extends AbstractOperationHandler {

		ReadOperationHandler(OperationInvoker operationInvoker) {
			super(operationInvoker);
		}

		@ResponseBody
		public Publisher<ResponseEntity<?>> handle(ServerWebExchange exchange) {
			return doHandle(exchange, null);
		}

	}

	/**
	 * An {@link OperationInvoker} that performs the invocation of a blocking operation on
	 * a separate thread using Reactor's {@link Schedulers#elastic() elastic scheduler}.
	 */
	private static final class ElasticSchedulerOperationInvoker
			implements OperationInvoker {

		private final OperationInvoker delegate;

		private ElasticSchedulerOperationInvoker(OperationInvoker delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object invoke(Map<String, Object> arguments) {
			return Mono.create((sink) -> Schedulers.elastic()
					.schedule(() -> invoke(arguments, sink)));
		}

		private void invoke(Map<String, Object> arguments, MonoSink<Object> sink) {
			try {
				Object result = this.delegate.invoke(arguments);
				sink.success(result);
			}
			catch (Exception ex) {
				sink.error(ex);
			}
		}

	}

}
