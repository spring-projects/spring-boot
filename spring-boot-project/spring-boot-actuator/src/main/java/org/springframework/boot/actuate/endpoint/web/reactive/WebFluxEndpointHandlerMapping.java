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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.reflect.ParametersMissingException;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebFluxEndpointHandlerMapping extends AbstractWebFluxEndpointHandlerMapping
		implements InitializingBean {

	private final Method handleRead = ReflectionUtils
			.findMethod(ReadOperationHandler.class, "handle", ServerWebExchange.class);

	private final Method handleWrite = ReflectionUtils.findMethod(
			WriteOperationHandler.class, "handle", ServerWebExchange.class, Map.class);

	private final Method links = ReflectionUtils.findMethod(getClass(), "links",
			ServerHttpRequest.class);

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param collection the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 */
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebOperation>> collection,
			EndpointMediaTypes endpointMediaTypes) {
		this(endpointMapping, collection, endpointMediaTypes, null);
	}

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param webEndpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 */
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebOperation>> webEndpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration) {
		super(endpointMapping, webEndpoints, endpointMediaTypes, corsConfiguration);
		setOrder(-100);
	}

	@Override
	protected Method getLinks() {
		return this.links;
	}

	@Override
	protected void registerMappingForOperation(WebOperation operation) {
		OperationType operationType = operation.getType();
		OperationInvoker operationInvoker = operation.getInvoker();
		if (operation.isBlocking()) {
			operationInvoker = new ElasticSchedulerOperationInvoker(operationInvoker);
		}
		registerMapping(createRequestMappingInfo(operation),
				operationType == OperationType.WRITE
						? new WebFluxEndpointHandlerMapping.WriteOperationHandler(
								operationInvoker)
						: new WebFluxEndpointHandlerMapping.ReadOperationHandler(
								operationInvoker),
				operationType == OperationType.WRITE ? this.handleWrite
						: this.handleRead);
	}

	@ResponseBody
	private Map<String, Map<String, Link>> links(ServerHttpRequest request) {
		return Collections.singletonMap("_links",
				this.endpointLinksResolver.resolveLinks(getEndpoints(),
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

		@SuppressWarnings({ "unchecked" })
		Publisher<ResponseEntity<Object>> doHandle(ServerWebExchange exchange,
				Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>((Map<String, String>) exchange
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			if (body != null) {
				arguments.putAll(body);
			}
			exchange.getRequest().getQueryParams().forEach((name, values) -> arguments
					.put(name, values.size() == 1 ? values.get(0) : values));
			return handleResult((Publisher<?>) this.operationInvoker.invoke(arguments),
					exchange.getRequest().getMethod());
		}

		private Publisher<ResponseEntity<Object>> handleResult(Publisher<?> result,
				HttpMethod httpMethod) {
			return Mono.from(result).map(this::toResponseEntity)
					.onErrorReturn(ParametersMissingException.class,
							new ResponseEntity<>(HttpStatus.BAD_REQUEST))
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
		public Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange,
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
		public Publisher<ResponseEntity<Object>> handle(ServerWebExchange exchange) {
			return doHandle(exchange, null);
		}

	}

}
