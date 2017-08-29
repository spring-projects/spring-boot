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

package org.springframework.boot.endpoint.web.mvc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available
 * over HTTP using Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public abstract class AbstractWebEndpointServletHandlerMapping
		extends RequestMappingInfoHandlerMapping implements InitializingBean {

	private final String endpointPath;

	private final Collection<EndpointInfo<WebEndpointOperation>> webEndpoints;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param endpointPath the path beneath which all endpoints should be mapped
	 * @param collection the web endpoints operations
	 */
	public AbstractWebEndpointServletHandlerMapping(String endpointPath,
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
	public AbstractWebEndpointServletHandlerMapping(String endpointPath,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints,
			CorsConfiguration corsConfiguration) {
		this.endpointPath = (endpointPath.startsWith("/") ? "" : "/") + endpointPath;
		this.webEndpoints = webEndpoints;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	public Collection<EndpointInfo<WebEndpointOperation>> getEndpoints() {
		return this.webEndpoints;
	}

	public String getEndpointPath() {
		return this.endpointPath;
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
		PatternsRequestCondition patterns = patternsRequestConditionForPattern("");
		RequestMethodsRequestCondition methods = new RequestMethodsRequestCondition(
				RequestMethod.GET);
		RequestMappingInfo mapping = new RequestMappingInfo(patterns, methods, null, null,
				null, null, null);
		registerMapping(mapping, this, getLinks());
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	protected abstract Method getLinks();

	protected abstract void registerMappingForOperation(WebEndpointOperation operation);

	protected RequestMappingInfo createRequestMappingInfo(
			WebEndpointOperation operationInfo) {
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
		String[] patterns = new String[] {
				this.endpointPath + (StringUtils.hasText(path) ? "/" + path : "") };
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
