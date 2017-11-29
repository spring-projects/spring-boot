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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public abstract class AbstractWebMvcEndpointHandlerMapping
		extends RequestMappingInfoHandlerMapping implements InitializingBean {

	private final EndpointMapping endpointMapping;

	private final Collection<EndpointInfo<WebOperation>> webEndpoints;

	private final EndpointMediaTypes endpointMediaTypes;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param collection the web endpoints operations
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 */
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
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
	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebOperation>> webEndpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration) {
		this.endpointMapping = endpointMapping;
		this.webEndpoints = webEndpoints;
		this.endpointMediaTypes = endpointMediaTypes;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	public Collection<EndpointInfo<WebOperation>> getEndpoints() {
		return this.webEndpoints;
	}

	public EndpointMapping getEndpointMapping() {
		return this.endpointMapping;
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
		if (StringUtils.hasText(this.endpointMapping.getPath())) {
			registerLinksRequestMapping();
		}
	}

	private void registerLinksRequestMapping() {
		PatternsRequestCondition patterns = patternsRequestConditionForPattern("");
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.GET);
		ProducesRequestCondition produces = new ProducesRequestCondition(
				this.endpointMediaTypes.getProduced().toArray(
						new String[this.endpointMediaTypes.getProduced().size()]));
		RequestMappingInfo mapping = new RequestMappingInfo(patterns, methods, null, null,
				null, produces, null);
		registerMapping(mapping, this, getLinks());
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	protected abstract Method getLinks();

	protected abstract void registerMappingForOperation(WebOperation operation);

	protected RequestMappingInfo createRequestMappingInfo(
			WebOperation operationInfo) {
		OperationRequestPredicate requestPredicate = operationInfo.getRequestPredicate();
		PatternsRequestCondition patterns = patternsRequestConditionForPattern(
				requestPredicate.getPath());
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.valueOf(requestPredicate.getHttpMethod().name()));
		ConsumesRequestCondition consumes = new ConsumesRequestCondition(
				toStringArray(requestPredicate.getConsumes()));
		ProducesRequestCondition produces = new ProducesRequestCondition(
				toStringArray(requestPredicate.getProduces()));
		return new RequestMappingInfo(null, patterns, methods, null, null, consumes,
				produces, null);
	}

	private PatternsRequestCondition patternsRequestConditionForPattern(String path) {
		String[] patterns = new String[] { this.endpointMapping.createSubPath(path) };
		return new PatternsRequestCondition(patterns, null, null, false, false);
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

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

	/**
	 * {@link HandlerInterceptorAdapter} to ensure that
	 * {@link PathExtensionContentNegotiationStrategy} is skipped for web endpoints.
	 */
	private static final class SkipPathExtensionContentNegotiation
			extends HandlerInterceptorAdapter {

		private static final String SKIP_ATTRIBUTE = PathExtensionContentNegotiationStrategy.class
				.getName() + ".SKIP";

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler) throws Exception {
			request.setAttribute(SKIP_ATTRIBUTE, Boolean.TRUE);
			return true;
		}

	}

}
