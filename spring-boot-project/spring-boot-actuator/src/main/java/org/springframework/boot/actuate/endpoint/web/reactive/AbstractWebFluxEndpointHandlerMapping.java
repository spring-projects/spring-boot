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
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public abstract class AbstractWebFluxEndpointHandlerMapping
		extends RequestMappingInfoHandlerMapping {

	private static final PathPatternParser pathPatternParser = new PathPatternParser();

	private final EndpointMapping endpointMapping;

	private final Collection<EndpointInfo<WebOperation>> webEndpoints;

	private final EndpointMediaTypes endpointMediaTypes;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param collection the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 */
	public AbstractWebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
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
	public AbstractWebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebOperation>> webEndpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration) {
		this.endpointMapping = endpointMapping;
		this.webEndpoints = webEndpoints;
		this.endpointMediaTypes = endpointMediaTypes;
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
		PatternsRequestCondition patterns = new PatternsRequestCondition(
				pathPatternParser.parse(this.endpointMapping.getPath()));
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.GET);
		ProducesRequestCondition produces = new ProducesRequestCondition(
				this.endpointMediaTypes.getProduced().toArray(
						new String[this.endpointMediaTypes.getProduced().size()]));
		RequestMappingInfo mapping = new RequestMappingInfo(patterns, methods, null, null,
				null, produces, null);
		registerMapping(mapping, this, getLinks());
	}

	protected RequestMappingInfo createRequestMappingInfo(
			WebOperation operationInfo) {
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
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	public Collection<EndpointInfo<WebOperation>> getEndpoints() {
		return this.webEndpoints;
	}

	protected abstract Method getLinks();

	protected abstract void registerMappingForOperation(WebOperation operation);

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	protected RequestMappingInfo getMappingForMethod(Method method,
			Class<?> handlerType) {
		return null;
	}

	/**
	 * An {@link OperationInvoker} that performs the invocation of a blocking operation on
	 * a separate thread using Reactor's {@link Schedulers#elastic() elastic scheduler}.
	 */
	protected static final class ElasticSchedulerOperationInvoker
			implements OperationInvoker {

		private final OperationInvoker delegate;

		public ElasticSchedulerOperationInvoker(OperationInvoker delegate) {
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
