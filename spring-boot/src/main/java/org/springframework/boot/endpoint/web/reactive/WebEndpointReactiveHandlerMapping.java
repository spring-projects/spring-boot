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

package org.springframework.boot.endpoint.web.reactive;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.OperationInvoker;
import org.springframework.boot.endpoint.ParameterMappingException;
import org.springframework.boot.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.endpoint.web.Link;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ReflectionUtils;
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
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available
 * over HTTP using Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointReactiveHandlerMapping extends RequestMappingInfoHandlerMapping
		implements InitializingBean {

	private static final PathPatternParser pathPatternParser = new PathPatternParser();

	private final Method handleRead = ReflectionUtils
			.findMethod(ReadOperationHandler.class, "handle", ServerWebExchange.class);

	private final Method handleWrite = ReflectionUtils.findMethod(
			WriteOperationHandler.class, "handle", ServerWebExchange.class, Map.class);

	private final Method links = ReflectionUtils.findMethod(getClass(), "links",
			ServerHttpRequest.class);

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	private final String endpointPath;

	private final Collection<EndpointInfo<WebEndpointOperation>> webEndpoints;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointPath the path beneath which all endpoints should be mapped
	 * @param collection the web endpoints
	 */
	public WebEndpointReactiveHandlerMapping(String endpointPath,
			Collection<EndpointInfo<WebEndpointOperation>> collection) {
		this(endpointPath, collection, null);
	}

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointPath the path beneath which all endpoints should be mapped
	 * @param webEndpoints the web endpoints
	 * @param corsConfiguration the CORS configuraton for the endpoints
	 */
	public WebEndpointReactiveHandlerMapping(String endpointPath,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints,
			CorsConfiguration corsConfiguration) {
		this.endpointPath = (endpointPath.startsWith("/") ? "" : "/") + endpointPath;
		this.webEndpoints = webEndpoints;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
		registerMapping(new RequestMappingInfo(
				new PatternsRequestCondition(pathPatternParser.parse(this.endpointPath)),
				new RequestMethodsRequestCondition(RequestMethod.GET), null, null, null,
				null, null), this, this.links);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	private void registerMappingForOperation(WebEndpointOperation operation) {
		EndpointOperationType operationType = operation.getType();
		registerMapping(createRequestMappingInfo(operation),
				operationType == EndpointOperationType.WRITE
						? new WriteOperationHandler(operation.getOperationInvoker())
						: new ReadOperationHandler(operation.getOperationInvoker()),
				operationType == EndpointOperationType.WRITE ? this.handleWrite
						: this.handleRead);
	}

	private RequestMappingInfo createRequestMappingInfo(
			WebEndpointOperation operationInfo) {
		OperationRequestPredicate requestPredicate = operationInfo.getRequestPredicate();
		return new RequestMappingInfo(null,
				new PatternsRequestCondition(pathPatternParser
						.parse(this.endpointPath + "/" + requestPredicate.getPath())),
				new RequestMethodsRequestCondition(
						RequestMethod.valueOf(requestPredicate.getHttpMethod().name())),
				null, null,
				new ConsumesRequestCondition(
						toStringArray(requestPredicate.getConsumes())),
				new ProducesRequestCondition(
						toStringArray(requestPredicate.getProduces())),
				null);
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

		@SuppressWarnings("unchecked")
		ResponseEntity<?> doHandle(ServerWebExchange exchange, Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>((Map<String, String>) exchange
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			if (body != null) {
				arguments.putAll(body);
			}
			exchange.getRequest().getQueryParams().forEach((name, values) -> arguments
					.put(name, values.size() == 1 ? values.get(0) : values));
			try {
				return handleResult(this.operationInvoker.invoke(arguments),
						exchange.getRequest().getMethod());
			}
			catch (ParameterMappingException ex) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
		}

		private ResponseEntity<?> handleResult(Object result, HttpMethod httpMethod) {
			if (result == null) {
				return new ResponseEntity<>(httpMethod == HttpMethod.GET
						? HttpStatus.NOT_FOUND : HttpStatus.NO_CONTENT);
			}
			if (!(result instanceof WebEndpointResponse)) {
				return new ResponseEntity<>(result, HttpStatus.OK);
			}
			WebEndpointResponse<?> response = (WebEndpointResponse<?>) result;
			return new ResponseEntity<Object>(response.getBody(),
					HttpStatus.valueOf(response.getStatus()));
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
		public ResponseEntity<?> handle(ServerWebExchange exchange,
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
		public ResponseEntity<?> handle(ServerWebExchange exchange) {
			return doHandle(exchange, null);
		}

	}

}
